package com.wisdom.clound.ui.goods;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoodsDetailsActivity extends AppCompatActivity {
    // 控件
    private ImageView btnBack, ivCart;
    private RelativeLayout rlCartIcon;
    private ViewPager2 vpBanner;
    private LinearLayout indicatorLayout;
    private TextView tvGoodsName, tvGoodsPrice, tvGoodsContent, tvUserAddress, tvCartCount;
    private LinearLayout llAddressLayout, layoutGoodsMode;
    private RecyclerView rvDetailsImage;
    private View btnAddCart, btnBuyNow;

    // 数据
    private int goodsId;
    private String userId;
    private OkHttpClient okHttpClient;
    private Handler handler = new Handler(Looper.getMainLooper());
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");
    private double userBalance = 0.0; // 账户余额

    // 地址相关
    private int selectedAddressId = 0;
    private List<AddressBean> addressList = new ArrayList<>();
    private AlertDialog addressDialog;

    // 支付弹窗
    private AlertDialog payDialog;
    private RadioGroup rgPayWay;
    private RadioButton rbBalance, rbWechat, rbMix;
    private TextView tvBalance;

    // 轮播/详情图数据
    private List<String> bannerList = new ArrayList<>();
    private List<String> detailsImageList = new ArrayList<>();

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_details);

        // 1. 获取商品ID
        goodsId = getIntent().getIntExtra("goodsId", 0);
        if (goodsId == 0) {
            showToast("商品参数错误");
            finish();
            return;
        }

        // 2. 校验登录
        userId = SPUtils.getUserId(this);
        if (userId == null || userId.isEmpty()) {
            showToast("请先登录");
            finish();
            return;
        }

        // 3. 初始化
        initView();
        initOkHttp();
        getGoodsDetails();
        getCartCount();

        // 4. 点击事件
        btnBack.setOnClickListener(v -> finish());
        llAddressLayout.setOnClickListener(v -> getUserAddressList());
        btnAddCart.setOnClickListener(v -> addToCart());
        rlCartIcon.setOnClickListener(v -> jumpToCartFragment());

        // ========== 立即购买点击事件 ==========
        btnBuyNow.setOnClickListener(v -> {
            if (selectedAddressId == 0) {
                showToast("请先选择收货地址");
                return;
            }
            // 先获取余额，再弹出支付弹窗
            getUserBalance();
        });
    }

    // 初始化控件
    private void initView() {
        btnBack = findViewById(R.id.btn_back);
        vpBanner = findViewById(R.id.vp_banner);
        indicatorLayout = findViewById(R.id.indicator_layout);
        tvGoodsName = findViewById(R.id.tv_goods_name);
        tvGoodsPrice = findViewById(R.id.tv_goods_price);
        tvGoodsContent = findViewById(R.id.tv_goods_content);
        tvUserAddress = findViewById(R.id.tv_user_address);
        llAddressLayout = findViewById(R.id.ll_address_layout);
        layoutGoodsMode = findViewById(R.id.layout_goods_mode);
        rvDetailsImage = findViewById(R.id.rv_details_image);
        btnAddCart = findViewById(R.id.btn_add_cart);
        btnBuyNow = findViewById(R.id.btn_buy_now);

        rlCartIcon = findViewById(R.id.rl_cart_icon);
        ivCart = findViewById(R.id.iv_cart);
        tvCartCount = findViewById(R.id.tv_cart_count);

        rvDetailsImage.setLayoutManager(new LinearLayoutManager(this));
    }

    // ========== 1. 获取用户账户余额 ==========
    private void getUserBalance() {
        String url = "https://api.rzkj.qyqd123.cn/Android/WalletActivity/GetUserWallet?userId=" + userId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("获取余额失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject root = new JSONObject(json);
                    if (root.getInt("code") == 200) {
                        JSONObject data = root.getJSONObject("data");
                        userBalance = data.getDouble("UserWallets");
                        runOnUiThread(() -> showPayWayDialog());
                    } else {
                        runOnUiThread(() -> showToast("获取余额失败"));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> showToast("余额解析异常"));
                }
            }
        });
    }

    // ========== 2. 弹出底部支付方式弹窗 ==========
    private void showPayWayDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pay_way, null);
        RadioButton rbBalance = dialogView.findViewById(R.id.rb_balance);
        RadioButton rbWechat = dialogView.findViewById(R.id.rb_wechat);
//        RadioButton rbMix = dialogView.findViewById(R.id.rb_mix);
        TextView tvBalance = dialogView.findViewById(R.id.tv_balance);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm_pay);

        // 单选组互斥逻辑
        rbBalance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbWechat.setChecked(false);
//                rbMix.setChecked(false);
            }
        });
        rbWechat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbBalance.setChecked(false);
//                rbMix.setChecked(false);
            }
        });
//        rbMix.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                rbBalance.setChecked(false);
//                rbWechat.setChecked(false);
//            }
//        });

        // 显示余额
        tvBalance.setText("余额：¥" + priceFormat.format(userBalance));

        // 余额为0，禁用余额支付
        if (userBalance <= 0) {
            rbBalance.setEnabled(false);
            rbBalance.setAlpha(0.5f);
            rbWechat.setChecked(true); // 默认选中微信
        }

        // 创建底部弹窗
        payDialog = new AlertDialog.Builder(this, R.style.Dialog_Bottom)
                .setView(dialogView)
                .create();

        Window window = payDialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        payDialog.show();

        // 确认支付按钮
        btnConfirm.setOnClickListener(v -> {
            int payTypeId = 0;
            if (rbBalance.isChecked()) payTypeId = 0;
            else if (rbWechat.isChecked()) payTypeId = 1;
//            else if (rbMix.isChecked()) payTypeId = 2;

            payDialog.dismiss();
            createOrder(payTypeId); // 创建订单
        });
    }

    // ========== 3. 创建订单 POST请求 ==========
    private void createOrder(int payTypesId) {
        String url = "https://api.rzkj.qyqd123.cn/Android/OrderActivity/CreateOrder";

        // 构建请求参数
        JSONObject params = new JSONObject();
        try {
            params.put("userId", userId);
            params.put("userMapsId", selectedAddressId);
            params.put("goodsId", goodsId);
            params.put("payTypesId", payTypesId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(params.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("订单创建失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject root = new JSONObject(json);
                    if (root.getInt("code") == 200) {
                        int orderId = root.getInt("data");
                        runOnUiThread(() -> {
                            showToast("创建订单成功");
                            // 跳转到支付页面，传递订单ID
                            Intent intent = new Intent(GoodsDetailsActivity.this, OrderPayActivity.class);
                            intent.putExtra("orderId", orderId);
                            startActivity(intent);
                        });
                    } else {
                        runOnUiThread(() -> showToast("订单创建失败：" + root.optString("msg")));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> showToast("订单解析异常"));
                }
            }
        });
    }

    // 初始化OkHttp
    private void initOkHttp() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // 请求商品详情
    private void getGoodsDetails() {
        String url = "https://api.rzkj.qyqd123.cn/Android/GoodsActivity/GetGoodsDetails?userId=" + userId + "&goodsId=" + goodsId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("网络请求失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> parseData(json));
            }
        });
    }

    // 解析商品数据
    private void parseData(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("code") != 200) {
                showToast("数据加载失败");
                return;
            }

            JSONObject data = root.getJSONObject("data");
            JSONObject goodses = data.getJSONObject("Goodses");

            tvGoodsName.setText(goodses.getString("GoodsName"));
            double price = goodses.getDouble("GoodsWallet");
            tvGoodsPrice.setText("¥" + priceFormat.format(price));
            tvGoodsContent.setText(goodses.getString("GoodsContent"));

            JSONArray bannerArray = data.getJSONArray("GoodsBannerList");
            bannerList.clear();
            for (int i = 0; i < bannerArray.length(); i++) {
                bannerList.add(bannerArray.getJSONObject(i).getString("BannerImage"));
            }
            initBanner();

            JSONObject userMaps = data.optJSONObject("UserMaps");
            if (userMaps != null && !userMaps.isNull("Address")) {
                selectedAddressId = userMaps.getInt("Id");
                String address = userMaps.getString("Province") + userMaps.getString("City")
                        + userMaps.getString("District") + userMaps.getString("Address");
                tvUserAddress.setText(address);
            } else {
                tvUserAddress.setText("请添加收货地址");
                selectedAddressId = 0;
            }

            JSONArray modeList = data.getJSONArray("GoodsModeList");
            layoutGoodsMode.removeViews(1, layoutGoodsMode.getChildCount() - 1);
            layoutGoodsMode.setPadding(20, 20, 20, 20);
            for (int i = 0; i < modeList.length(); i++) {
                JSONObject mode = modeList.getJSONObject(i);
                addModeViewCenter(mode.getString("ModeKey"), mode.getString("ModeValue"));
            }

            JSONArray detailsList = data.getJSONArray("GoodsDetailses");
            detailsImageList.clear();
            for (int i = 0; i < detailsList.length(); i++) {
                detailsImageList.add(detailsList.getJSONObject(i).getString("DetailsImage"));
            }
            rvDetailsImage.setAdapter(new DetailsImageAdapter());

        } catch (Exception e) {
            e.printStackTrace();
            showToast("数据解析异常");
        }
    }

    // 商品规格
    private void addModeViewCenter(String key, String value) {
        LinearLayout rowLayout = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 10, 0, 10);
        rowLayout.setLayoutParams(rowParams);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setWeightSum(2);

        TextView tvKey = new TextView(this);
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tvKey.setLayoutParams(keyParams);
        tvKey.setText(key);
        tvKey.setTextSize(15);
        tvKey.setTextColor(Color.parseColor("#999999"));
        tvKey.setGravity(Gravity.LEFT);
        tvKey.setPadding(0, 5, 0, 5);

        TextView tvValue = new TextView(this);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tvValue.setLayoutParams(valueParams);
        tvValue.setText(value);
        tvValue.setTextSize(15);
        tvValue.setTextColor(Color.parseColor("#333333"));
        tvValue.setGravity(Gravity.LEFT);
        tvValue.setPadding(0, 5, 0, 5);

        rowLayout.addView(tvKey);
        rowLayout.addView(tvValue);
        layoutGoodsMode.addView(rowLayout);
    }

    // 获取购物车数量
    private void getCartCount() {
        String url = "https://api.rzkj.qyqd123.cn/Android/UserCarActivity/GetUserCarCount?userId=" + userId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()-> tvCartCount.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject root = new JSONObject(json);
                    int count = root.getInt("data");
                    runOnUiThread(() -> {
                        if (count > 0) {
                            tvCartCount.setText(String.valueOf(count));
                            tvCartCount.setVisibility(View.VISIBLE);
                        } else {
                            tvCartCount.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(()-> tvCartCount.setVisibility(View.GONE));
                }
            }
        });
    }

    // 加入购物车
    private void addToCart() {
        String url = "https://api.rzkj.qyqd123.cn/Android/UserCarActivity/InCollect?userId=" + userId + "&goodsId=" + goodsId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("加入失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject root = new JSONObject(json);
                    runOnUiThread(() -> {
                        try {
                            if (root.getInt("code") == 200) {
                                showToast(root.optString("msg"));
                                getCartCount();
                            } else {
                                showToast("操作失败");
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> showToast("加入失败"));
                }
            }
        });
    }

    // 获取地址列表
    private void getUserAddressList() {
        String url = "https://api.rzkj.qyqd123.cn/Android/UserMap/GetUserMap?userId=" + userId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("获取地址失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> parseAddressData(json));
            }
        });
    }

    private void parseAddressData(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("code") != 200) {
                showToast(root.getString("msg"));
                return;
            }

            addressList.clear();
            JSONArray dataArray = root.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject obj = dataArray.getJSONObject(i);
                AddressBean bean = new AddressBean();
                bean.setId(obj.getInt("Id"));
                bean.setUserName(obj.getString("UserName"));
                bean.setUserPhone(obj.getString("UserPhone"));
                bean.setAddress(obj.getString("Province") + obj.getString("City") + obj.getString("District") + obj.getString("Address"));
                bean.setIsDefault(obj.getInt("IsDefault"));
                addressList.add(bean);
            }
            showAddressDialog();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("地址解析失败");
        }
    }

    private void showAddressDialog() {
        if (addressList.isEmpty()) {
            showToast("暂无收货地址");
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_address_list, null);
        RecyclerView rvAddress = dialogView.findViewById(R.id.rv_address_list);
        rvAddress.setLayoutManager(new LinearLayoutManager(this));
        AddressAdapter adapter = new AddressAdapter(addressList);
        rvAddress.setAdapter(adapter);

        addressDialog = new AlertDialog.Builder(this)
                .setTitle("选择收货地址")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .create();
        addressDialog.show();

        adapter.setOnAddressClickListener(bean -> {
            selectedAddressId = bean.getId();
            tvUserAddress.setText(bean.getAddress());
            addressDialog.dismiss();
        });
    }

    // 跳转到购物车
    private void jumpToCartFragment() {
        try {
            Intent intent = new Intent(GoodsDetailsActivity.this, com.wisdom.clound.MainActivity.class);
            intent.putExtra("target_fragment", "switch_to_cart");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("跳转购物车失败");
        }
    }

    static class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
        private List<AddressBean> list;
        private OnAddressClickListener listener;

        public AddressAdapter(List<AddressBean> list) {
            this.list = list;
        }

        public void setOnAddressClickListener(OnAddressClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address_dialog, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AddressBean bean = list.get(position);
            holder.tvName.setText(bean.getUserName());
            holder.tvPhone.setText(bean.getUserPhone());
            holder.tvAddress.setText(bean.getAddress());
            holder.tvDefaultTag.setVisibility(bean.getIsDefault() == 1 ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(bean);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDefaultTag, tvName, tvPhone, tvAddress;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDefaultTag = itemView.findViewById(R.id.tv_default_tag);
                tvName = itemView.findViewById(R.id.tv_user_name);
                tvPhone = itemView.findViewById(R.id.tv_user_phone);
                tvAddress = itemView.findViewById(R.id.tv_address);
            }
        }
    }

    interface OnAddressClickListener {
        void onClick(AddressBean bean);
    }

    static class AddressBean {
        private int id;
        private String userName;
        private String userPhone;
        private String address;
        private int isDefault;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserPhone() { return userPhone; }
        public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public int getIsDefault() { return isDefault; }
        public void setIsDefault(int isDefault) { this.isDefault = isDefault; }
    }

    // 轮播初始化
    private void initBanner() {
        if (bannerList.isEmpty()) return;
        vpBanner.setAdapter(new BannerAdapter());
        initIndicator();
    }

    private void initIndicator() {
        indicatorLayout.removeAllViews();
        for (int i = 0; i < bannerList.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(8, 8);
            params.setMargins(5, 0, 5, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0 ? R.drawable.shape_indicator_selected : R.drawable.shape_indicator_normal);
            indicatorLayout.addView(dot);
        }

        vpBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < indicatorLayout.getChildCount(); i++) {
                    indicatorLayout.getChildAt(i).setBackgroundResource(i == position ? R.drawable.shape_indicator_selected : R.drawable.shape_indicator_normal);
                }
            }
        });
    }

    private class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(GoodsDetailsActivity.this);
            imageView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new Holder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            loadImage(bannerList.get(position), (ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() { return bannerList.size(); }
        class Holder extends RecyclerView.ViewHolder { public Holder(@NonNull View itemView) { super(itemView); } }
    }

    private class DetailsImageAdapter extends RecyclerView.Adapter<DetailsImageAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(GoodsDetailsActivity.this);
            imageView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new Holder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            loadImage(detailsImageList.get(position), (ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() { return detailsImageList.size(); }
        class Holder extends RecyclerView.ViewHolder { public Holder(@NonNull View itemView) { super(itemView); } }
    }

    private void loadImage(String url, ImageView imageView) {
        new Thread(() -> {
            try {
                InputStream is = new URL(url).openStream();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                handler.post(() -> imageView.setImageBitmap(bitmap));
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 统一Toast
    private void showToast(String msg) {
        if (msg == null || msg.isEmpty()) return;
        TextView textView = new TextView(this);
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(0xFFFFFFFF);
        textView.setBackgroundColor(0xCC000000);
        textView.setPadding(50, 25, 50, 25);
        textView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(this);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (addressDialog != null && addressDialog.isShowing()) addressDialog.dismiss();
        if (payDialog != null && payDialog.isShowing()) payDialog.dismiss();
    }
}
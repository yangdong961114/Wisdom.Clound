package com.wisdom.clound.ui.goods;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

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

public class OrderPayActivity extends AppCompatActivity {
    // 控件
    private ImageView btnBack;
    private TextView tvOrderAddress, tvTotalPrice, tvPayName;
    private ImageView ivPayIcon;
    private RecyclerView rvOrderGoods;
    private LinearLayout llAddressLayout;
    private View btnPayNow;

    // 数据
    private int orderId;
    private String userId;
    private OkHttpClient okHttpClient;
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");
    private double totalPrice = 0.0; // 总金额
    private int payTypeId = 0; // 支付方式ID

    // 地址相关
    private int selectedAddressId = 0;
    private List<GoodsDetailsActivity.AddressBean> addressList = new ArrayList<>();
    private android.app.AlertDialog addressDialog;

    // 商品列表
    private List<OrderGoodsBean> goodsList = new ArrayList<>();
    private OrderGoodsAdapter goodsAdapter;

    // 图片加载
    private Handler handler = new Handler(Looper.getMainLooper());

    // ==================== 支付密码弹窗 相关变量 ====================
    private Dialog pwdDialog;
    private EditText[] passwordEditTexts = new EditText[6];
    private int currentPosition = 0;
    // 微信支付SDK
    private IWXAPI wxApi;
    // 微信开放平台APPID（替换成你的微信开放平台APPID）
    private static final String WX_APP_ID = "你的微信开放平台APPID";
    // POST请求JSON格式
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_pay);

        // 初始化微信支付
        wxApi = WXAPIFactory.createWXAPI(this, WX_APP_ID, true);
        wxApi.registerApp(WX_APP_ID);

        // 1. 接收上一页传递的订单ID
        orderId = getIntent().getIntExtra("orderId", 0);
        if (orderId == 0) {
            showToast("订单参数错误");
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
        getOrderDetails(); // 请求订单详情

        // 4. 点击事件
        btnBack.setOnClickListener(v -> finish());
        llAddressLayout.setOnClickListener(v -> getUserAddressList()); // 切换地址
        btnPayNow.setOnClickListener(v -> payNow()); // 立即付款
    }

    // 初始化控件
    private void initView() {
        btnBack = findViewById(R.id.btn_back);
        tvOrderAddress = findViewById(R.id.tv_order_address);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        ivPayIcon = findViewById(R.id.iv_pay_icon);
        tvPayName = findViewById(R.id.tv_pay_name);
        rvOrderGoods = findViewById(R.id.rv_order_goods);
        llAddressLayout = findViewById(R.id.ll_address_layout);
        btnPayNow = findViewById(R.id.btn_pay_now);

        // 商品列表初始化
        rvOrderGoods.setLayoutManager(new LinearLayoutManager(this));
        goodsAdapter = new OrderGoodsAdapter();
        rvOrderGoods.setAdapter(goodsAdapter);
    }

    // ==================== 核心：立即付款 ====================
    private void payNow() {
        if (payTypeId == 0) {
            // 1. 余额支付 → 弹出密码盘
            showSixPasswordDialog();
        } else if (payTypeId == 1) {
            // 2. 微信支付 → 调用微信预支付接口
            weChatPay();
        } else {
            showToast("暂不支持该支付方式");
        }
    }

    // ==================== 1. 余额支付（密码验证+接口请求） ====================
    private void walletPay(String userPayPwd) {
        // 构建POST参数
        JSONObject params = new JSONObject();
        try {
            params.put("orderId", orderId);
            params.put("userPayPwd", userPayPwd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());
        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/OrderPayActivity/WalletPay")
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    dismissAllDialog();
                    showToast("网络请求失败");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        dismissAllDialog();
                        if (root.getInt("code") == 200) {
                            showToast("订单支付成功");
                            // 支付成功，关闭页面，刷新订单列表
                            setResult(RESULT_OK); // 关键：返回成功结果
                            finish();
                        } else {
                            showToast(root.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("支付失败");
                    }
                });
            }
        });
    }

    // ==================== 2. 微信支付（预支付+拉起微信） ====================
    private void weChatPay() {
        // 构建POST参数
        JSONObject params = new JSONObject();
        try {
            params.put("orderId", orderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());
        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/OrderPayActivity/WeChatOrderPay")
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("微信支付请求失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        if (root.getInt("code") == 200) {
                            // 解析微信支付参数
                            JSONObject data = root.getJSONObject("data");
                            // 拉起微信支付
                            startWxPay(data);
                        } else {
                            showToast(root.getString("msg"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("微信支付参数解析失败");
                    }
                });
            }
        });
    }

    // 拉起微信APP支付
    private void startWxPay(JSONObject data) {
        try {
            PayReq req = new PayReq();
            req.appId = data.getString("appid");
            req.partnerId = data.getString("partnerid");
            req.prepayId = data.getString("prepayid");
            req.nonceStr = data.getString("noncestr");
            req.timeStamp = data.getString("timestamp");
            req.packageValue = data.getString("package");
            req.sign = data.getString("sign");
            // 发送请求到微信
            wxApi.sendReq(req);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("拉起微信支付失败");
        }
    }

    // ==================== 6位支付密码弹窗（完全复用你的逻辑） ====================
    private void showSixPasswordDialog() {
        currentPosition = 0;
        pwdDialog = new Dialog(this, R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pay_password, null);
        pwdDialog.setContentView(view);
        pwdDialog.setCancelable(true);

        // 弹窗配置
        if (pwdDialog.getWindow() != null) {
            WindowManager.LayoutParams params = pwdDialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            params.gravity = Gravity.CENTER;
            pwdDialog.getWindow().setAttributes(params);
        }

        // 初始化密码框
        passwordEditTexts[0] = view.findViewById(R.id.et_pwd_1);
        passwordEditTexts[1] = view.findViewById(R.id.et_pwd_2);
        passwordEditTexts[2] = view.findViewById(R.id.et_pwd_3);
        passwordEditTexts[3] = view.findViewById(R.id.et_pwd_4);
        passwordEditTexts[4] = view.findViewById(R.id.et_pwd_5);
        passwordEditTexts[5] = view.findViewById(R.id.et_pwd_6);
        TextView tvCancel = view.findViewById(R.id.tv_cancel);

        // 绑定键盘按钮
        Button btn0 = view.findViewById(R.id.btn_0);
        Button btn1 = view.findViewById(R.id.btn_1);
        Button btn2 = view.findViewById(R.id.btn_2);
        Button btn3 = view.findViewById(R.id.btn_3);
        Button btn4 = view.findViewById(R.id.btn_4);
        Button btn5 = view.findViewById(R.id.btn_5);
        Button btn6 = view.findViewById(R.id.btn_6);
        Button btn7 = view.findViewById(R.id.btn_7);
        Button btn8 = view.findViewById(R.id.btn_8);
        Button btn9 = view.findViewById(R.id.btn_9);
        Button btnDel = view.findViewById(R.id.btn_del);

        // 清空密码框
        for (EditText et : passwordEditTexts) et.setText("");

        // 数字点击
        btn0.setOnClickListener(v -> inputPassword("0"));
        btn1.setOnClickListener(v -> inputPassword("1"));
        btn2.setOnClickListener(v -> inputPassword("2"));
        btn3.setOnClickListener(v -> inputPassword("3"));
        btn4.setOnClickListener(v -> inputPassword("4"));
        btn5.setOnClickListener(v -> inputPassword("5"));
        btn6.setOnClickListener(v -> inputPassword("6"));
        btn7.setOnClickListener(v -> inputPassword("7"));
        btn8.setOnClickListener(v -> inputPassword("8"));
        btn9.setOnClickListener(v -> inputPassword("9"));

        // 删除/取消
        btnDel.setOnClickListener(v -> deletePassword());
        tvCancel.setOnClickListener(v -> pwdDialog.dismiss());

        pwdDialog.show();
    }

    // 输入密码
    private void inputPassword(String num) {
        if (currentPosition < 6) {
            passwordEditTexts[currentPosition].setText(num);
            currentPosition++;
            // 输满6位 → 提交余额支付
            if (currentPosition == 6) {
                String pwd = getPassword();
                walletPay(pwd);
            }
        }
    }

    // 删除密码
    private void deletePassword() {
        if (currentPosition > 0) {
            currentPosition--;
            passwordEditTexts[currentPosition].setText("");
        }
    }

    // 获取完整密码
    private String getPassword() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : passwordEditTexts) {
            sb.append(et.getText().toString().trim());
        }
        return sb.toString();
    }

    // 关闭所有弹窗
    private void dismissAllDialog() {
        if (pwdDialog != null && pwdDialog.isShowing()) {
            pwdDialog.dismiss();
        }
        if (addressDialog != null && addressDialog.isShowing()) {
            addressDialog.dismiss();
        }
    }

    // ==================== 订单详情请求（原有代码不变） ====================
    private void getOrderDetails() {
        String url = "https://api.rzkj.qyqd123.cn/Android/OrderActivity/GetOrder?orderId=" + orderId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("网络请求失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> parseOrderData(json));
            }
        });
    }

    private void parseOrderData(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("code") != 200) {
                showToast("订单加载失败");
                return;
            }

            JSONObject data = root.getJSONObject("data");
            totalPrice = data.getDouble("SumWallet");
            tvTotalPrice.setText("总金额：¥" + priceFormat.format(totalPrice));

            JSONObject orders = data.getJSONObject("Orders");
            payTypeId = orders.getInt("PayTypesId");
            setPayTypeView(payTypeId);

            JSONObject userMaps = data.getJSONObject("UserMaps");
            selectedAddressId = userMaps.getInt("Id");
            String address = userMaps.getString("Province") + userMaps.getString("City")
                    + userMaps.getString("District") + userMaps.getString("Address");
            tvOrderAddress.setText(address);

            org.json.JSONArray goodsArray = data.getJSONArray("OrderViewModels");
            goodsList.clear();
            for (int i = 0; i < goodsArray.length(); i++) {
                JSONObject obj = goodsArray.getJSONObject(i);
                OrderGoodsBean bean = new OrderGoodsBean();
                bean.setGoodsName(obj.getString("GoodsName"));
                bean.setGoodsAvatar(obj.getString("GoodsAvatar"));
                bean.setGoodsWallet(obj.getDouble("GoodsWallet"));
                bean.setNums(obj.getInt("Nums"));
                goodsList.add(bean);
            }
            goodsAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("数据解析异常");
        }
    }

    // ==================== 原有代码不变 ====================
    private void setPayTypeView(int payTypeId) {
        switch (payTypeId) {
            case 0:
                ivPayIcon.setImageResource(R.drawable.ic_order_wallet);
                tvPayName.setText("余额支付");
                break;
            case 1:
                ivPayIcon.setImageResource(R.drawable.ic_order_wx);
                tvPayName.setText("微信支付");
                break;
            case 2:
                ivPayIcon.setImageResource(R.drawable.ic_order_all);
                tvPayName.setText("微信加余额支付");
                break;
            default:
                ivPayIcon.setImageResource(R.drawable.ic_order_wallet);
                tvPayName.setText("未知支付方式");
        }
    }

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
            org.json.JSONArray dataArray = root.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject obj = dataArray.getJSONObject(i);
                GoodsDetailsActivity.AddressBean bean = new GoodsDetailsActivity.AddressBean();
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
        GoodsDetailsActivity.AddressAdapter adapter = new GoodsDetailsActivity.AddressAdapter(addressList);
        rvAddress.setAdapter(adapter);

        addressDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("选择收货地址")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .create();
        addressDialog.show();

        adapter.setOnAddressClickListener(bean -> {
            selectedAddressId = bean.getId();
            tvOrderAddress.setText(bean.getAddress());
            addressDialog.dismiss();
        });
    }

    private void initOkHttp() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

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

    class OrderGoodsAdapter extends RecyclerView.Adapter<OrderGoodsAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(OrderPayActivity.this).inflate(R.layout.item_order_goods, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            OrderGoodsBean bean = goodsList.get(position);
            holder.tvName.setText(bean.getGoodsName());
            holder.tvPrice.setText("¥" + priceFormat.format(bean.getGoodsWallet()));
            holder.tvNum.setText("x" + bean.getNums());
            loadImage(bean.getGoodsAvatar(), holder.ivGoods);
        }

        @Override
        public int getItemCount() {
            return goodsList.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView ivGoods;
            TextView tvName, tvPrice, tvNum;
            public Holder(@NonNull View itemView) {
                super(itemView);
                ivGoods = itemView.findViewById(R.id.iv_goods);
                tvName = itemView.findViewById(R.id.tv_goods_name);
                tvPrice = itemView.findViewById(R.id.tv_goods_price);
                tvNum = itemView.findViewById(R.id.tv_goods_num);
            }
        }
    }

    static class OrderGoodsBean {
        private String goodsName;
        private String goodsAvatar;
        private double goodsWallet;
        private int nums;

        public String getGoodsName() { return goodsName; }
        public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
        public String getGoodsAvatar() { return goodsAvatar; }
        public void setGoodsAvatar(String goodsAvatar) { this.goodsAvatar = goodsAvatar; }
        public double getGoodsWallet() { return goodsWallet; }
        public void setGoodsWallet(double goodsWallet) { this.goodsWallet = goodsWallet; }
        public int getNums() { return nums; }
        public void setNums(int nums) { this.nums = nums; }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissAllDialog();
    }
}
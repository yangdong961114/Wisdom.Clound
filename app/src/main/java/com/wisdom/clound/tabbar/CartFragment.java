package com.wisdom.clound.tabbar;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.wisdom.clound.Bean.CartResponse;
import com.wisdom.clound.R;
import com.wisdom.clound.ui.goods.OrderPayActivity;
import com.wisdom.clound.utils.SPUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartFragment extends Fragment {
    // 控件声明
    private LinearLayout llCartContent;      // 有数据容器
    private LinearLayout llEmptyCart;        // 空购物车容器
    private LinearLayout llAddressContainer; // 地址容器
    private TextView tvUserName;             // 收货人姓名
    private TextView tvUserPhone;            // 收货人电话
    private TextView tvAddress;              // 收货地址
    private RecyclerView rvCartList;         // 购物车商品列表
    private CheckBox cbSelectAll;            // 全选框
    private TextView tvTotalPrice;           // 合计价格
    private Button btnGoShop;                // 去商城按钮
    private Button btnCheckout;              // 结算按钮

    // 数据变量
    private String userId;                   // 用户ID
    private CartResponse.CartData cartData;  // 购物车数据
    private List<CartResponse.CartItem> cartItemList = new ArrayList<>(); // 商品列表
    private CartAdapter cartAdapter;         // 商品适配器
    private AlertDialog addressDialog;       // 地址选择弹窗
    private List<AddressBean> addressList = new ArrayList<>(); // 地址列表
    private int selectedAddressId = 0;       // 选中的地址ID

    private OkHttpClient okHttpClient;
    private Gson gson;
    private static final String TAG = "CartFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        initViews(view);
        initData();
        return view;
    }

    /**
     * 初始化控件
     */
    private void initViews(View view) {
        // 容器
        llCartContent = view.findViewById(R.id.ll_cart_content);
        llEmptyCart = view.findViewById(R.id.ll_empty_cart);
        // 地址相关
        llAddressContainer = view.findViewById(R.id.ll_address_container);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        tvAddress = view.findViewById(R.id.tv_address);
        // 商品列表
        rvCartList = view.findViewById(R.id.rv_cart_list);
        rvCartList.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(cartItemList);
        rvCartList.setAdapter(cartAdapter);

        // 底部结算栏
        cbSelectAll = view.findViewById(R.id.cb_select_all);
        tvTotalPrice = view.findViewById(R.id.tv_total_price);
        btnCheckout = view.findViewById(R.id.btn_checkout);
        // 空购物车按钮
        btnGoShop = view.findViewById(R.id.btn_go_shop);

        // 初始化点击事件
        initClickEvents();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        okHttpClient = new OkHttpClient();
        gson = new Gson();
        // 获取用户ID
        userId = SPUtils.getUserId(getContext());
        if (TextUtils.isEmpty(userId)) {
            // 无用户ID，显示空购物车
            showEmptyCart();
            return;
        }
        // 有用户ID，请求购物车数据
        requestCartData();
    }

    /**
     * 初始化点击事件
     */
    private void initClickEvents() {
        // 去商城逛逛
        btnGoShop.setOnClickListener(v -> jumpToHomePage());

        // 收货地址点击 → 弹出地址选择弹窗
        llAddressContainer.setOnClickListener(v -> getUserAddressList());

        // 全选框点击
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CartResponse.CartItem item : cartItemList) {
                item.setIsChecket(isChecked);
            }
            cartAdapter.notifyDataSetChanged();
            calculateTotalPrice();
        });

        // 结算按钮
        // 结算按钮
        btnCheckout.setOnClickListener(v -> {
            if (cartItemList.size() == 0) {
                showToast("购物车为空，无法结算");
                return;
            }
            // 1. 校验是否选中商品
            boolean hasSelect = false;
            StringBuilder carIdsBuilder = new StringBuilder();
            for (CartResponse.CartItem item : cartItemList) {
                if (item.isIsChecket()) {
                    hasSelect = true;
                    carIdsBuilder.append(item.getUserCarId()).append(",");
                }
            }
            if (!hasSelect) {
                showToast("请选择要结算的商品");
                return;
            }
            // 2. 处理拼接的ID，去掉最后一个逗号
            String userCarIds = carIdsBuilder.toString();
            if (userCarIds.endsWith(",")) {
                userCarIds = userCarIds.substring(0, userCarIds.length() - 1);
            }
            // 3. 校验是否选择收货地址
            if (selectedAddressId == 0) {
                showToast("请选择收货地址");
                return;
            }
            // 4. 调用创建订单API
            createOrder(userCarIds);
        });
    }

    // ========== 创建订单（结算）API ==========
    private void createOrder(String userCarIds) {
        // 拼接API地址
        String url = "https://api.rzkj.qyqd123.cn/Android/UserCarActivity/CreateOrder" +
                "?userId=" + userId +
                "&userMapId=" + selectedAddressId +
                "&userCarIds=" + userCarIds;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> showToast("网络异常，创建订单失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        if (root.getInt("code") == 200) {
                            // 获取订单ID
                            int orderId = root.getInt("data");
                            showToast("订单创建成功");
                            requestCartData();
                            // 跳转到支付页面
                            Intent intent = new Intent(getContext(), OrderPayActivity.class);
                            intent.putExtra("orderId", orderId);
                            startActivity(intent);
                            // 可选：创建订单成功后刷新购物车（删除已结算商品）
                            // requestCartData();
                        } else {
                            showToast("创建订单失败");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("订单数据解析失败");
                    }
                });
            }
        });
    }

    // ========== 删除购物车商品 API ==========
    private void deleteCartItem(int userCarId, int position) {
        String url = "https://api.rzkj.qyqd123.cn/Android/UserCarActivity/unCollect?userCarId=" + userCarId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> {
                    showToast("删除失败");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject obj = new JSONObject(json);
                    int code = obj.getInt("code");
                    getActivity().runOnUiThread(() -> {
                        if (code == 200) {
                            // 删除成功，移除列表
                            cartItemList.remove(position);
                            cartAdapter.notifyItemRemoved(position);
                            calculateTotalPrice();
                            showToast("删除成功");
                            // 判断是否为空购物车
                            if (cartItemList.isEmpty()) {
                                showEmptyCart();
                            }
                        } else {
                            showToast("删除失败");
                        }
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(() -> {
                        showToast("删除失败");
                    });
                }
            }
        });
    }

    // ========== 购物车数量修改 API ==========
    private void updateCartNumApi(int userCarId, int goodsNums) {
        String url = "https://api.rzkj.qyqd123.cn/Android/UserCarActivity/EditCarNums?userCarId=" + userCarId + "&goodsNums=" + goodsNums;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                response.close();
            }
        });
    }

    /**
     * 请求购物车数据
     */
    private void requestCartData() {
        Log.d(TAG, "requestCartData: " + userId);
        String apiUrl = "https://api.rzkj.qyqd123.cn/Android/UserCarActivity/GetUserCar?userId=" + userId;
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "购物车请求失败：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    showEmptyCart();
                    showToast("购物车加载失败，请稍后重试");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "响应失败或响应体为空");
                    getActivity().runOnUiThread(() -> showEmptyCart());
                    return;
                }

                String jsonStr = response.body().string();
                try {
                    CartResponse cartResponse = gson.fromJson(jsonStr, CartResponse.class);
                    getActivity().runOnUiThread(() -> {
                        if (cartResponse == null || cartResponse.getCode() != 200 || cartResponse.getData() == null) {
                            showEmptyCart();
                            return;
                        }

                        cartData = cartResponse.getData();
                        updateAddressUI();
                        updateCartList(); // 由该方法控制布局显示/隐藏
                    });
                } catch (Exception e) {
                    Log.e(TAG, "JSON解析失败：" + e.getMessage());
                    getActivity().runOnUiThread(() -> {
                        showEmptyCart();
                        showToast("购物车数据解析失败");
                    });
                }
            }
        });
    }

    /**
     * 更新收货地址UI
     */
    private void updateAddressUI() {
        if (cartData.getUserMap() == null) return;

        CartResponse.AddressInfo addressInfo = cartData.getUserMap();
        tvUserName.setText(TextUtils.isEmpty(addressInfo.getUserName()) ? "未知" : addressInfo.getUserName());
        tvUserPhone.setText(TextUtils.isEmpty(addressInfo.getUserPhone()) ? "未知" : addressInfo.getUserPhone());
        String fullAddress = addressInfo.getCityDesc() + " " + addressInfo.getAddress();
        tvAddress.setText(TextUtils.isEmpty(fullAddress) ? "未设置收货地址" : fullAddress);
        selectedAddressId = addressInfo.getId();
    }

    /**
     * 更新购物车商品列表 → 【核心修复】
     */
    private void updateCartList() {
        // 商品列表为空 → 显示空购物车
        if (cartData.getUserCarList() == null || cartData.getUserCarList().isEmpty()) {
            showEmptyCart();
            return;
        }

        // 商品列表不为空 → 显示购物车内容
        cartItemList.clear();
        cartItemList.addAll(cartData.getUserCarList());
        cartAdapter.notifyDataSetChanged();
        calculateTotalPrice();
        cbSelectAll.setChecked(true);

        llCartContent.setVisibility(View.VISIBLE);
        llEmptyCart.setVisibility(View.GONE);
    }

    /**
     * 计算选中商品总价
     */
    private void calculateTotalPrice() {
        double total = 0.0;
        for (CartResponse.CartItem item : cartItemList) {
            if (item.isIsChecket()) {
                total += item.getGoodsWallet() * item.getGoodsNums();
            }
        }
        tvTotalPrice.setText("合计: ¥" + String.format("%.2f", total));
    }

    /**
     * 显示空购物车
     */
    private void showEmptyCart() {
        llCartContent.setVisibility(View.GONE);
        llEmptyCart.setVisibility(View.VISIBLE);
    }

    // ==================== 地址功能 ====================
    private void getUserAddressList() {
        String url = "https://api.rzkj.qyqd123.cn/Android/UserMap/GetUserMap?userId=" + userId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> showToast("获取地址失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                getActivity().runOnUiThread(() -> parseAddressData(json));
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

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_address_list, null);
        RecyclerView rvAddress = dialogView.findViewById(R.id.rv_address_list);
        rvAddress.setLayoutManager(new LinearLayoutManager(getActivity()));
        AddressAdapter adapter = new AddressAdapter(addressList);
        rvAddress.setAdapter(adapter);

        addressDialog = new AlertDialog.Builder(getActivity())
                .setTitle("选择收货地址")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .create();
        addressDialog.show();

        adapter.setOnAddressClickListener(bean -> {
            selectedAddressId = bean.getId();
            tvUserName.setText(bean.getUserName());
            tvUserPhone.setText(bean.getUserPhone());
            tvAddress.setText(bean.getAddress());
            addressDialog.dismiss();
        });
    }

    /**
     * 跳转到Home页
     */
    private void jumpToHomePage() {
        if (getActivity() == null) {
            showToast("跳转失败，请稍后重试");
            return;
        }

        ViewPager2 viewPager2 = getActivity().findViewById(R.id.viewPager);
        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);

        if (viewPager2 == null || bottomNav == null) {
            showToast("页面容器异常");
            return;
        }

        viewPager2.setCurrentItem(0, true);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    /**
     * 购物车商品适配器
     */
    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
        private List<CartResponse.CartItem> mList;

        public CartAdapter(List<CartResponse.CartItem> list) {
            this.mList = list;
        }

        @NonNull
        @Override
        public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cart_goods, parent, false);
            return new CartViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
            CartResponse.CartItem item = mList.get(position);
            holder.cbGoodsSelect.setChecked(item.isIsChecket());
            holder.cbGoodsSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setIsChecket(isChecked);
                calculateTotalPrice();
            });
            Glide.with(getContext())
                    .load(item.getGoodsAvatar())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.ivGoodsAvatar);
            holder.tvGoodsName.setText(item.getGoodsName());
            holder.tvGoodsPrice.setText("¥" + String.format("%.2f", item.getGoodsWallet()));
            holder.tvGoodsNum.setText(String.valueOf(item.getGoodsNums()));

            // 减数量
            holder.btnMinus.setOnClickListener(v -> {
                int num = item.getGoodsNums();
                if (num > 1) {
                    num--;
                    item.setGoodsNums(num);
                    holder.tvGoodsNum.setText(String.valueOf(num));
                    calculateTotalPrice();
                    updateCartNumApi(item.getUserCarId(), num);
                } else {
                    showToast("产品数量不能小于1");
                }
            });

            // 加数量
            holder.btnPlus.setOnClickListener(v -> {
                int num = item.getGoodsNums();
                num++;
                item.setGoodsNums(num);
                holder.tvGoodsNum.setText(String.valueOf(num));
                calculateTotalPrice();
                updateCartNumApi(item.getUserCarId(), num);
            });

            // 右下角 删除按钮点击事件
            holder.tvDelete.setOnClickListener(v -> {
                deleteCartItem(item.getUserCarId(), position);
            });
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        class CartViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbGoodsSelect;
            ImageView ivGoodsAvatar;
            TextView tvGoodsName;
            TextView tvGoodsPrice;
            Button btnMinus;
            TextView tvGoodsNum;
            Button btnPlus;
            TextView tvDelete; // 删除按钮

            public CartViewHolder(@NonNull View itemView) {
                super(itemView);
                cbGoodsSelect = itemView.findViewById(R.id.cb_goods_select);
                ivGoodsAvatar = itemView.findViewById(R.id.iv_goods_avatar);
                tvGoodsName = itemView.findViewById(R.id.tv_goods_name);
                tvGoodsPrice = itemView.findViewById(R.id.tv_goods_price);
                btnMinus = itemView.findViewById(R.id.btn_minus);
                tvGoodsNum = itemView.findViewById(R.id.tv_goods_num);
                btnPlus = itemView.findViewById(R.id.btn_plus);
                tvDelete = itemView.findViewById(R.id.tv_delete); // 绑定删除按钮
            }
        }
    }

    // ==================== 地址适配器 ====================
    private class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
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
            holder.tvUserName.setText(bean.getUserName());
            holder.tvUserPhone.setText(bean.getUserPhone());
            holder.tvAddress.setText(bean.getAddress());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(bean);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserPhone, tvAddress;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tv_user_name);
                tvUserPhone = itemView.findViewById(R.id.tv_user_phone);
                tvAddress = itemView.findViewById(R.id.tv_address);
            }
        }
    }

    // 地址点击接口
    interface OnAddressClickListener {
        void onClick(AddressBean bean);
    }

    // 地址实体类
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
    // ==================== 统一封装Toast ====================
    private void showToast(String msg) {
        if (!isAdded() || getContext() == null || TextUtils.isEmpty(msg)) {
            return;
        }
        TextView textView = new TextView(getContext());
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(0xFFFFFFFF);
        textView.setBackgroundColor(0xCC000000);
        textView.setPadding(50, 25, 50, 25);
        textView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(getContext());
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (addressDialog != null && addressDialog.isShowing()) {
            addressDialog.dismiss();
        }
    }
}
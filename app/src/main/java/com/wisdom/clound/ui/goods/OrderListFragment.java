package com.wisdom.clound.ui.goods;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

import org.json.JSONArray;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrderListFragment extends Fragment {
    private static final String ARG_TYPE_ID = "typeId";
    private int typeId;
    private int page = 1;

    private RecyclerView rvOrder;
    private SwipeRefreshLayout swipeRefresh;
    private OrderAdapter adapter;
    private List<OrderBean> orderList = new ArrayList<>();
    private OkHttpClient okHttpClient;
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");
    private Handler handler = new Handler(Looper.getMainLooper());

    // 支付页面回调
    private ActivityResultLauncher<Intent> payLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    refreshData();
                }
            }
    );

    public static OrderListFragment newInstance(int typeId) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE_ID, typeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            typeId = getArguments().getInt(ARG_TYPE_ID);
        }
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);
        rvOrder = view.findViewById(R.id.rv_order_list);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        rvOrder.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter();
        rvOrder.setAdapter(adapter);

        initSwipeRefresh();
        getOrderList();
        return view;
    }

    // 下拉刷新
    private void initSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_red_light,
                android.R.color.holo_blue_light
        );
        swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    // 统一刷新
    private void refreshData() {
        page = 1;
        orderList.clear();
        adapter.notifyDataSetChanged();
        getOrderList();
    }

    // Toast
    private void showToast(String msg) {
        if (msg == null || msg.isEmpty() || getContext() == null) return;
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

    // 请求订单列表
    private void getOrderList() {
        String userId = SPUtils.getUserId(getContext());
        String url = "https://api.rzkj.qyqd123.cn/Android/OrderActivity/GetOrderList" +
                "?userId=" + userId +
                "&typeId=" + typeId +
                "&page=" + page;
        Log.d("OrderList", "请求地址：" + url);

        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) getActivity().runOnUiThread(() -> {
                    showToast("网络异常");
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                if (isAdded()) getActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    parseOrderList(json);
                });
            }
        });
    }

    // 删除订单
    private void deleteOrder(int orderId, int position) {
        String url = "https://api.rzkj.qyqd123.cn/Android/OrderActivity/UnOrder?orderId=" + orderId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) getActivity().runOnUiThread(() -> showToast("网络异常，删除失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                if (isAdded()) getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        if (root.getInt("code") == 200) {
                            orderList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, orderList.size());
                            showToast(root.getString("msg"));
                        } else {
                            showToast("删除失败");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("数据解析失败");
                    }
                });
            }
        });
    }

    // 确认收货
    private void confirmOrder(int orderId) {
        String url = "https://api.rzkj.qyqd123.cn/Android/OrderActivity/ConfirmOrder?orderId=" + orderId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) getActivity().runOnUiThread(() -> showToast("网络异常，确认收货失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                if (isAdded()) getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        if (root.getInt("code") == 200) {
                            showToast(root.getString("msg"));
                            refreshData();
                        } else {
                            showToast("确认收货失败");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("数据解析失败");
                    }
                });
            }
        });
    }

    // 解析订单（支持多商品）
    private void parseOrderList(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("code") != 200) return;

            JSONArray dataArray = root.getJSONArray("data");
            orderList.clear();

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject itemObj = dataArray.getJSONObject(i);
                JSONObject ordersObj = itemObj.getJSONObject("Orders");
                JSONObject userMapObj = itemObj.getJSONObject("UserMaps");
                JSONArray goodsArray = itemObj.getJSONArray("OrderViewModels");

                // 订单基础信息
                int orderId = ordersObj.getInt("Id");
                String orderNumber = ordersObj.getString("OrderNumber");
                String orderStatus = ordersObj.getString("StrIsPay");
                int payTypesId = ordersObj.getInt("PayTypesId");
                double totalPrice = itemObj.getDouble("SumWallet");

                // 地址信息
                String userName = userMapObj.getString("UserName");
                String userPhone = userMapObj.getString("UserPhone");
                String cityDesc = userMapObj.getString("CityDesc");
                String address = userMapObj.getString("Address");
                String fullAddress = cityDesc + address + "（" + userName + " " + userPhone + "）";

                // 多商品解析
                List<OrderGoodsBean> goodsList = new ArrayList<>();
                for (int j = 0; j < goodsArray.length(); j++) {
                    JSONObject goodsObj = goodsArray.getJSONObject(j);
                    OrderGoodsBean goodsBean = new OrderGoodsBean();
                    goodsBean.setGoodsName(goodsObj.getString("GoodsName"));
                    goodsBean.setGoodsAvatar(goodsObj.getString("GoodsAvatar"));
                    goodsBean.setNums(goodsObj.getInt("Nums"));
                    goodsBean.setGoodsPrice(goodsObj.getDouble("GoodsWallet"));
                    goodsList.add(goodsBean);
                }

                // 封装订单
                OrderBean bean = new OrderBean();
                bean.setOrderId(orderId);
                bean.setOrderNumber(orderNumber);
                bean.setOrderStatus(orderStatus);
                bean.setPayTypesId(payTypesId);
                bean.setAddress(fullAddress);
                bean.setTotalPrice(totalPrice);
                bean.setTypeId(typeId);
                bean.setGoodsList(goodsList);

                orderList.add(bean);
            }
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("数据解析失败：" + e.getMessage());
        }
    }

    // 订单适配器
    class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_order, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            OrderBean bean = orderList.get(position);
            holder.tvOrderNumber.setText("订单号：" + bean.getOrderNumber());
            holder.tvOrderStatus.setText(bean.getOrderStatus());
            holder.tvAddress.setText("收货地址：" + bean.getAddress());
            holder.tvTotalPrice.setText("订单总价：¥" + priceFormat.format(bean.getTotalPrice()));
            setPayType(holder.ivPayIcon, holder.tvPayName, bean.getPayTypesId());
            setActionButtons(holder, bean.getTypeId());

            // 绑定多商品列表
            holder.rvOrderGoods.setLayoutManager(new LinearLayoutManager(getContext()));
            GoodsItemAdapter goodsAdapter = new GoodsItemAdapter(bean.getGoodsList());
            holder.rvOrderGoods.setAdapter(goodsAdapter);

            // 按钮事件
            holder.btnAction1.setOnClickListener(v -> clickButton1(bean));
            holder.btnAction2.setOnClickListener(v -> clickButton2(bean, position));
            holder.btnAction3.setOnClickListener(v -> clickButton3(bean));
        }

        @Override
        public int getItemCount() {
            return orderList.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvOrderNumber, tvOrderStatus, tvAddress, tvTotalPrice;
            ImageView ivPayIcon;
            TextView tvPayName;
            RecyclerView rvOrderGoods;
            TextView btnAction1, btnAction2, btnAction3;

            public Holder(@NonNull View itemView) {
                super(itemView);
                tvOrderNumber = itemView.findViewById(R.id.tv_order_number);
                tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
                tvAddress = itemView.findViewById(R.id.tv_address);
                tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
                ivPayIcon = itemView.findViewById(R.id.iv_pay_icon);
                tvPayName = itemView.findViewById(R.id.tv_pay_name);
                rvOrderGoods = itemView.findViewById(R.id.rv_order_goods);
                btnAction1 = itemView.findViewById(R.id.btn_action1);
                btnAction2 = itemView.findViewById(R.id.btn_action2);
                btnAction3 = itemView.findViewById(R.id.btn_action3);
            }
        }
    }

    // 商品子适配器
    class GoodsItemAdapter extends RecyclerView.Adapter<GoodsItemAdapter.GoodsHolder> {
        private List<OrderGoodsBean> goodsList;

        public GoodsItemAdapter(List<OrderGoodsBean> goodsList) {
            this.goodsList = goodsList;
        }

        @NonNull
        @Override
        public GoodsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_order_goods, parent, false);
            return new GoodsHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GoodsHolder holder, int position) {
            OrderGoodsBean bean = goodsList.get(position);
            holder.tvGoodsName.setText(bean.getGoodsName());
            holder.tvGoodsPrice.setText("¥" + priceFormat.format(bean.getGoodsPrice()));
            holder.tvGoodsNum.setText("x" + bean.getNums());
            loadImage(bean.getGoodsAvatar(), holder.ivGoods);
        }

        @Override
        public int getItemCount() {
            return goodsList.size();
        }

        class GoodsHolder extends RecyclerView.ViewHolder {
            ImageView ivGoods;
            TextView tvGoodsName, tvGoodsPrice, tvGoodsNum;

            public GoodsHolder(@NonNull View itemView) {
                super(itemView);
                ivGoods = itemView.findViewById(R.id.iv_goods);
                tvGoodsName = itemView.findViewById(R.id.tv_goods_name);
                tvGoodsPrice = itemView.findViewById(R.id.tv_goods_price);
                tvGoodsNum = itemView.findViewById(R.id.tv_goods_num);
            }
        }
    }

    // 支付方式
    private void setPayType(ImageView iv, TextView tv, int payTypeId) {
        switch (payTypeId) {
            case 0:
                iv.setImageResource(R.drawable.ic_order_wallet);
                tv.setText("余额支付");
                break;
            case 1:
                iv.setImageResource(R.drawable.ic_order_wx);
                tv.setText("微信支付");
                break;
            case 2:
                iv.setImageResource(R.drawable.ic_order_all);
                tv.setText("微信加余额支付");
                break;
            default:
                iv.setImageResource(R.drawable.ic_order_wallet);
                tv.setText("未知方式");
        }
    }

    // 按钮显示
    private void setActionButtons(OrderAdapter.Holder holder, int typeId) {
        holder.btnAction1.setVisibility(View.GONE);
        holder.btnAction2.setVisibility(View.GONE);
        holder.btnAction3.setVisibility(View.GONE);

        switch (typeId) {
            case 1: // 待付款
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("删除订单");
                holder.btnAction3.setVisibility(View.VISIBLE);
                holder.btnAction3.setText("确认付款");
                break;
            case 2: // 已付款
                holder.btnAction3.setVisibility(View.VISIBLE);
                holder.btnAction3.setText("再来一单");
                break;
            case 3: // 已发货
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("再来一单");
                holder.btnAction3.setVisibility(View.VISIBLE);
                holder.btnAction3.setText("确认收货");
                break;
            case 4: // 已收货
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("再来一单");
                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("删除订单");
                break;
        }
    }

    // 按钮1
    private void clickButton1(OrderBean bean) {
        int pos = orderList.indexOf(bean);
        if (pos != -1) {
            TextView btn = rvOrder.findViewHolderForAdapterPosition(pos).itemView.findViewById(R.id.btn_action1);
            String btnText = btn.getText().toString();
            if (btnText.equals("删除订单")) {
                showDeleteDialog(bean, pos);
            } else if (btnText.equals("再来一单")) {
                againOrder(bean.getOrderId());
            }
        }
    }

    // 按钮2
    private void clickButton2(OrderBean bean, int position) {
        showDeleteDialog(bean, position);
    }

    // 删除弹窗
    private void showDeleteDialog(OrderBean bean, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("提示")
                .setMessage("确定要删除该订单吗？")
                .setPositiveButton("确定", (dialog, which) -> deleteOrder(bean.getOrderId(), position))
                .setNegativeButton("取消", null)
                .show();
    }

    // 按钮3
    private void clickButton3(OrderBean bean) {
        if (bean.getTypeId() == 1) {
            Intent intent = new Intent(getContext(), OrderPayActivity.class);
            intent.putExtra("orderId", bean.getOrderId());
            payLauncher.launch(intent);
        }
        if (bean.getTypeId() == 2) {
            againOrder(bean.getOrderId());
        }
        if (bean.getTypeId() == 3) {
            confirmOrder(bean.getOrderId());
        }
    }

    // 加载图片
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

    // 再来一单
    private void againOrder(int oldOrderId) {
        String url = "https://api.rzkj.qyqd123.cn/Android/OrderActivity/AddOrder?orderId=" + oldOrderId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) getActivity().runOnUiThread(() -> showToast("网络异常，下单失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                if (isAdded()) getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        if (root.getInt("code") == 200) {
                            int newOrderId = root.getInt("data");
                            showToast(root.getString("msg"));
                            Intent intent = new Intent(getContext(), OrderPayActivity.class);
                            intent.putExtra("orderId", newOrderId);
                            payLauncher.launch(intent);
                        } else {
                            showToast("下单失败：" + root.getString("msg"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("数据解析失败");
                    }
                });
            }
        });
    }

    // 订单实体类
    static class OrderBean {
        private int orderId;
        private String orderNumber;
        private String orderStatus;
        private int payTypesId;
        private String address;
        private double totalPrice;
        private int typeId;
        private List<OrderGoodsBean> goodsList;

        public int getOrderId() {return orderId;}
        public void setOrderId(int orderId) {this.orderId = orderId;}
        public String getOrderNumber() {return orderNumber;}
        public void setOrderNumber(String orderNumber) {this.orderNumber = orderNumber;}
        public String getOrderStatus() {return orderStatus;}
        public void setOrderStatus(String orderStatus) {this.orderStatus = orderStatus;}
        public int getPayTypesId() {return payTypesId;}
        public void setPayTypesId(int payTypesId) {this.payTypesId = payTypesId;}
        public String getAddress() {return address;}
        public void setAddress(String address) {this.address = address;}
        public double getTotalPrice() {return totalPrice;}
        public void setTotalPrice(double totalPrice) {this.totalPrice = totalPrice;}
        public int getTypeId() {return typeId;}
        public void setTypeId(int typeId) {this.typeId = typeId;}
        public List<OrderGoodsBean> getGoodsList() {return goodsList;}
        public void setGoodsList(List<OrderGoodsBean> goodsList) {this.goodsList = goodsList;}
    }

    // 商品实体类
    static class OrderGoodsBean {
        private String goodsName;
        private String goodsAvatar;
        private int nums;
        private double goodsPrice;

        public String getGoodsName() {return goodsName;}
        public void setGoodsName(String goodsName) {this.goodsName = goodsName;}
        public String getGoodsAvatar() {return goodsAvatar;}
        public void setGoodsAvatar(String goodsAvatar) {this.goodsAvatar = goodsAvatar;}
        public int getNums() {return nums;}
        public void setNums(int nums) {this.nums = nums;}
        public double getGoodsPrice() {return goodsPrice;}
        public void setGoodsPrice(double goodsPrice) {this.goodsPrice = goodsPrice;}
    }
}
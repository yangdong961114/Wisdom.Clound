package com.wisdom.clound.ui.bouns;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BounsIndexActivity extends AppCompatActivity {
    private ImageView ivBack;
    private TextView tab0, tab1, tab2;
    private TextView tvMaxTitle; // 宫格第三个标题（动态修改）
    private TextView tvTotal, tvToday, tvMax, tvDesc, tvNoPermission;
    private RecyclerView rvBouns;

    private String userId;
    private int currentType = 0; // 0=推荐奖励 1=团队业绩 2=广告收益
    private OkHttpClient okHttpClient;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private BounsAdapter adapter;
    private List<BounsBean> bounsList = new ArrayList<>();

    // 团队业绩专用字段
    private String userGradeFee = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bouns_index);
        // 接收上一页传递的userId
        userId = getIntent().getStringExtra("userId");
        if (userId == null) userId = SPUtils.getUserId(this);

        initView();
        initTab();
        // 初始默认显示正常页面
        setNormalViewVisible(true);
        getBounsData();
    }

    // 初始化控件
    private void initView() {
        ivBack = findViewById(R.id.iv_back);
        tab0 = findViewById(R.id.tab_type0);
        tab1 = findViewById(R.id.tab_type1);
        tab2 = findViewById(R.id.tab_type2);
        tvTotal = findViewById(R.id.tv_total);
        tvToday = findViewById(R.id.tv_today);
        tvMax = findViewById(R.id.tv_max);
        tvDesc = findViewById(R.id.tv_desc);
        rvBouns = findViewById(R.id.rv_bouns);
        tvNoPermission = findViewById(R.id.tv_no_permission);
        tvMaxTitle = findViewById(R.id.tv_max_title);

        // 返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 初始化列表
        rvBouns.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BounsAdapter();
        rvBouns.setAdapter(adapter);

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // 初始化选项卡点击事件
    private void initTab() {
        tab0.setOnClickListener(v -> switchTab(0));
        tab1.setOnClickListener(v -> switchTab(1));
        tab2.setOnClickListener(v -> switchTab(2));
    }

    /**
     * 控制正常页面显示/隐藏
     */
    private void setNormalViewVisible(boolean visible) {
        int status = visible ? View.VISIBLE : View.GONE;
        findViewById(R.id.layout_stats).setVisibility(status);
        tvDesc.setVisibility(status);
        rvBouns.setVisibility(status);
        // 无资格提示取反
        tvNoPermission.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    // 切换选项卡
    private void switchTab(int type) {
        currentType = type;
        // 重置颜色
        tab0.setTextColor(getResources().getColor(R.color.gray));
        tab1.setTextColor(getResources().getColor(R.color.gray));
        tab2.setTextColor(getResources().getColor(R.color.gray));

        // 选中状态
        if (type == 0) {
            tvMaxTitle.setText("最大单型");
            tab0.setTextColor(getResources().getColor(R.color.blue));
        } else if (type == 1) {
            tvMaxTitle.setText("分成比例");
            tab1.setTextColor(getResources().getColor(R.color.blue));
        } else {
            tvMaxTitle.setText("分成比例");
            tab2.setTextColor(getResources().getColor(R.color.blue));
        }
        // 重新请求数据
        getBounsData();
    }

    // 请求分红数据
    private void getBounsData() {
        String url = "https://api.rzkj.qyqd123.cn/Android/BounsActivity/GetBounsList?userId=" + userId + "&types=" + currentType;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(BounsIndexActivity.this, "网络异常", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> parseData(json));
            }
        });
    }

    // 解析数据（兼容推荐奖励+团队业绩）
    private void parseData(String json) {
        try {
            JSONObject root = new JSONObject(json);
            // 判断未获得分红资格
            String msg = root.optString("msg");
            if ("未获得分红资格".equals(msg)) {
                setNormalViewVisible(false);
                return;
            }

            // 正常数据
            setNormalViewVisible(true);
            if (root.getInt("code") != 200) return;

            JSONObject data = root.getJSONObject("data");
            // 公共赋值
            tvDesc.setText(data.getString("StrHour"));
            double sumBouns = data.getDouble("SumBounsWallet");
            double maxToday = data.getDouble("MaxTodayWallet");
            tvTotal.setText(df.format(sumBouns));
            tvToday.setText(df.format(maxToday));

            // 根据类型赋值宫格第三个数据
            if (currentType == 0) {
                // 推荐奖励：最大单型
                double maxBouns = data.getDouble("MaxBounsWallet");
                tvMax.setText(df.format(maxBouns));
            } else if (currentType == 1) {
                // 团队业绩：团队级别
                userGradeFee = data.optString("UserGradeFee", "0%");
                tvMax.setText(userGradeFee);
            } else {
                userGradeFee = data.optString("UserGradeFee", "0%");
                tvMax.setText(userGradeFee);
            }

            // 解析列表
            JSONArray listArray = data.getJSONArray("List");
            bounsList.clear();
            for (int i = 0; i < listArray.length(); i++) {
                JSONObject obj = listArray.getJSONObject(i);
                BounsBean bean = new BounsBean();
                bean.setId(obj.getInt("Id"));
                bean.setIsStatus(obj.getInt("IsStatus"));
                bean.setDays(obj.getString("Days"));
                bean.setMaxDays(obj.getString("MaxDays"));
                bean.setOrderNumber(obj.getString("OrderNumber"));
                bean.setShareUserName(obj.getString("ShareUserName"));
                bean.setShareUserLoginName(obj.getString("ShareUserLoginName"));
                bean.setWallet(obj.getDouble("Wallet"));
                bean.setAddWallet(obj.getDouble("AddWallet"));
                bean.setTodayWallet(obj.getDouble("TodayWallet"));
                bean.setStrCreateDate(obj.getString("StrCreateDate"));
                bean.setStrStatus(obj.getString("StrStatus"));

                // 团队业绩专属字段
                bean.setUserGrade(obj.optString("UserGrade"));
                bean.setSumUser(obj.optInt("SumUser"));
                bean.setUserFee(obj.optString("UserFee"));
                bean.setStrUserGrade(obj.optString("StrUserGrade"));

                bounsList.add(bean);
            }
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据解析失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 列表适配器
    class BounsAdapter extends RecyclerView.Adapter<BounsAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_bouns, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            BounsBean bean = bounsList.get(position);

            // ============== 状态颜色（两种类型通用） ==============
            holder.tvStatus.setText(bean.getStrStatus());
            if (bean.getIsStatus() == 0) {
                holder.tvStatus.setTextColor(getResources().getColor(R.color.blue));
            } else {
                holder.tvStatus.setTextColor(getResources().getColor(R.color.red));
            }
            // 日期
            holder.tvDate.setText(bean.getStrCreateDate());

            // ============== 根据类型切换展示内容 ==============
            if (currentType == 0) {
                // 推荐奖励分成
                String desc = bean.getShareUserLoginName() + "(" + bean.getShareUserName() + ")\n购买订单(" + bean.getOrderNumber() + ")";
                holder.tvDesc.setText(desc);
                holder.tvDays.setText("全部分红需" + bean.getDays() + "完成\n当前第" + bean.getMaxDays());
                holder.tvWallet.setText("全部分红\n" + df.format(bean.getWallet()));
                holder.tvAddWallet.setText("已分金额\n" + df.format(bean.getAddWallet()));
                holder.tvTodayWallet.setText("今日可分\n" + df.format(bean.getTodayWallet()));
            } else {
                // 团队业绩分成
                String desc = "管理级别：" + bean.getStrUserGrade() + "\n分配比例：" + bean.getUserFee() + " | 分成人数：" + bean.getSumUser();
                holder.tvDesc.setText(desc);
                holder.tvDays.setText("全部分红需" + bean.getDays() + "完成\n当前第" + bean.getMaxDays());
                holder.tvWallet.setText("全部分红\n" + df.format(bean.getWallet()));
                holder.tvAddWallet.setText("已分金额\n" + df.format(bean.getAddWallet()));
                holder.tvTodayWallet.setText("今日可分\n" + df.format(bean.getTodayWallet()));
            }
        }

        @Override
        public int getItemCount() {
            return bounsList.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvDesc, tvDays, tvStatus, tvWallet, tvAddWallet, tvTodayWallet, tvDate;

            public Holder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_bouns_desc);
                tvDays = itemView.findViewById(R.id.tv_days);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvWallet = itemView.findViewById(R.id.tv_wallet);
                tvAddWallet = itemView.findViewById(R.id.tv_add_wallet);
                tvTodayWallet = itemView.findViewById(R.id.tv_today_wallet);
                tvDate = itemView.findViewById(R.id.tv_date);
            }
        }
    }

    // 分红实体类（兼容两种类型）
    static class BounsBean {
        private int Id;
        private int IsStatus;
        private String Days;
        private String MaxDays;
        private String OrderNumber;
        private String ShareUserName;
        private String ShareUserLoginName;
        private double Wallet;
        private double AddWallet;
        private double TodayWallet;
        private String StrCreateDate;
        private String StrStatus;

        // 团队业绩专属字段
        private String UserGrade;
        private int SumUser;
        private String UserFee;
        private String StrUserGrade;
        private String UserGradeFee;

        public int getId() {return Id;}
        public void setId(int id) {Id = id;}
        public int getIsStatus() {return IsStatus;}
        public void setIsStatus(int isStatus) {IsStatus = isStatus;}
        public String getDays() {return Days;}
        public void setDays(String days) {Days = days;}
        public String getMaxDays() {return MaxDays;}
        public void setMaxDays(String maxDays) {MaxDays = maxDays;}
        public String getOrderNumber() {return OrderNumber;}
        public void setOrderNumber(String orderNumber) {OrderNumber = orderNumber;}
        public String getShareUserName() {return ShareUserName;}
        public void setShareUserName(String shareUserName) {ShareUserName = shareUserName;}
        public String getShareUserLoginName() {return ShareUserLoginName;}
        public void setShareUserLoginName(String shareUserLoginName) {ShareUserLoginName = shareUserLoginName;}
        public double getWallet() {return Wallet;}
        public void setWallet(double wallet) {Wallet = wallet;}
        public double getAddWallet() {return AddWallet;}
        public void setAddWallet(double addWallet) {AddWallet = addWallet;}
        public double getTodayWallet() {return TodayWallet;}
        public void setTodayWallet(double todayWallet) {TodayWallet = todayWallet;}
        public String getStrCreateDate() {return StrCreateDate;}
        public void setStrCreateDate(String strCreateDate) {StrCreateDate = strCreateDate;}
        public String getStrStatus() {return StrStatus;}
        public void setStrStatus(String strStatus) {StrStatus = strStatus;}

        public String getUserGrade() {return UserGrade;}
        public void setUserGrade(String userGrade) {UserGrade = userGrade;}
        public int getSumUser() {return SumUser;}
        public void setSumUser(int sumUser) {SumUser = sumUser;}
        public String getUserFee() {return UserFee;}
        public void setUserFee(String userFee) {UserFee = userFee;}
        public String getStrUserGrade() {return StrUserGrade;}
        public void setStrUserGrade(String strUserGrade) {StrUserGrade = strUserGrade;}
        public String getUserGradeFee() {return UserGradeFee;}
        public void setUserGradeFee(String userGradeFee) {UserGradeFee = userGradeFee;}
    }
}
package com.wisdom.clound.ui.wallet;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WalletIndexActivity extends AppCompatActivity {
    // 控件
    private ImageView ivBack;
    private TextView tvWallet, tvPoints, tvAmount;
    private LinearLayout llTabContainer;
    private RecyclerView rvWallet;
    private Button btnRecharge, btnWithdraw;

    // 数据
    private String userId;
    private int currentTypesId = 1;
    private OkHttpClient okHttpClient;
    private final DecimalFormat df = new DecimalFormat("0.00");
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 列表
    private WalletAdapter adapter;
    private List<WalletRecord> recordList = new ArrayList<>();

    // Tab
    private TextView selectedTab;

    // 微信支付
    private IWXAPI wxApi;
    private static final String WX_APP_ID = "wxf5260230f4286cce";

    // 支付密码弹窗
    private Dialog pwdDialog;
    private EditText[] passwordEditTexts = new EditText[6];
    private int currentPosition = 0;
    private double withdrawMoney = 0;
    private double cashSale = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_index);

        // 初始化微信
        wxApi = WXAPIFactory.createWXAPI(this, WX_APP_ID, true);
        wxApi.registerApp(WX_APP_ID);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) userId = SPUtils.getUserId(this);

        initView();
        initOkHttp();
        getWalletData();
    }

    // 初始化控件
    private void initView() {
        ivBack = findViewById(R.id.iv_back);
        tvWallet = findViewById(R.id.tv_wallet);
        tvPoints = findViewById(R.id.tv_points);
        tvAmount = findViewById(R.id.tv_amount);
        llTabContainer = findViewById(R.id.ll_tab_container);
        rvWallet = findViewById(R.id.rv_wallet);
        btnRecharge = findViewById(R.id.btn_recharge);
        btnWithdraw = findViewById(R.id.btn_withdraw);

        ivBack.setOnClickListener(v -> finish());
        rvWallet.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalletAdapter(recordList);
        rvWallet.setAdapter(adapter);

        // 按钮点击
        btnRecharge.setOnClickListener(v -> showRechargeDialog());
        btnWithdraw.setOnClickListener(v -> getWithdrawConfig());
    }

    // ====================== 1. 充值功能 ======================
    private void showRechargeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("微信充值");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input_money, null);
        EditText etMoney = view.findViewById(R.id.et_money);
        builder.setView(view);
        builder.setPositiveButton("确认充值", (dialog, which) -> {
            String moneyStr = etMoney.getText().toString().trim();
            if (moneyStr.isEmpty()) {
                showToast("请输入金额");
                return;
            }
            double money = Double.parseDouble(moneyStr);
            weChatRecharge(money);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 微信充值请求
    private void weChatRecharge(double money) {
        JSONObject params = new JSONObject();
        try {
            params.put("userId", userId);
            params.put("money", money);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());
        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/WalletActivity/WeChatRecharge")
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()-> showToast("网络请求失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(()->{
                    try {
                        JSONObject root = new JSONObject(json);
                        if (root.getInt("code") == 200) {
                            startWxPay(root.getJSONObject("data"));
                        } else {
                            showToast(root.getString("msg"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("充值参数解析失败");
                    }
                });
            }
        });
    }

    // ====================== 2. 提现功能 ======================
    private void getWithdrawConfig() {
        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/WalletActivity/GetWeChatConfig")
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()-> showToast("获取提现配置失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(()-> parseWithdrawConfig(json));
            }
        });
    }

    private void parseWithdrawConfig(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("code") == 200) {
                JSONObject data = root.getJSONObject("data");
                cashSale = data.getDouble("CashSale");
                showWithdrawDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("配置解析失败");
        }
    }

    private void showWithdrawDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("微信提现");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input_money, null);
        EditText etMoney = view.findViewById(R.id.et_money);
        TextView tvTip = view.findViewById(R.id.tv_tip);
        tvTip.setText("温馨提示：提现将扣除 " + cashSale + "% 的手续费");
        tvTip.setVisibility(View.VISIBLE);
        builder.setView(view);
        builder.setPositiveButton("确认提现", (dialog, which) -> {
            String moneyStr = etMoney.getText().toString().trim();
            if (moneyStr.isEmpty()) {
                showToast("请输入金额");
                return;
            }
            withdrawMoney = Double.parseDouble(moneyStr);
            showSixPasswordDialog();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ====================== 修复核心：密码输入完成 → 调用微信提现 ======================
    private void submitWithdraw(String payPwd) {
        JSONObject params = new JSONObject();
        try {
            params.put("userId", userId);
            params.put("money", withdrawMoney);
            params.put("userPayPwd", payPwd);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, params.toString());
        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/WalletActivity/WeChatWithdraw")
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()->{
                    dismissAllDialog();
                    showToast("提现请求失败");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(()->{
                    try {
                        JSONObject root = new JSONObject(json);
                        dismissAllDialog();
                        if (root.getInt("code") == 200) {
                            // 修复：获取微信参数并拉起APP
                            JSONObject data = root.getJSONObject("data");
                            startWxPay(data);
                            showToast("已拉起微信提现");
                            getWalletData();
                        } else {
                            showToast(root.getString("msg"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("微信提现参数错误");
                    }
                });
            }
        });
    }

    // ====================== 支付密码弹窗 ======================
    private void showSixPasswordDialog() {
        currentPosition = 0;
        pwdDialog = new Dialog(this, R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pay_password, null);
        pwdDialog.setContentView(view);
        pwdDialog.setCancelable(true);

        if (pwdDialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = pwdDialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            params.gravity = Gravity.CENTER;
            pwdDialog.getWindow().setAttributes(params);
        }

        passwordEditTexts[0] = view.findViewById(R.id.et_pwd_1);
        passwordEditTexts[1] = view.findViewById(R.id.et_pwd_2);
        passwordEditTexts[2] = view.findViewById(R.id.et_pwd_3);
        passwordEditTexts[3] = view.findViewById(R.id.et_pwd_4);
        passwordEditTexts[4] = view.findViewById(R.id.et_pwd_5);
        passwordEditTexts[5] = view.findViewById(R.id.et_pwd_6);
        TextView tvCancel = view.findViewById(R.id.tv_cancel);

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

        for (EditText et : passwordEditTexts) et.setText("");

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

        btnDel.setOnClickListener(v -> deletePassword());
        tvCancel.setOnClickListener(v -> pwdDialog.dismiss());

        pwdDialog.show();
    }

    private void inputPassword(String num) {
        if (currentPosition < 6) {
            passwordEditTexts[currentPosition].setText(num);
            currentPosition++;
            // 输满6位 → 提交提现
            if (currentPosition == 6) {
                submitWithdraw(getPassword());
            }
        }
    }

    private void deletePassword() {
        if (currentPosition > 0) {
            currentPosition--;
            passwordEditTexts[currentPosition].setText("");
        }
    }

    private String getPassword() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : passwordEditTexts) {
            sb.append(et.getText().toString().trim());
        }
        return sb.toString();
    }

    private void dismissAllDialog() {
        if (pwdDialog != null && pwdDialog.isShowing()) pwdDialog.dismiss();
    }

    // ====================== 拉起微信支付/提现 ======================
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
            wxApi.sendReq(req);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("拉起微信失败");
        }
    }

    // ====================== 网络请求 ======================
    private void initOkHttp() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    private void getWalletData() {
        String url = "https://api.rzkj.qyqd123.cn/Android/WalletActivity/GetUserWalletViewModel?userId=" + userId + "&typesId=" + currentTypesId;
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("网络异常"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> parseData(json));
            }
        });
    }

    private void parseData(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("code") != 200) {
                showToast("数据请求失败");
                return;
            }

            JSONObject data = root.getJSONObject("data");
            tvWallet.setText(df.format(data.getDouble("Wallet")));
            tvPoints.setText(df.format(data.getDouble("Points")));
            tvAmount.setText(df.format(data.getDouble("Amount")));

            JSONArray tabArray = data.getJSONArray("UserWalletTabList");
            parseTab(tabArray);

            JSONArray listArray = data.getJSONArray("UserWalletList");
            parseList(listArray);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("数据解析失败");
        }
    }

    private void parseTab(JSONArray array) {
        llTabContainer.removeAllViews();
        List<TabBean> tabList = new ArrayList<>();

        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int key = obj.getInt("Key");
                String value = obj.getString("Value");
                tabList.add(new TabBean(key, value));
            }

            for (int i = 0; i < tabList.size(); i++) {
                TabBean bean = tabList.get(i);
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
                params.setMargins(30, 0, 30, 0);
                tv.setLayoutParams(params);
                tv.setText(bean.getValue());
                tv.setTextSize(20);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(15, 0, 15, 0);

                if (bean.getKey() == currentTypesId) {
                    tv.setTextColor(0xFFdd524d);
                    selectedTab = tv;
                } else {
                    tv.setTextColor(0xFF666666);
                }

                int finalKey = bean.getKey();
                tv.setOnClickListener(v -> {
                    if (selectedTab != null) selectedTab.setTextColor(0xFF666666);
                    tv.setTextColor(0xFFdd524d);
                    selectedTab = tv;
                    currentTypesId = finalKey;
                    getWalletData();
                });

                llTabContainer.addView(tv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseList(JSONArray array) {
        recordList.clear();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                WalletRecord record = new WalletRecord();
                record.setWallet(obj.getString("Wallet"));
                record.setTime(obj.getString("StrCreateDate"));
                recordList.add(record);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("列表数据加载失败");
        }
    }

    // ====================== 适配器 ======================
    class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.Holder> {
        private final List<WalletRecord> list;

        public WalletAdapter(List<WalletRecord> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_record, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            WalletRecord bean = list.get(position);
            String walletText = bean.getWallet();
            holder.tvWallet.setText(walletText);

            if (walletText != null && walletText.startsWith("-")) {
                holder.tvWallet.setTextColor(0xFF0E4492);
            } else {
                holder.tvWallet.setTextColor(0xFFdd524d);
            }

            holder.tvTime.setText(bean.getTime());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvWallet, tvTime;

            public Holder(@NonNull View itemView) {
                super(itemView);
                tvWallet = itemView.findViewById(R.id.tv_wallet);
                tvTime = itemView.findViewById(R.id.tv_time);
            }
        }
    }

    // ====================== 实体类 ======================
    static class TabBean {
        private int Key;
        private String Value;

        public TabBean(int key, String value) {
            Key = key;
            Value = value;
        }

        public int getKey() { return Key; }
        public String getValue() { return Value; }
    }

    static class WalletRecord {
        private String Wallet;
        private String Time;

        public String getWallet() {return Wallet;}
        public void setWallet(String wallet) {Wallet = wallet;}
        public String getTime() {return Time;}
        public void setTime(String time) {Time = time;}
    }

    // ====================== 你指定的自定义Toast ======================
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
        dismissAllDialog();
    }
}
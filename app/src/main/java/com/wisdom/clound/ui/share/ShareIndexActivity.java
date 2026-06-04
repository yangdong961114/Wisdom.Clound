package com.wisdom.clound.ui.share;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.wisdom.clound.Bean.ConfigBean;
import com.wisdom.clound.Bean.ShareListBean;
import com.wisdom.clound.Bean.UserWalletBean;
import com.wisdom.clound.R;
import com.wisdom.clound.adapter.ShareListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShareIndexActivity extends AppCompatActivity {
    private Dialog pwdDialog;
    private Dialog transferDialog;
    // 控件
    private ImageButton btnBack;
    private TabLayout tabLayout;
    // 宫格统计控件
    private TextView tv_shareContext,tv_shareDetailsContext, tv_shareUserName, tv_shareUserCounts, tv_teamUserCounts, tv_teamTotal;
    private EditText etSearch;
    private RecyclerView rvShareList;
    private TextView tvLoadMore;

    // 数据相关
    private String userId;
    private int currentTabIndex = 0;
    private String currentUserName = "";
    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    private boolean isLoading = false;

    // 转账相关
    private double userBalance = 0;
    private double transferFee = 0;
    private ShareListBean.DataBean currentPayee;
    private double currentTransferAmount = 0;

    // 密码输入
    private EditText[] passwordEditTexts = new EditText[6];
    private int currentPosition = 0;

    // 键盘
    private List<Map<String, String>> keyboardList;
    private String[] keyboardValue = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "删除"};

    // 网络请求
    private OkHttpClient okHttpClient;
    private Gson gson;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 适配器
    private ShareListAdapter shareListAdapter;
    private List<ShareListBean.DataBean> shareList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_index);

        initView();
        initData();
        setListener();
        initRecyclerView();

        loadShareStatistic(currentTabIndex);
        loadShareList(false);
    }

    // ==================== 统一封装：黑色透明Toast + 居中 + 无图标 ====================
    private void showToast(String msg) {
        if (TextUtils.isEmpty(msg)) return;
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

    private void initView() {
        btnBack = findViewById(R.id.btn_back);
        tabLayout = findViewById(R.id.tab_layout);
        // 绑定宫格统计控件
        tv_shareContext = findViewById(R.id.tv_shareContext);
        tv_shareDetailsContext = findViewById(R.id.tv_shareDetailsContext);
        tv_shareUserName = findViewById(R.id.tv_shareUserName);
        tv_shareUserCounts = findViewById(R.id.tv_shareUserCounts);
        tv_teamUserCounts = findViewById(R.id.tv_teamUserCounts);
        tv_teamTotal = findViewById(R.id.tv_teamTotal);

        etSearch = findViewById(R.id.et_search);
        rvShareList = findViewById(R.id.rv_share_list);
        tvLoadMore = findViewById(R.id.tv_load_more);
    }

    private void initData() {
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            showToast("用户ID为空");
            finish();
            return;
        }
        okHttpClient = new OkHttpClient();
        gson = new Gson();
        initKeyboardData();
    }

    // 初始化键盘数据
    private void initKeyboardData() {
        keyboardList = new ArrayList<>();
        for (String s : keyboardValue) {
            Map<String, String> map = new HashMap<>();
            map.put("value", s);
            keyboardList.add(map);
        }
    }

    private void setListener() {
        btnBack.setOnClickListener(v -> finish());

        // Tab切换监听：推荐=0，接点=1
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabIndex = tab.getPosition();
                currentPage = 1;
                // 切换标题：推荐→推荐人，接点→接点人
                if (currentTabIndex == 0) {
                    tv_shareContext.setText("推荐人");
                    tv_shareDetailsContext.setText("直推人数");
                } else {
                    tv_shareContext.setText("接点人");
                    tv_shareDetailsContext.setText("接点人数");
                }
                loadShareStatistic(currentTabIndex);
                loadShareList(false);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            currentUserName = etSearch.getText().toString().trim();
            currentPage = 1;
            loadShareList(false);
            return true;
        });

        rvShareList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                if (lastVisibleItemPosition == shareList.size() - 1 && !isLoading && shareList.size() >= PAGE_SIZE) {
                    tvLoadMore.setVisibility(View.VISIBLE);
                    currentPage++;
                    loadShareList(true);
                }
            }
        });
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvShareList.setLayoutManager(layoutManager);
        shareListAdapter = new ShareListAdapter(this, shareList);
        rvShareList.setAdapter(shareListAdapter);

        shareListAdapter.setOnTransferClickListener(bean -> {
            currentPayee = bean;
            showTransferDialog();
        });
    }

    // 转账弹窗
    private void showTransferDialog() {
        transferDialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_transfer, null);
        transferDialog.setContentView(view);
        transferDialog.setCancelable(true);

        if (transferDialog.getWindow() != null) {
            WindowManager.LayoutParams params = transferDialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            transferDialog.getWindow().setAttributes(params);
        }

        TextView tvPayee = view.findViewById(R.id.tv_payee_info);
        TextView tvBalance = view.findViewById(R.id.tv_balance);
        TextView tvFee = view.findViewById(R.id.tv_fee);
        EditText etAmount = view.findViewById(R.id.et_amount);
        TextView tvTransferAll = view.findViewById(R.id.tv_transfer_all);
        android.widget.Button btnSubmit = view.findViewById(R.id.btn_submit);

        String name = currentPayee.getUserName() == null ? "未知" : currentPayee.getUserName();
        String loginName = currentPayee.getUserLoginName() == null ? "无" : currentPayee.getUserLoginName();
        tvPayee.setText("收款人：" + name + "（" + loginName + "）");

        getUserWallet(tvBalance);
        getConfig(tvFee);

        tvTransferAll.setOnClickListener(v -> etAmount.setText(String.valueOf(userBalance)));

        btnSubmit.setOnClickListener(v -> {
            String amount = etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amount) || Double.parseDouble(amount) <= 0) {
                showToast("请输入正确金额");
                return;
            }
            if (Double.parseDouble(amount) > userBalance) {
                showToast("余额不足");
                return;
            }
            currentTransferAmount = Double.parseDouble(amount);
            showSixPasswordDialog();
        });

        transferDialog.show();
    }

    // 6位非全屏密码弹窗
    private void showSixPasswordDialog() {
        currentPosition = 0;
        pwdDialog = new Dialog(this, R.style.Theme_Translucent_NoTitleBar);
        View view = getLayoutInflater().inflate(R.layout.dialog_pay_password, null);
        pwdDialog.setContentView(view);
        pwdDialog.setCancelable(true);

        if (pwdDialog.getWindow() != null) {
            WindowManager.LayoutParams params = pwdDialog.getWindow().getAttributes();
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

    // 输入密码
    private void inputPassword(String num) {
        if (currentPosition < 6) {
            passwordEditTexts[currentPosition].setText(num);
            currentPosition++;
            if (currentPosition == 6) {
                String pwd = getPassword();
                submitTransfer(currentTransferAmount, pwd);
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

    // 获取余额
    private void getUserWallet(TextView tvBalance) {
        String url = "https://api.rzkj.qyqd123.cn/Android/WalletActivity/GetUserWallet?userId=" + userId;
        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    UserWalletBean bean = gson.fromJson(response.body().string(), UserWalletBean.class);
                    if (bean.getCode() == 200) {
                        userBalance = bean.getData().getUserWallets();
                        runOnUiThread(() -> tvBalance.setText("账户余额：" + userBalance));
                    }
                }
            }
        });
    }

    // 获取手续费
    private void getConfig(TextView tvFee) {
        String url = "https://api.rzkj.qyqd123.cn/Android/SystemFragment/GetConfig";
        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ConfigBean bean = gson.fromJson(response.body().string(), ConfigBean.class);
                    if (bean.getCode() == 200) {
                        transferFee = bean.getData().getTranSale();
                        runOnUiThread(() -> {
                            if (transferFee == 0) tvFee.setVisibility(View.GONE);
                            else {
                                tvFee.setVisibility(View.VISIBLE);
                                tvFee.setText("转账将扣除手续费：" + transferFee + "%");
                            }
                        });
                    }
                }
            }
        });
    }

    // 提交转账
    private void submitTransfer(double amount, String payPwd) {
        if (TextUtils.isEmpty(payPwd) || payPwd.length() != 6) {
            showToast("请输入6位支付密码");
            return;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("userId", userId);
            params.put("tUserId", currentPayee.getId());
            params.put("wallet", amount);
            params.put("userPayPwd", payPwd);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestBody requestBody = RequestBody.create(JSON, params.toString());
        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/UserTranWallet/AddUserTransfer")
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (pwdDialog != null) pwdDialog.dismiss();
                    showToast("网络请求失败");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject result = new JSONObject(json);
                        int code = result.getInt("code");
                        String msg = result.getString("msg");

                        if (code == 200) {
                            if (pwdDialog != null) pwdDialog.dismiss();
                            if (transferDialog != null) transferDialog.dismiss();
                            showToast("转账提交成功");
                        } else if (code == 0) {
                            if (pwdDialog != null) pwdDialog.dismiss();
                            showToast(msg);
                        } else {
                            if (pwdDialog != null) pwdDialog.dismiss();
                            showToast("转账异常");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("数据解析失败");
                    }
                });
            }
        });
    }

    // 获取当前密码弹窗对象
    private Dialog getDialog(){
        return pwdDialog;
    }

    // ==================== 【核心改造】新统计接口适配 ====================
    private void loadShareStatistic(int typesId) {
        String url = "https://api.rzkj.qyqd123.cn/Android/ShareActivity/GetUserShare?userId=" + userId + "&typesId=" + typesId;
        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("统计数据加载失败"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 200) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            runOnUiThread(() -> {
                                // 赋值数据
                                tv_shareUserName.setText(data.optString("ShareUserName"));
                                tv_shareUserCounts.setText(data.optString("UserShareCounts"));
                                tv_teamUserCounts.setText(data.optString("TeamShareCounts"));
                                tv_teamTotal.setText(data.optString("TeamTotal"));
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(()-> showToast("数据解析失败"));
                    }
                }
            }
        });
    }

    // 列表接口
    // 列表接口
    private void loadShareList(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;
        // 核心修改：新增 &typesId=currentTabIndex 参数
        String url = "https://api.rzkj.qyqd123.cn/Android/ShareActivity/GetUserShareList?userId=" + userId + "&userName=" + currentUserName + "&page=" + currentPage + "&typesId=" + currentTabIndex;
        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> { tvLoadMore.setVisibility(View.GONE); isLoading = false; });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ShareListBean listBean = gson.fromJson(response.body().string(), ShareListBean.class);
                    runOnUiThread(() -> {
                        tvLoadMore.setVisibility(View.GONE);
                        isLoading = false;
                        if (listBean.getCode() == 200 && listBean.getData() != null) {
                            shareListAdapter.updateData(listBean.getData(), isLoadMore);
                        }
                    });
                }
            }
        });
    }
}
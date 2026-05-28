package com.wisdom.clound.ui.activate;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.wisdom.clound.Bean.ActivateResponse;
import com.wisdom.clound.R;
import com.wisdom.clound.adapter.ActivateAdapter;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ActivateIndexActivity extends AppCompatActivity implements ActivateAdapter.OnActivateListener {
    private ImageButton btnBack;
    private TextView tvTotal, tvUsed, tvUnUsed;
    private RecyclerView recyclerView;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate_index);

        initView();
        getIntentData();
        backClick();
    }

    private void initView() {
        btnBack = findViewById(R.id.btn_back);
        tvTotal = findViewById(R.id.tv_total);
        tvUsed = findViewById(R.id.tv_used);
        tvUnUsed = findViewById(R.id.tv_unused);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getIntentData() {
        userId = getIntent().getStringExtra("userId");
        if (TextUtils.isEmpty(userId)) {
            showToast("请登录后再使用此功能");
            finish();
            return;
        }
        getActivateData();
    }

    private void backClick() {
        btnBack.setOnClickListener(v -> finish());
    }

    // 获取激活码列表数据（GET）
    private void getActivateData() {
        new Thread(() -> {
            try {
                String url = "https://api.rzkj.qyqd123.cn/Android/UserActivate/GetUserActivate?userId=" + userId;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();
                String json = response.body().string();

                Gson gson = new Gson();
                ActivateResponse bean = gson.fromJson(json, ActivateResponse.class);

                runOnUiThread(() -> {
                    if (bean.getCode() == 200) {
                        tvTotal.setText(String.valueOf(bean.getData().getCodeCounts()));
                        tvUsed.setText(String.valueOf(bean.getData().getUnCodeCounts()));
                        tvUnUsed.setText(String.valueOf(bean.getData().getInCodeCounts()));

                        // 设置适配器 + 激活监听
                        ActivateAdapter adapter = new ActivateAdapter(this, bean.getData().getUsers());
                        adapter.setOnActivateListener(this); // 绑定回调
                        recyclerView.setAdapter(adapter);
                    } else {
                        showToast("请求失败");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("网络请求失败"));
            }
        }).start();
    }

    // ===================== 激活按钮回调（核心） =====================
    @Override
    public void onActivate(int userActivateId, String userLoginName) {
        // 调用POST激活接口
        postActivateData(userActivateId, userLoginName);
    }

    // POST请求：激活账号
    private void postActivateData(int userActivateId, String userLoginName) {
        new Thread(() -> {
            try {
                String url = "https://api.rzkj.qyqd123.cn/Android/UserActivate/AddUserActivate";
                OkHttpClient client = new OkHttpClient();

                // 构造POST表单参数
                RequestBody body = new FormBody.Builder()
                        .add("userActivateId", String.valueOf(userActivateId))
                        .add("userLoginName", userLoginName)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String json = response.body().string();

                // 解析返回结果
                Gson gson = new Gson();
                ActivateResult result = gson.fromJson(json, ActivateResult.class);

                runOnUiThread(() -> {
                    if (result.getCode() == 200) {
                        showToast("激活成功");
                        getActivateData(); // 刷新页面数据
                    } else {
                        showToast(result.getMsg());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("激活失败，请重试"));
            }
        }).start();
    }

    // 自定义Toast
    public void showToast(String msg) {
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

    // 激活接口返回实体类
    static class ActivateResult {
        private int code;
        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
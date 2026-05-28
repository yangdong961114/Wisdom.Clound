package com.wisdom.clound.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserPayPwdActivity extends AppCompatActivity {

    private EditText etOldPwd, etNewPwd, etConfirmPwd;
    private LinearLayout btnSavePwd;
    private ImageView ivBack;
    private String userId;
    // 接口返回的原密码（解密后）
    private String originalPwd;
    // 网络请求客户端
    private OkHttpClient okHttpClient;
    private static final String URL_UPDATE_USER_INFO = "https://api.rzkj.qyqd123.cn/Android/MineFragment/UserPayPwdEdit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_pay);

        // 初始化OkHttp
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        // 初始化控件
        initView();
        // 获取用户ID
        getUserId();
        // 从接口获取原密码
        getOriginalPwdFromApi();
        // 设置保存按钮点击事件
        setSaveBtnClickListener();
        toBack();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        etOldPwd = findViewById(R.id.et_old_pwd);
        etNewPwd = findViewById(R.id.et_new_pwd);
        etConfirmPwd = findViewById(R.id.et_confirm_pwd);
        btnSavePwd = findViewById(R.id.btn_save);
        ivBack = findViewById(R.id.iv_back);
    }

    /**
     * 从SP获取用户ID
     */
    private void getUserId() {
        userId = SPUtils.getUserId(getApplicationContext());
        if (userId.isEmpty()) {
            showToast("用户ID获取失败，请重新登录");
            finish();
        }
    }

    /**
     * 从接口获取原密码
     */
    private void getOriginalPwdFromApi() {
        // 2. 构建请求URL
        String url = "https://api.rzkj.qyqd123.cn/Android/MineFragment/GetUserById?userId=" + userId;
        // 3. 构建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        // 4. 发起异步请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 网络请求失败（子线程）
                new Handler(Looper.getMainLooper()).post(() -> {
                    showToast("获取原密码失败：" + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 网络请求成功（子线程）
                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    try {
                        // 解析JSON
                        JSONObject jsonObject = new JSONObject(responseStr);
                        int code = jsonObject.getInt("code");
                        if (code == 200) {
                            JSONObject dataObj = jsonObject.getJSONObject("data");
                            // 获取解密后的原密码
                            originalPwd = dataObj.getString("UserPayPwdDecrypt");
                        } else {
                            String msg = jsonObject.getString("msg");
                            new Handler(Looper.getMainLooper()).post(() -> {
                                showToast(msg);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            showToast("原密码解析失败：" + e.getMessage());
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        showToast("接口返回空数据");
                    });
                }
            }
        });
    }

    /**
     * 设置保存按钮点击事件
     */
    private void setSaveBtnClickListener() {
        btnSavePwd.setOnClickListener(v -> {
            // 1. 获取输入的密码
            String inputOldPwd = etOldPwd.getText().toString().trim();
            String inputNewPwd = etNewPwd.getText().toString().trim();
            String inputConfirmPwd = etConfirmPwd.getText().toString().trim();

            // 2. 输入验证
            if (!validateInput(inputOldPwd, inputNewPwd, inputConfirmPwd)) {
                return;
            }

            // 3. 验证通过，执行密码修改逻辑
            modifyPassword(inputNewPwd);
        });
    }

    /**
     * 输入验证逻辑
     */
    private boolean validateInput(String inputOldPwd, String inputNewPwd, String inputConfirmPwd) {
        // 验证原密码是否为空
        if (inputOldPwd.isEmpty()) {
            showToast("请输入原密码");
            return false;
        }

        // 验证新密码是否为空
        if (inputNewPwd.isEmpty()) {
            showToast("请输入新密码");
            return false;
        }

        // 验证确认密码是否为空
        if (inputConfirmPwd.isEmpty()) {
            showToast("请确认新密码");
            return false;
        }

        // 验证原密码是否正确
        if (originalPwd == null || !originalPwd.equals(inputOldPwd)) {
            showToast("原密码输入错误");
            return false;
        }

        // 验证新密码是否和原密码一致
        if (inputNewPwd.equals(inputOldPwd)) {
            showToast("新密码不能与原密码一致");
            return false;
        }

        // 验证确认密码是否和新密码一致
        if (!inputConfirmPwd.equals(inputNewPwd)) {
            showToast("确认密码与新密码不一致");
            return false;
        }

        return true;
    }

    /**
     * 执行密码修改逻辑
     */
    private void modifyPassword(String newPwd) {
        // 构建请求参数
        JSONObject params = new JSONObject();
        try {
            params.put("userId", userId);
            params.put("newPwd", newPwd);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("参数构建失败");
            return;
        }

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                params.toString()
        );

        Request request = new Request.Builder()
                .url(URL_UPDATE_USER_INFO)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("保存失败：网络异常"));
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            String responseStr = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseStr);
                            if (jsonObject.getInt("code") == 200) {
                                showToast("修改密码成功");
                                jumpToMineFragment();
                            } else {
                                showToast(jsonObject.getString("msg"));
                            }
                        } catch (Exception e) {
                            showToast("解析返回结果失败");
                            e.printStackTrace();
                        }
                    } else {
                        showToast("保存失败：服务器错误");
                    }
                });
            }
        });
    }

    private void toBack() {
        ivBack.setOnClickListener(v -> finish());
    }

    // ==================== 统一封装：黑色透明Toast + 居中 + 无图标 ====================
    private void showToast(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        // 纯文字TextView，无任何系统默认图标
        TextView textView = new TextView(this);
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(0xFFFFFFFF); // 白色文字
        textView.setBackgroundColor(0xCC000000); // 黑色半透明背景
        textView.setPadding(50, 25, 50, 25);
        textView.setGravity(Gravity.CENTER);

        // 屏幕居中显示
        Toast toast = new Toast(this);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void jumpToMineFragment() {
        Intent intent = new Intent(UserPayPwdActivity.this, com.wisdom.clound.MainActivity.class);
        intent.putExtra("target_fragment", "mine");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
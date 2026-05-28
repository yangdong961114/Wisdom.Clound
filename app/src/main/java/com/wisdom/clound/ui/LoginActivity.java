package com.wisdom.clound.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.HttpUtils;
import com.wisdom.clound.utils.SPUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    // 新增：日志标签
    private static final String TAG = "LoginActivity";
    // 新增：SharedPreferences 常量（缓存用户信息）
    private static final String SP_NAME = "user_id";
    private static final String KEY_MEMBER_ID = "member_id";

    private EditText etAccount;
    private EditText etPwd;
    private TextView btnLogin;
    private TextView tvRegister;
    private TextView btnBack;
    private ImageButton btnTogglePwd;
    private boolean isPwdVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();

        // 密码显示/隐藏逻辑（不变）
        btnTogglePwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPwdVisible) {
                    etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnTogglePwd.setImageResource(R.drawable.ic_eye_close);
                } else {
                    etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnTogglePwd.setImageResource(R.drawable.ic_eye_open);
                }
                etPwd.setSelection(etPwd.getText().length());
                isPwdVisible = !isPwdVisible;
            }
        });

        // ********** 核心修改：登录按钮点击事件 **********
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = etAccount.getText().toString().trim();
                String pwd = etPwd.getText().toString().trim();

                // 简单校验
                if (account.isEmpty() || pwd.isEmpty()) {
                    showToast("账号/密码不能为空");
                    return;
                }

                // 调用登录接口
                doLogin(account, pwd);
            }
        });

        // 注册按钮逻辑（不变）
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // 返回按钮逻辑（不变）
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initView() {
        etAccount = findViewById(R.id.et_account);
        etPwd = findViewById(R.id.et_pwd);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        btnTogglePwd = findViewById(R.id.btn_toggle_pwd);
        btnBack = findViewById(R.id.btn_back);
    }

    // ********** 新增：登录接口调用方法 **********
    private void doLogin(String account, String pwd) {
        // 1. 构造POST请求参数
        JSONObject params = new JSONObject();
        try {
            params.put("account", account); // 账号参数
            params.put("password", pwd);    // 密码参数
        } catch (JSONException e) {
            Log.e(TAG, "构造登录参数失败：", e);
            showToast("参数异常");
            return;
        }

        // 2. 调用POST接口
        HttpUtils.post(
                "/RegisterActivity/Login", // 登录接口地址
                params.toString(),         // 请求参数
                getLifecycle(),            // 绑定生命周期，避免内存泄漏
                new HttpUtils.HttpCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "登录接口返回数据：" + result);
                        try {
                            // 解析返回的JSON数据
                            JSONObject response = new JSONObject(result);
                            int code = response.getInt("code");
                            String msg = response.getString("msg");

                            if (code == 200) {
                                // 登录成功：解析会员ID并缓存
                                String memberId = response.getString("data");
                                SPUtils.saveUserId(getApplicationContext(), memberId);

                                showToast(msg);
                                // 跳转到MineFragment所在的主页面（假设主页面是MainActivity）
                                jumpToMineFragment();
                            } else {
                                // 登录失败（如账号密码错误）
                                showToast("登录失败：" + msg);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "解析登录返回数据失败：", e);
                            showToast("数据解析异常");
                        }
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e(TAG, "登录接口请求失败：" + error);
                        showToast("网络异常：" + error);
                    }
                }
        );
    }

    // ********** 统一封装：黑色透明Toast + 居中 + 无图标 **********
    private void showToast(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        // 自定义纯文字TextView，无系统默认图标
        TextView textView = new TextView(this);
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(0xFFFFFFFF); // 白色文字
        textView.setBackgroundColor(0xCC000000); // 黑色半透明背景
        textView.setPadding(50, 25, 50, 25);
        textView.setGravity(Gravity.CENTER);

        // 创建Toast并居中显示
        Toast toast = new Toast(this);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // ********** 新增：跳转到MineFragment **********
    private void jumpToMineFragment() {
        // 假设MineFragment是在MainActivity中（底部导航/侧边栏切换）
        Intent intent = new Intent(LoginActivity.this, com.wisdom.clound.MainActivity.class);
        // 传递标记，让MainActivity切换到MineFragment（可选，根据你的MainActivity逻辑调整）
        intent.putExtra("target_fragment", "mine");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // 清除栈顶，避免重复页面
        startActivity(intent);
        finish(); // 关闭登录页
    }
}
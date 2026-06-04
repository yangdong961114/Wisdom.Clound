package com.wisdom.clound.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText etInviteCode, etAccount, etPwd, etConfirmPwd, etPointsCode;
    private ImageButton btnTogglePwd, btnToggleConfirmPwd;
    private TextView btnRegister, tvBackLogin, tvAgreement;
    private CheckBox cbAgree; // 协议复选框

    private boolean isPwdShow = false;
    private boolean isConfirmPwdShow = false;

    // 协议链接
    private static final String PRIVACY_URL = "https://www.1eryon.xyz/privacy/";
    private static final String USER_URL = "https://www.1eryon.xyz/user/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
        setAgreementText(); // 设置协议文字
        setPwdToggleListener();
        setRegisterBtnListener();
        setBackLoginListener();
    }

    private void initView() {
        etInviteCode = findViewById(R.id.et_invite_code);
        etPointsCode = findViewById(R.id.et_points_code);
        etAccount = findViewById(R.id.et_account);
        etPwd = findViewById(R.id.et_pwd);
        etConfirmPwd = findViewById(R.id.et_confirm_pwd);
        btnTogglePwd = findViewById(R.id.btn_toggle_pwd);
        btnToggleConfirmPwd = findViewById(R.id.btn_toggle_confirm_pwd);
        btnRegister = findViewById(R.id.btn_register);
        tvBackLogin = findViewById(R.id.tv_back_login);
        cbAgree = findViewById(R.id.cb_agree);
        tvAgreement = findViewById(R.id.tv_agreement);

    }

    /**
     * 设置可点击的协议文字
     */
    /**
     * 终极修复：100%正确的字符索引，永不崩溃
     */
    private void setAgreementText() {
        String text = "我已阅读并同意《隐私政策》和《用户协议》";
        SpannableString spannable = new SpannableString(text);

        // 点击《隐私政策》
        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                jumpToWeb(PRIVACY_URL, "隐私政策");
            }
        };
        // 点击《用户协议》
        ClickableSpan userSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                jumpToWeb(USER_URL, "用户协议");
            }
        };

        // 《隐私政策》 → 索引 7 ~ 12
        spannable.setSpan(privacySpan, 7, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 《用户协议》 → 索引 14 ~ 19
        spannable.setSpan(userSpan, 14, 19, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvAgreement.setText(spannable);
        tvAgreement.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    /**
     * 跳转到网页 → 改为弹出本地弹窗
     */
    private void jumpToWeb(String url, String title) {
        // 加载自定义布局
        View dialogView;
        int btnId;
        if (title.equals("隐私政策")) {
            dialogView = getLayoutInflater().inflate(R.layout.dialog_privacy, null);
            btnId = R.id.btn_confirm;
        } else {
            dialogView = getLayoutInflater().inflate(R.layout.dialog_user_agreement, null);
            btnId = R.id.btn_agree;
        }

        // 创建弹窗
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 修复空指针：正确绑定按钮+判空
        View btnClose = dialogView.findViewById(btnId);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // 显示弹窗
        dialog.show();
        // 设置弹窗宽度为屏幕90%，高度85%
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.85)
        );
    }

    // 密码显示/隐藏
    private void setPwdToggleListener() {
        btnTogglePwd.setOnClickListener(v -> {
            if (isPwdShow) {
                etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePwd.setImageResource(R.drawable.ic_eye_close);
            } else {
                etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePwd.setImageResource(R.drawable.ic_eye_open);
            }
            isPwdShow = !isPwdShow;
            etPwd.setSelection(etPwd.getText().length());
        });

        btnToggleConfirmPwd.setOnClickListener(v -> {
            if (isConfirmPwdShow) {
                etConfirmPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggleConfirmPwd.setImageResource(R.drawable.ic_eye_close);
            } else {
                etConfirmPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggleConfirmPwd.setImageResource(R.drawable.ic_eye_open);
            }
            isConfirmPwdShow = !isConfirmPwdShow;
            etConfirmPwd.setSelection(etConfirmPwd.getText().length());
        });
    }

    /**
     * 注册按钮 + 协议校验
     */
    private void setRegisterBtnListener() {
        btnRegister.setOnClickListener(v -> {
            String account = etAccount.getText().toString().trim();
            String pwd = etPwd.getText().toString().trim();
            String confirmPwd = etConfirmPwd.getText().toString().trim();

            // 1. 基础输入校验
            if (!checkInput(account, pwd, confirmPwd)) return;

            // 2. 协议校验（核心）
            if (!cbAgree.isChecked()) {
                showAgreeDialog();
                return;
            }

            // 3. 执行注册
            String inviteCode = etInviteCode.getText().toString().trim();
            String pointsCode = etPointsCode.getText().toString().trim();
            doRegister(inviteCode, pointsCode, account, pwd);
        });
    }

    /**
     * 协议确认弹窗
     */
    private void showAgreeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("温馨提示")
                .setMessage("请您仔细阅读《隐私政策》和《用户协议》，是否同意并勾选？")
                .setPositiveButton("同意", (dialog, which) -> {
                    cbAgree.setChecked(true); // 自动勾选
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 输入校验
    private boolean checkInput(String account, String pwd, String confirmPwd) {
        if (account.isEmpty()) {
            showToast("请输入账号");
            return false;
        }
        if (pwd.isEmpty()) {
            showToast("请输入密码");
            return false;
        }
        if (pwd.length() < 6) {
            showToast("密码长度不能少于6位");
            return false;
        }
        if (confirmPwd.isEmpty()) {
            showToast("请确认密码");
            return false;
        }
        if (!pwd.equals(confirmPwd)) {
            showToast("两次输入的密码不一致");
            return false;
        }
        return true;
    }

    // 注册接口
    private void doRegister(String inviteCode,String pointCode, String account, String pwd) {
        String deviceId = getDeviceUniqueId();
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("inviteCode", inviteCode);
            jsonParams.put("pointCode", pointCode);
            jsonParams.put("account", account);
            jsonParams.put("password", pwd);
            jsonParams.put("deviceId", deviceId);
        } catch (JSONException e) {
            e.printStackTrace();
            showToast("参数构造失败");
            return;
        }

        HttpUtils.post(
                "/RegisterActivity/Register",
                jsonParams.toString(),
                getLifecycle(),
                new HttpUtils.HttpCallback() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            JSONObject response = new JSONObject(result);
                            int code = response.getInt("code");
                            String msg = response.getString("msg");
                            if (code == 200) {
                                showToast("注册成功！");
                                new android.os.Handler().postDelayed(() -> {
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                }, 1000);
                            } else {
                                showToast(msg);
                            }
                        } catch (JSONException e) {
                            showToast("响应解析失败");
                        }
                    }

                    @Override
                    public void onFailed(String error) {
                        showToast("注册失败：" + error);
                    }
                }
        );
    }

    // 获取设备ID
    private String getDeviceUniqueId() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty()) {
            androidId = Build.MODEL + "_" + Build.SERIAL + "_" + System.currentTimeMillis();
        }
        return androidId;
    }

    // 返回登录
    private void setBackLoginListener() {
        tvBackLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    // 统一Toast
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
}
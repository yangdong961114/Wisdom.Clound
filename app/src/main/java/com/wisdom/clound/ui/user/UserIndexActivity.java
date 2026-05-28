package com.wisdom.clound.ui.user;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

public class UserIndexActivity extends AppCompatActivity implements View.OnClickListener {

    // 返回按钮
    private ImageView ivBack;
    // 功能条目
    private LinearLayout llInfoManage;
    private LinearLayout llPwdManage;
    private LinearLayout llPayPwdManage;
    private LinearLayout llAddressManage, llExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_index);

        // 初始化控件
        initView();
        // 设置点击事件
        setClickListener();
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

    /**
     * 初始化控件
     */
    private void initView() {
        ivBack = findViewById(R.id.iv_back);
        llInfoManage = findViewById(R.id.ll_info_manage);
        llPwdManage = findViewById(R.id.ll_pwd_manage);
        llPayPwdManage = findViewById(R.id.ll_pay_pwd_manage);
        llAddressManage = findViewById(R.id.ll_address_manage);
        llExit = findViewById(R.id.ll_exit);
    }

    /**
     * 设置点击事件
     */
    private void setClickListener() {
        ivBack.setOnClickListener(this);
        llInfoManage.setOnClickListener(this);
        llPwdManage.setOnClickListener(this);
        llPayPwdManage.setOnClickListener(this);
        llAddressManage.setOnClickListener(this);
        llExit.setOnClickListener(v -> showExitConfirmDialog());
    }

    // ==================== 修复后的退出弹窗方法（无报错+符合需求） ====================
    private void showExitConfirmDialog() {
        // 弹出退出确认框
        new AlertDialog.Builder(this)
                .setTitle("退出确认")
                .setMessage("是否确定退出当前账号？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 1. 清空userId缓存
                    SPUtils.clearUserId(this);
                    // 2. 提示退出成功
                    showToast("退出成功");
                    // 3. 跳转到登录页面，并关闭当前所有页面
                    Intent intent = new Intent(this, com.wisdom.clound.ui.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    // 4. 关闭对话框
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 取消退出，关闭对话框
                    dialog.dismiss();
                })
                .setCancelable(true) // 点击外部可取消
                .show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_back) {
            // 返回按钮 - 关闭当前页面
            finish();
        } else if (id == R.id.ll_info_manage) {
            // 信息管理
            Intent intent = new Intent(this, UserInfoActivity.class);
            startActivity(intent);
        } else if (id == R.id.ll_pwd_manage) {
            // 密码管理
            Intent intent = new Intent(this, UserPwdActivity.class);
            startActivity(intent);
        } else if (id == R.id.ll_pay_pwd_manage) {
            // 支付密码管理
            Intent intent = new Intent(this, UserPayPwdActivity.class);
            startActivity(intent);
        } else if (id == R.id.ll_address_manage) {
            // 地址管理
            Intent intent = new Intent(this, UserAddressActivity.class);
            startActivity(intent);
        }
    }
}
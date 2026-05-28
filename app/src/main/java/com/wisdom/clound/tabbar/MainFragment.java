package com.wisdom.clound.tabbar;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.wisdom.clound.Bean.UserResponse;
import com.wisdom.clound.R;
import com.wisdom.clound.ui.LoginActivity;
import com.wisdom.clound.ui.activate.ActivateIndexActivity;
import com.wisdom.clound.ui.bouns.BounsIndexActivity;
import com.wisdom.clound.ui.goods.OrderIndexActivity;
import com.wisdom.clound.ui.share.ShareIndexActivity;
import com.wisdom.clound.ui.user.UserIndexActivity;
import com.wisdom.clound.ui.wallet.WalletIndexActivity;
import com.wisdom.clound.utils.OkHttpUtils;
import com.wisdom.clound.utils.SPUtils;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvUserCode;
    private TextView tvUserStatus;
    private TextView tvAmount;
    private TextView tvUserWallet;
    private TextView tvUserPoints;
    private TextView tvUserGrade;
    private LinearLayout tmBouns;
    private LinearLayout tmUserInfo,tmUserShare,tmOrderInfo,tmActivate,tmWalletInfo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private OkHttpClient okHttpClient;
    private Gson gson;
    private Handler mainHandler;
    private static final String TAG = "MainFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        okHttpClient = OkHttpUtils.getInstance();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        initView(rootView);
        initListener();
        checkLoginStatus();
        return rootView;
    }

    /**
     * 初始化控件
     */
    private void initView(View rootView) {
        // 原有控件初始化
        ivAvatar = rootView.findViewById(R.id.iv_avatar);
        tvNickname = rootView.findViewById(R.id.tv_userName);
        tvUserCode = rootView.findViewById(R.id.tv_code);
        tvUserStatus = rootView.findViewById(R.id.tv_status);
        tvAmount = rootView.findViewById(R.id.tv_amount);
        tvUserWallet = rootView.findViewById(R.id.tv_wallet);
        tvUserPoints = rootView.findViewById(R.id.tv_points);
        tvUserGrade = rootView.findViewById(R.id.tv_grade);
        tmBouns = rootView.findViewById(R.id.item_bouns);
        tmUserInfo = rootView.findViewById(R.id.item_userInfo);
        tmUserShare = rootView.findViewById(R.id.item_teamInfo);
        tmOrderInfo = rootView.findViewById(R.id.item_orderInfo);
        tmActivate = rootView.findViewById(R.id.item_activate);
        tmWalletInfo = rootView.findViewById(R.id.item_walletInfo);

        // 下拉刷新控件
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_red_light,
                android.R.color.holo_green_light
        );
    }

    /**
     * 初始化监听
     */
    private void initListener() {
        // 昵称点击登录
        tvNickname.setOnClickListener(v -> {
            String nicknameText = tvNickname.getText().toString().trim();
            if ("立即登录".equals(nicknameText)) {
                if (getActivity() == null) return;
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            }
        });

        // ====================== 新增：邀请码点击复制功能 ======================
        tvUserCode.setOnClickListener(v -> {
            String inviteCode = tvUserCode.getText().toString().trim();
            // 判断是否有有效邀请码
            if (TextUtils.isEmpty(inviteCode) || "000000".equals(inviteCode)) {
                showToast("暂无邀请码可复制");
                return;
            }
            // 复制到系统剪贴板
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("用户邀请码", inviteCode);
            clipboardManager.setPrimaryClip(clipData);
            // 自定义Toast提示成功
            showToast("邀请码复制成功");
        });
        // ====================================================================

        // 奖金
        tmBouns.setOnClickListener( v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后再使用此功能");
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
                return;
            }
            if (getActivity() == null) return;
            Intent userIndexIntent = new Intent(getActivity(), BounsIndexActivity.class);
            userIndexIntent.putExtra("userId", userId);
            startActivity(userIndexIntent);
        });

        // 用户信息
        tmUserInfo.setOnClickListener(v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后再使用此功能");
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
                return;
            }
            if (getActivity() == null) return;
            Intent userIndexIntent = new Intent(getActivity(), UserIndexActivity.class);
            userIndexIntent.putExtra("userId", userId);
            startActivity(userIndexIntent);
        });

        // 分享
        tmUserShare.setOnClickListener(v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后再使用此功能");
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
                return;
            }
            if (getActivity() == null) return;
            Intent userIndexIntent = new Intent(getActivity(), ShareIndexActivity.class);
            userIndexIntent.putExtra("userId", userId);
            startActivity(userIndexIntent);
        });

        // 订单
        tmOrderInfo.setOnClickListener(v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后再使用此功能");
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
                return;
            }
            if (getActivity() == null) return;
            Intent userIndexIntent = new Intent(getActivity(), OrderIndexActivity.class);
            userIndexIntent.putExtra("userId", userId);
            startActivity(userIndexIntent);
        });

        // 激活
        tmActivate.setOnClickListener(v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后再使用此功能");
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
                return;
            }
            if (getActivity() == null) return;
            Intent userIndexIntent = new Intent(getActivity(), ActivateIndexActivity.class);
            userIndexIntent.putExtra("userId", userId);
            startActivity(userIndexIntent);
        });

        // 钱包
        tmWalletInfo.setOnClickListener(v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后再使用此功能");
                Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(loginIntent);
                return;
            }
            if (getActivity() == null) return;
            Intent userIndexIntent = new Intent(getActivity(), WalletIndexActivity.class);
            userIndexIntent.putExtra("userId", userId);
            startActivity(userIndexIntent);
        });

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::checkLoginStatus);
    }

    /**
     * 检查登录状态
     */
    private void checkLoginStatus() {
        if (!isAdded() || getContext() == null) return;

        String userId = SPUtils.getUserId(getContext());
        Log.d(TAG, "当前用户ID：" + userId);

        if (TextUtils.isEmpty(userId)) {
            showLoginEntry();
        } else {
            requestUserInfo(userId);
        }
    }

    /**
     * 未登录状态
     */
    private void showLoginEntry() {
        tvNickname.setText("立即登录");
        ivAvatar.setImageResource(R.drawable.ic_avatar);
        tvUserCode.setText("000000");
        tvUserStatus.setText("未激活");
        tvAmount.setText("0.00");
        tvUserWallet.setText("￥0元");
        tvUserPoints.setText("0.00");
        tvUserGrade.setText("无团队级别");
        stopRefreshAnimation();
    }

    /**
     * 请求用户信息
     */
    private void requestUserInfo(String userId) {
        if (okHttpClient == null) {
            Log.e(TAG, "OkHttpClient 初始化失败");
            showLoginEntry();
            return;
        }

        String apiUrl = "https://api.rzkj.qyqd123.cn/Android/MineFragment/GetUser?userId=" + userId;
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "用户信息请求失败：" + e.getMessage());
                mainHandler.post(() -> {
                    showLoginEntry();
                    stopRefreshAnimation();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> {
                        showLoginEntry();
                        stopRefreshAnimation();
                    });
                    return;
                }

                String jsonStr = response.body().string();
                if (TextUtils.isEmpty(jsonStr)) {
                    mainHandler.post(() -> {
                        showLoginEntry();
                        stopRefreshAnimation();
                    });
                    return;
                }

                try {
                    UserResponse userResponse = gson.fromJson(jsonStr, UserResponse.class);
                    if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                        String userName = userResponse.getData().getUserName();
                        String userAvatar = userResponse.getData().getUserAvatar();
                        String userCode = userResponse.getData().getUserCode();
                        String userGrade = userResponse.getData().getUserGrade();
                        String userStatus = userResponse.getData().getUserStatus();
                        String userWallet = userResponse.getData().getUserWallet();
                        String userPoints = userResponse.getData().getUserPoints();
                        String amount = userResponse.getData().getUserAmount();
                        String userFee = userResponse.getData().getUserFee();
                        mainHandler.post(() -> {
                            updateUserUI(userName, userAvatar, userCode, userGrade, userStatus, userWallet, userPoints, userFee, amount);
                            stopRefreshAnimation();
                        });
                    } else {
                        mainHandler.post(() -> {
                            showLoginEntry();
                            stopRefreshAnimation();
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        showLoginEntry();
                        stopRefreshAnimation();
                    });
                }
            }
        });
    }

    /**
     * 更新UI
     */
    private void updateUserUI(String userName, String userAvatar, String userCode, String userGrade, String userStatus, String userWallet, String userPoints, String userFee,String userAmount) {
        if (!isAdded() || getContext() == null) return;

        tvNickname.setText(TextUtils.isEmpty(userName) ? "立即登录" : userName);
        tvUserCode.setText(TextUtils.isEmpty(userCode) ? "000000" : userCode);
        tvUserGrade.setText(TextUtils.isEmpty(userGrade) ? "普通会员" : userGrade);
        tvUserStatus.setText(TextUtils.isEmpty(userStatus) ? "未激活" : userStatus);
        tvUserWallet.setText(TextUtils.isEmpty(userWallet) ? "￥0.00元" : userWallet);
        tvUserPoints.setText(TextUtils.isEmpty(userPoints) ? "0.00" : userPoints);
        tvAmount.setText(TextUtils.isEmpty(userFee) ? "0" : userAmount);

        Glide.with(this)
                .load(userAvatar)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .into(ivAvatar);

        stopRefreshAnimation();
    }

    /**
     * 停止刷新
     */
    private void stopRefreshAnimation() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * 自定义Toast
     */
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
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        stopRefreshAnimation();
    }
}
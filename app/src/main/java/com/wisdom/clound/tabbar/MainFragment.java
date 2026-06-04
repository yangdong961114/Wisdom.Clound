package com.wisdom.clound.tabbar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
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

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainFragment extends Fragment {

    // 【日志TAG】排查权限问题专用
    private static final String TAG = "PermissionCheck";
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
    private LinearLayout btn_start_task;
    private SwipeRefreshLayout swipeRefreshLayout;
    private OkHttpClient okHttpClient;
    private Gson gson;
    private Handler mainHandler;

    // 权限配置
    private static final int PERMISSION_REQ_CODE = 100;
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private AlertDialog loadingDialog;
    private double longitude = 0.0, latitude = 0.0;

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

    private void initView(View rootView) {
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
        btn_start_task = rootView.findViewById(R.id.item_start_task);

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_green_light);
    }

    private void initListener() {
        // 登录
        tvNickname.setOnClickListener(v -> {
            String nicknameText = tvNickname.getText().toString().trim();
            if ("立即登录".equals(nicknameText)) {
                if (getActivity() == null) return;
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            }
        });

        // 邀请码复制
        tvUserCode.setOnClickListener(v -> {
            String inviteCode = tvUserCode.getText().toString().trim();
            if (TextUtils.isEmpty(inviteCode) || "000000".equals(inviteCode)) {
                showToast("暂无邀请码可复制");
                return;
            }
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("用户邀请码", inviteCode);
            clipboardManager.setPrimaryClip(clipData);
            showToast("邀请码复制成功");
        });

        // 核心：开始任务
        btn_start_task.setOnClickListener(v -> {
            String userId = SPUtils.getUserId(getContext());
            if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
                showToast("请登录后执行任务");
                return;
            }

            // 【打印日志】检查权限
            Log.d(TAG, "===== 开始检查权限 =====");
            if (hasAllPermissions()) {
                // 新增：检查定位服务是否开启（模拟器90%是这个问题！）
                if (isLocationServiceEnabled()) {
                    Log.d(TAG, "权限+定位服务均正常");
                    getLocationAndStart();
                } else {
                    Log.d(TAG, "权限已开，但定位服务未开启");
                    showLocationServiceDialog();
                }
            } else {
                Log.d(TAG, "权限未全部授予");
                requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_REQ_CODE);
            }
        });

        // 原有点击事件（省略保留，和你代码一致）
        tmBouns.setOnClickListener( v -> { startOtherActivity(BounsIndexActivity.class); });
        tmUserInfo.setOnClickListener(v -> { startOtherActivity(UserIndexActivity.class); });
        tmUserShare.setOnClickListener(v -> { startOtherActivity(ShareIndexActivity.class); });
        tmOrderInfo.setOnClickListener(v -> { startOtherActivity(OrderIndexActivity.class); });
        tmActivate.setOnClickListener(v -> { startOtherActivity(ActivateIndexActivity.class); });
        tmWalletInfo.setOnClickListener(v -> { startOtherActivity(WalletIndexActivity.class); });

        swipeRefreshLayout.setOnRefreshListener(this::checkLoginStatus);
    }

    // 抽取跳转方法，简化代码
    private void startOtherActivity(Class<?> cls) {
        String userId = SPUtils.getUserId(getContext());
        if (TextUtils.isEmpty(userId) || "立即登录".equals(tvNickname.getText().toString().trim())) {
            showToast("请登录后再使用此功能");
            return;
        }
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), cls);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    // ====================== 【核心】带日志的权限检查 ======================
    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "安卓版本低于6.0，无需动态权限");
            return true;
        }

        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            boolean isGranted = ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED;
            // 【关键日志】打印每个权限的状态
            Log.d(TAG, "权限：" + permission + " → 已授予：" + isGranted);
            if (!isGranted) allGranted = false;
        }
        return allGranted;
    }

    // ====================== 【核心】检查定位服务是否开启（模拟器必查） ======================
    private boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        // 检查GPS或网络定位是否开启
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "GPS服务：" + isGPSEnabled + " | 网络定位：" + isNetworkEnabled);
        return isGPSEnabled || isNetworkEnabled;
    }

    // ====================== 权限回调（带日志） ======================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            Log.d(TAG, "===== 权限申请结果 =====");
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                Log.d(TAG, "权限：" + permissions[i] + " → 结果：" + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }

            if (allGranted) {
                if (isLocationServiceEnabled()) {
                    getLocationAndStart();
                } else {
                    showLocationServiceDialog();
                }
            } else {
                showPermissionSettingDialog();
            }
        }
    }

    // ====================== 弹窗：开启定位服务 ======================
    private void showLocationServiceDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("定位服务未开启")
                .setMessage("权限已开启，但需要打开系统定位服务才能执行任务")
                .setPositiveButton("去开启", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 权限设置弹窗
    private void showPermissionSettingDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("权限提示")
                .setMessage("任务需要获取手机信息、定位权限，请前往设置页面开启")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> showToast("已取消，无法执行任务"))
                .setCancelable(false)
                .show();
    }

    // ====================== 以下代码和你完全一致，仅保留功能 ======================
    private void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new AlertDialog.Builder(getContext()).setMessage("请稍候...").setCancelable(false).create();
        }
        if (!loadingDialog.isShowing() && isAdded()) loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing() && isAdded()) loadingDialog.dismiss();
    }

    private void getLocationAndStart() {
        try {
            LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) { longitude = loc.getLongitude(); latitude = loc.getLatitude(); }
        } catch (Exception e) { longitude = 0.0; latitude = 0.0; }
        startTaskRequest();
    }

    private void startTaskRequest() {
        showLoading();
        try {
            String userId = SPUtils.getUserId(getContext());
            String imei = getIMEI();
            String phoneModel = Build.MANUFACTURER + " " + Build.MODEL;
            String ip = getIP();
            String lng = String.valueOf(longitude);
            String lat = String.valueOf(latitude);

            JSONObject params = new JSONObject();
            params.put("UserId", userId);
            params.put("Imei", imei);
            params.put("PhoneVersion", phoneModel);
            params.put("UserIp", ip);
            params.put("Lng", lng);
            params.put("Lat", lat);

            RequestBody body = RequestBody.create(JSON, params.toString());
            Request request = new Request.Builder().url("https://api.rzkj.qyqd123.cn/Android/AdvActivity/AddAdvDetails").post(body).build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> { hideLoading(); showToast("网络请求失败"); });
                }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    mainHandler.post(() -> hideLoading());
                    String json = response.body().string();
                    mainHandler.post(() -> {
                        try {
                            JSONObject obj = new JSONObject(json);
                            int code = obj.getInt("code");
                            String msg = obj.getString("msg");
                            if (code == 200) { showToast(msg); onTaskSuccess(); } else { showToast(msg); }
                        } catch (Exception e) { showToast("接口解析失败"); }
                    });
                }
            });
        } catch (Exception e) { hideLoading(); showToast("参数获取失败"); }
    }

    private void onTaskSuccess() {}

    private String getIMEI() {
        try {
            TelephonyManager tm = (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return tm.getImei();
            else return tm.getDeviceId();
        } catch (Exception e) { return "unknown_imei"; }
    }

    private String getIP() {
        try {
            WifiManager wm = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            return InetAddress.getByAddress(new byte[]{(byte) (ip & 0xFF), (byte) (ip >> 8 & 0xFF), (byte) (ip >> 16 & 0xFF), (byte) (ip >> 24 & 0xFF)}).getHostAddress();
        } catch (Exception e) { return "0.0.0.0"; }
    }

    private void checkLoginStatus() {
        if (!isAdded() || getContext() == null) return;
        String userId = SPUtils.getUserId(getContext());
        if (TextUtils.isEmpty(userId)) showLoginEntry(); else requestUserInfo(userId);
    }

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

    private void requestUserInfo(String userId) {
        if (okHttpClient == null) { showLoginEntry(); return; }
        String apiUrl = "https://api.rzkj.qyqd123.cn/Android/MineFragment/GetUser?userId=" + userId;
        Request request = new Request.Builder().url(apiUrl).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> { showLoginEntry(); stopRefreshAnimation(); });
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) { mainHandler.post(()->showLoginEntry()); return; }
                String jsonStr = response.body().string();
                try {
                    UserResponse userResponse = gson.fromJson(jsonStr, UserResponse.class);
                    if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                        mainHandler.post(()->updateUserUI(
                                userResponse.getData().getUserName(), userResponse.getData().getUserAvatar(),
                                userResponse.getData().getUserCode(), userResponse.getData().getUserGrade(),
                                userResponse.getData().getUserStatus(), userResponse.getData().getUserWallet(),
                                userResponse.getData().getUserPoints(), userResponse.getData().getUserFee(),
                                userResponse.getData().getUserAmount()
                        ));
                    } else { mainHandler.post(()->showLoginEntry()); }
                } catch (Exception e) { mainHandler.post(()->showLoginEntry()); }
                finally { mainHandler.post(() -> stopRefreshAnimation()); }
            }
        });
    }

    private void updateUserUI(String userName, String userAvatar, String userCode, String userGrade, String userStatus, String userWallet, String userPoints, String userFee,String userAmount) {
        if (!isAdded() || getContext() == null) return;
        tvNickname.setText(TextUtils.isEmpty(userName) ? "立即登录" : userName);
        tvUserCode.setText(TextUtils.isEmpty(userCode) ? "000000" : userCode);
        tvUserGrade.setText(TextUtils.isEmpty(userGrade) ? "普通会员" : userGrade);
        tvUserStatus.setText(TextUtils.isEmpty(userStatus) ? "未激活" : userStatus);
        tvUserWallet.setText(TextUtils.isEmpty(userWallet) ? "￥0.00元" : userWallet);
        tvUserPoints.setText(TextUtils.isEmpty(userPoints) ? "0" : userPoints);
        tvAmount.setText(TextUtils.isEmpty(userFee) ? "0" : userAmount);
        Glide.with(this).load(userAvatar).transform(new CircleCrop()).placeholder(R.drawable.ic_avatar).error(R.drawable.ic_avatar).into(ivAvatar);
        stopRefreshAnimation();
    }

    private void stopRefreshAnimation() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
    }

    private void showToast(String msg) {
        if (!isAdded() || getContext() == null || TextUtils.isEmpty(msg)) return;
        TextView textView = new TextView(getContext());
        textView.setText(msg); textView.setTextSize(14); textView.setTextColor(0xFFFFFFFF);
        textView.setBackgroundColor(0xCC000000); textView.setPadding(50, 25, 50, 25); textView.setGravity(Gravity.CENTER);
        Toast toast = new Toast(getContext()); toast.setView(textView); toast.setDuration(Toast.LENGTH_SHORT); toast.setGravity(Gravity.CENTER, 0, 0); toast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        hideLoading();
        stopRefreshAnimation();
    }
}
package com.wisdom.clound.ui.user;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.wisdom.clound.R;
import com.wisdom.clound.adapter.UserAddressAdapter;
import com.wisdom.clound.Bean.AddressResponse;
import com.wisdom.clound.Bean.UserAddress;
import com.wisdom.clound.utils.SPUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 收货地址管理页面（完善省市区三级联动）
 */
public class UserAddressActivity extends AppCompatActivity implements UserAddressAdapter.OnAddressOperateListener {


    // 三级联动数据源映射
    private Map<String, Integer> provinceArrayMap;    // 省份名称 → 城市数组资源ID
    private Map<String, Integer> cityArrayMap;       // 城市名称 → 区县数组资源ID
    private Map<String, String[]> tempProvinceCityMap; // 缓存：省份→城市列表
    private Map<String, String[]> tempCityDistrictMap; // 缓存：城市→区县列表

    // 弹窗控件
    private EditText etName, etPhone, etAddress;
    private Spinner spProvince, spCity, spDistrict;
    private AlertDialog addAddressDialog;
    private RecyclerView rvAddressList;
    private TextView tvEmptyTip;
    private UserAddressAdapter mAdapter;
    private OkHttpClient mOkHttpClient;
    private Gson mGson;

    // 编辑模式标记
    private boolean isEditMode = false;
    private UserAddress editAddress; // 待编辑的地址
    private boolean isEchoing = false;
    private Switch switchDefault;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_address_details);

        // 初始化省市区映射关系（核心：自动关联strings.xml数组）
        initProvinceCityDistrictMapping();

        // 初始化控件
        initView();

        // 初始化OkHttp
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        mGson = new Gson();
        // 获取地址列表
        loadAddressList();
        // 设置按钮点击事件
        setClickListener();
    }

    /**
     * 初始化省市区映射关系（自动关联strings.xml中的数组）
     * 规则：
     * - 省份数组：province_list
     * - 城市数组：city_省份拼音（如city_beijing、city_guangdong）
     * - 区县数组：district_城市拼音（如district_beijing、district_guangzhou）
     */
    private void initProvinceCityDistrictMapping() {
        // 1. 初始化映射表
        provinceArrayMap = new HashMap<>();
        cityArrayMap = new HashMap<>();
        tempProvinceCityMap = new HashMap<>();
        tempCityDistrictMap = new HashMap<>();

        // 2. 省份→城市数组ID 映射（根据strings.xml命名规则）
        provinceArrayMap.put("北京市", R.array.city_beijing);
        provinceArrayMap.put("上海市", R.array.city_shanghai);
        provinceArrayMap.put("天津市", R.array.city_tianjin);
        provinceArrayMap.put("重庆市", R.array.city_chongqing);
        provinceArrayMap.put("河北省", R.array.city_hebei);
        provinceArrayMap.put("山西省", R.array.city_shanxi);
        provinceArrayMap.put("辽宁省", R.array.city_liaoning);
        provinceArrayMap.put("吉林省", R.array.city_jilin);
        provinceArrayMap.put("黑龙江省", R.array.city_heilongjiang);
        provinceArrayMap.put("江苏省", R.array.city_jiangsu);
        provinceArrayMap.put("浙江省", R.array.city_zhejiang);
        provinceArrayMap.put("安徽省", R.array.city_anhui);
        provinceArrayMap.put("福建省", R.array.city_fujian);
        provinceArrayMap.put("江西省", R.array.city_jiangxi);
        provinceArrayMap.put("山东省", R.array.city_shandong);
        provinceArrayMap.put("河南省", R.array.city_henan);
        provinceArrayMap.put("湖北省", R.array.city_hubei);
        provinceArrayMap.put("湖南省", R.array.city_hunan);
        provinceArrayMap.put("广东省", R.array.city_guangdong);
        provinceArrayMap.put("海南省", R.array.city_hainan);
        provinceArrayMap.put("四川省", R.array.city_sichuan);
        provinceArrayMap.put("贵州省", R.array.city_guizhou);
        provinceArrayMap.put("云南省", R.array.city_yunnan);
        provinceArrayMap.put("陕西省", R.array.city_shaanxi);
        provinceArrayMap.put("甘肃省", R.array.city_gansu);
        provinceArrayMap.put("青海省", R.array.city_qinghai);
        provinceArrayMap.put("台湾省", R.array.city_taiwan);
        provinceArrayMap.put("内蒙古自治区", R.array.city_neimenggu);
        provinceArrayMap.put("广西壮族自治区", R.array.city_guangxi);
        provinceArrayMap.put("西藏自治区", R.array.city_xizang);
        provinceArrayMap.put("宁夏回族自治区", R.array.city_ningxia);
        provinceArrayMap.put("新疆维吾尔自治区", R.array.city_xinjiang);
        provinceArrayMap.put("香港特别行政区", R.array.city_hongkong);
        provinceArrayMap.put("澳门特别行政区", R.array.city_macao);

        // 3. 城市→区县数组ID 映射
        // 直辖市
        cityArrayMap.put("北京市", R.array.district_beijing);
        cityArrayMap.put("上海市", R.array.district_shanghai);
        cityArrayMap.put("天津市", R.array.district_tianjin);
        cityArrayMap.put("重庆市", R.array.district_chongqing);
        // 河北省
        cityArrayMap.put("石家庄市", R.array.district_shijiazhuang);
        cityArrayMap.put("唐山市", R.array.district_tangshan);
        // 山西省
        cityArrayMap.put("太原市", R.array.district_taiyuan);
        // 辽宁省
        cityArrayMap.put("沈阳市", R.array.district_shenyang);
        cityArrayMap.put("大连市", R.array.district_dalian);
        // 吉林省
        cityArrayMap.put("长春市", R.array.district_changchun);
        // 黑龙江省
        cityArrayMap.put("哈尔滨市", R.array.district_harbin);
        // 江苏省
        cityArrayMap.put("南京市", R.array.district_nanjing);
        cityArrayMap.put("苏州市", R.array.district_suzhou);
        cityArrayMap.put("无锡市", R.array.district_wuxi);
        // 浙江省
        cityArrayMap.put("杭州市", R.array.district_hangzhou);
        cityArrayMap.put("宁波市", R.array.district_ningbo);
        cityArrayMap.put("温州市", R.array.district_wenzhou);
        // 安徽省
        cityArrayMap.put("合肥市", R.array.district_hefei);
        // 福建省
        cityArrayMap.put("福州市", R.array.district_fuzhou);
        cityArrayMap.put("厦门市", R.array.district_xiamen);
        // 江西省
        cityArrayMap.put("南昌市", R.array.district_nanchang);
        // 山东省
        cityArrayMap.put("济南市", R.array.district_jinan);
        cityArrayMap.put("青岛市", R.array.district_qingdao);
        // 河南省
        cityArrayMap.put("郑州市", R.array.district_zhengzhou);
        // 湖北省
        cityArrayMap.put("武汉市", R.array.district_wuhan);
        // 湖南省
        cityArrayMap.put("长沙市", R.array.district_changsha);
        // 广东省
        cityArrayMap.put("广州市", R.array.district_guangzhou);
        cityArrayMap.put("深圳市", R.array.district_shenzhen);
        cityArrayMap.put("佛山市", R.array.district_foshan);
        // 海南省
        cityArrayMap.put("海口市", R.array.district_haikou);
        // 四川省
        cityArrayMap.put("成都市", R.array.district_chengdu);
        // 贵州省
        cityArrayMap.put("贵阳市", R.array.district_guiyang);
        // 云南省
        cityArrayMap.put("昆明市", R.array.district_kunming);
        // 陕西省
        cityArrayMap.put("西安市", R.array.district_xian);
        // 甘肃省
        cityArrayMap.put("兰州市", R.array.district_lanzhou);
        // 青海省
        cityArrayMap.put("西宁市", R.array.district_xining);
        // 台湾省
        cityArrayMap.put("台北市", R.array.district_taibei);
        // 内蒙古
        cityArrayMap.put("呼和浩特市", R.array.district_huhehaote);
        // 广西
        cityArrayMap.put("南宁市", R.array.district_nanning);
        // 西藏
        cityArrayMap.put("拉萨市", R.array.district_lasa);
        // 宁夏
        cityArrayMap.put("银川市", R.array.district_yinchuan);
        // 新疆
        cityArrayMap.put("乌鲁木齐市", R.array.district_wulumuqi);
        // 香港/澳门
        cityArrayMap.put("香港特别行政区", R.array.district_hongkong);
        cityArrayMap.put("澳门特别行政区", R.array.district_macao);
    }

    /**
     * 初始化控件
     */
    private void initView() {
        rvAddressList = findViewById(R.id.rv_address_list);
        tvEmptyTip = findViewById(R.id.tv_empty_tip);
        // 初始化RecyclerView
        rvAddressList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new UserAddressAdapter(this, null, this);
        rvAddressList.setAdapter(mAdapter);
    }

    /**
     * 设置按钮点击事件
     */
    private void setClickListener() {
        // 返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        // 添加新地址按钮
        findViewById(R.id.btn_add_address).setOnClickListener(v -> {
            isEditMode = false;
            editAddress = null;
            showAddAddressDialog();
        });
    }

    /**
     * 设置Spinner选中指定值（修复：去除空格匹配，兼容所有情况）
     */
    private void setSpinnerSelection(Spinner spinner, String targetValue) {
        if (targetValue == null || spinner.getAdapter() == null) return;
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            // 🔥 关键：去除首尾空格再匹配，防止隐形空格导致选不中
            String item = adapter.getItem(i).toString().trim();
            if (item.equals(targetValue.trim())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void showAddAddressDialog() {
        // 加载弹窗布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null);

        // 绑定弹窗控件
        etName = dialogView.findViewById(R.id.et_name);
        etPhone = dialogView.findViewById(R.id.et_phone);
        spProvince = dialogView.findViewById(R.id.sp_province);
        spCity = dialogView.findViewById(R.id.sp_city);
        spDistrict = dialogView.findViewById(R.id.sp_district);
        etAddress = dialogView.findViewById(R.id.et_address);
        switchDefault = dialogView.findViewById(R.id.switch_default); // 绑定开关
        Button btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        Button btnSave = dialogView.findViewById(R.id.btn_save_address);

        // 初始化省市区三级联动
        initProvinceCityDistrictSpinner();

        // 编辑模式：回显数据 + 同步默认地址状态
        if (isEditMode && editAddress != null) {
            etName.setText(editAddress.getUserName());
            etPhone.setText(editAddress.getUserPhone());
            etAddress.setText(editAddress.getAddress());
            // 🔥 核心：编辑时同步默认地址状态
            switchDefault.setChecked(editAddress.getIsDefault());

            // 开启回显模式：禁用所有自动监听
            isEchoing = true;

            spProvince.post(() -> {
                // 1. 选中省份
                setSpinnerSelection(spProvince, editAddress.getProvince());
                // 2. 手动加载对应城市
                updateCitySpinner(editAddress.getProvince());

                spCity.post(() -> {
                    // 3. 选中城市
                    setSpinnerSelection(spCity, editAddress.getCity());
                    // 4. 手动加载对应区县
                    updateDistrictSpinner(editAddress.getCity());

                    // 选中区县
                    spDistrict.postDelayed(() -> {
                        setSpinnerSelection(spDistrict, editAddress.getDistrict());
                        isEchoing = false;
                    }, 100);
                });
            });
        }

        // 创建弹窗
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(true);
        addAddressDialog = builder.create();
        addAddressDialog.show();

        // 设置弹窗宽度
        Window window = addAddressDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 关闭按钮点击
        btnClose.setOnClickListener(v -> addAddressDialog.dismiss());

        // 保存按钮点击
        btnSave.setOnClickListener(v -> {
            if (isEditMode) {
                updateAddress();
            } else {
                saveAddress();
            }
        });
    }

    /**
     * 初始化省市区三级联动Spinner
     */
    private void initProvinceCityDistrictSpinner() {
        // 1. 初始化省份Spinner
        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.province_list,
                android.R.layout.simple_spinner_item
        );
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProvince.setAdapter(provinceAdapter);

        // 2. 省份选择监听 → 更新城市列表（🔥 回显时不执行）
        spProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 🔥 核心：编辑回显时，禁用自动刷新，防止覆盖手动选中的值
                if (isEchoing) return;

                String selectedProvince = parent.getItemAtPosition(position).toString();
                updateCitySpinner(selectedProvince);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 3. 城市选择监听 → 更新区县列表（🔥 回显时不执行）
        spCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 🔥 核心：编辑回显时，禁用自动刷新
                if (isEchoing) return;

                String selectedCity = parent.getItemAtPosition(position).toString();
                updateDistrictSpinner(selectedCity);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4. 初始化默认值
        updateCitySpinner("请选择省份");
        updateDistrictSpinner("请选择城市");
    }

    /**
     * 根据选中的省份更新城市Spinner
     */
    private void updateCitySpinner(String province) {
        // 优先从缓存获取
        String[] cityList = tempProvinceCityMap.get(province);
        if (cityList == null) {
            // 未缓存：从资源文件加载
            Integer cityArrayId = provinceArrayMap.get(province);
            if (cityArrayId != null) {
                cityList = getResources().getStringArray(cityArrayId);
            } else {
                // 无匹配的城市列表
                cityList = new String[]{"请选择城市"};
            }
            tempProvinceCityMap.put(province, cityList);
        }

        // 设置城市适配器
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cityList
        );
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCity.setAdapter(cityAdapter);
    }

    /**
     * 根据选中的城市更新区县Spinner
     */
    private void updateDistrictSpinner(String city) {
        // 优先从缓存获取
        String[] districtList = tempCityDistrictMap.get(city);
        if (districtList == null) {
            // 未缓存：从资源文件加载
            Integer districtArrayId = cityArrayMap.get(city);
            if (districtArrayId != null) {
                districtList = getResources().getStringArray(districtArrayId);
            } else {
                // 无匹配的区县列表
                districtList = new String[]{"请选择区域"};
            }
            tempCityDistrictMap.put(city, districtList);
        }

        // 设置区县适配器
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                districtList
        );
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDistrict.setAdapter(districtAdapter);
    }

    /**
     * 新增地址
     */
    private void saveAddress() {
        // 1. 获取输入内容
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String province = spProvince.getSelectedItem().toString();
        String city = spCity.getSelectedItem().toString();
        String district = spDistrict.getSelectedItem().toString();
        String detailAddress = etAddress.getText().toString().trim();
        String userId = SPUtils.getUserId(getApplicationContext());
        int isDefault = switchDefault.isChecked() ? 1 : 0;

        // 2. 输入校验
        if (validateInput(name, phone, province, city, district, detailAddress, userId)) {
            return;
        }

        // 3. 构建POST请求参数
        FormBody formBody = new FormBody.Builder()
                .add("UserId", userId)
                .add("UserName", name)
                .add("UserPhone", phone)
                .add("Province", province)
                .add("City", city)
                .add("District", district)
                .add("Address", detailAddress)
                .add("IsDefault", String.valueOf(isDefault)) // 🔥 传递默认地址
                .build();

        // 4. 发送新增请求
        sendAddressRequest("https://api.rzkj.qyqd123.cn/Android/UserMap/AddMap", formBody, "添加");
    }

    /**
     * 编辑地址
     */
    private void updateAddress() {
        // 1. 获取输入内容
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String province = spProvince.getSelectedItem().toString();
        String city = spCity.getSelectedItem().toString();
        String district = spDistrict.getSelectedItem().toString();
        String detailAddress = etAddress.getText().toString().trim();
        String userId = SPUtils.getUserId(getApplicationContext());
        int isDefault = switchDefault.isChecked() ? 1 : 0;

        // 2. 输入校验
        if (validateInput(name, phone, province, city, district, detailAddress, userId)) {
            return;
        }

        // 3. 构建POST请求参数（新增mapId）
        FormBody formBody = new FormBody.Builder()
                .add("Id", String.valueOf(editAddress.getId()))
                .add("UserId", userId)
                .add("UserName", name)
                .add("UserPhone", phone)
                .add("Province", province)
                .add("City", city)
                .add("District", district)
                .add("Address", detailAddress)
                .add("IsDefault", String.valueOf(isDefault))
                .build();
        Log.d("UserAddressActivity", "updateAddress: " + name+phone+province+city+district+detailAddress+userId);
        // 4. 发送编辑请求（替换为实际编辑API）
        sendAddressRequest("https://api.rzkj.qyqd123.cn/Android/UserMap/EditMap", formBody, "修改");
    }

    /**
     * 输入校验通用方法
     * @return true-校验失败 false-校验通过
     */
    private boolean validateInput(String name, String phone, String province, String city,
                                  String district, String detailAddress, String userId) {
        if (name.isEmpty()) {
            showToast("请输入收货人姓名");
            return true;
        }
        if (phone.isEmpty() || !phone.matches("^1[3-9]\\d{9}$")) {
            showToast("请输入正确的手机号");
            return true;
        }
        if ("请选择省份".equals(province) || "请选择城市".equals(city) || "请选择区域".equals(district)) {
            showToast("请选择完整的省/市/区");
            return true;
        }
        if (detailAddress.isEmpty()) {
            showToast("请输入详细收货地址");
            return true;
        }
        if (userId.isEmpty()) {
            showToast("用户未登录");
            return true;
        }
        return false;
    }

    /**
     * 发送地址请求（新增/编辑通用）
     */
    private void sendAddressRequest(String url, FormBody formBody, String operateType) {
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast(operateType + "失败：" + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    AddressResponse addressResponse = mGson.fromJson(responseStr, AddressResponse.class);

                    runOnUiThread(() -> {
                        if (addressResponse.getCode() == 200) {
                            showToast("地址" + operateType + "成功");
                            addAddressDialog.dismiss();
                            loadAddressList(); // 刷新列表
                        } else {
                            showToast(operateType + "失败：" + addressResponse.getMsg());
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast(operateType + "失败：" + response.code());
                    });
                }
            }
        });
    }

    /**
     * 加载地址列表
     */
    private void loadAddressList() {
        // 1. 获取userId
        String userId = SPUtils.getUserId(getApplicationContext());
        if (userId.isEmpty()) {
            showToast("用户ID为空，请先登录");
            tvEmptyTip.setVisibility(View.VISIBLE);
            return;
        }

        // 2. 拼接API地址
        String apiUrl = "https://api.rzkj.qyqd123.cn/Android/UserMap/GetUserMap?userId=" + userId;

        // 3. 创建网络请求
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        // 4. 异步请求
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("网络请求失败：" + e.getMessage());
                    tvEmptyTip.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    AddressResponse addressResponse = mGson.fromJson(responseStr, AddressResponse.class);

                    runOnUiThread(() -> {
                        if (addressResponse.getCode() == 200) {
                            List<UserAddress> addressList = addressResponse.getData();
                            if (addressList == null || addressList.isEmpty()) {
                                tvEmptyTip.setVisibility(View.VISIBLE);
                                rvAddressList.setVisibility(View.GONE);
                            } else {
                                tvEmptyTip.setVisibility(View.GONE);
                                rvAddressList.setVisibility(View.VISIBLE);
                                mAdapter.updateData(addressList);
                            }
                        } else {
                            showToast("查询失败：" + addressResponse.getMsg());
                            tvEmptyTip.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast("请求失败：" + response.code());
                        tvEmptyTip.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    /**
     * 修改地址回调
     */
    @Override
    public void onEdit(UserAddress address) {
        isEditMode = true;
        editAddress = address;
        showAddAddressDialog(); // 打开编辑弹窗
    }

    /**
     * 删除地址回调
     */
    @Override
    public void onDelete(UserAddress address) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要删除该地址吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    deleteAddress(address.getId());
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除地址
     */
    private void deleteAddress(int mapId) {
        FormBody formBody = new FormBody.Builder()
                .add("mapId", String.valueOf(mapId))
                .build();

        Request request = new Request.Builder()
                .url("https://api.rzkj.qyqd123.cn/Android/UserMap/DeleteUserMap")
                .post(formBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("删除失败：" + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseStr = response.body().string();
                    AddressResponse deleteResponse = mGson.fromJson(responseStr, AddressResponse.class);

                    runOnUiThread(() -> {
                        if (deleteResponse.getCode() == 200) {
                            showToast("删除成功");
                            loadAddressList();
                        } else {
                            showToast("删除失败：" + deleteResponse.getMsg());
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast("删除失败：" + response.code());
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddressList();
    }
}
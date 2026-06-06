package com.wisdom.clound.tabbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wisdom.clound.R;
import com.wisdom.clound.ui.goods.GoodsDetailsActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    // 控件声明
    private ImageView ivLogo, ivBanner;
    private LinearLayout llTabContainer, llCategoryContainer;
    private SwipeRefreshLayout srlGoods;
    private RecyclerView rvGoods;

    // 数据集合
    private List<TabBean> tabList = new ArrayList<>();
    private List<CategoryBean> categoryList = new ArrayList<>();
    private List<GoodsBean> goodsList = new ArrayList<>();

    // 选中状态记录
    private TextView selectedTab;
    private LinearLayout selectedCategoryItem;

    // 分页参数
    private int currentTypesId = 1;
    private int currentPage = 1;
    private boolean isLoading = false;

    // 商品适配器
    private GoodsAdapter goodsAdapter;

    // OkHttp客户端
    private OkHttpClient okHttpClient;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String BASE_URL = "https://api.rzkj.qyqd123.cn/Android";
    private static final String UPDATE_API = "/WalletActivity/GetWeChatConfig";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initOkHttpClient();
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        initView(rootView);
        initGoodsList();

        getHomeImagesFromApi();
        getTabListFromApi();
        getGoodsListFromApi(currentTypesId, currentPage, true);
        checkAppUpdate();

        return rootView;
    }

    private void initOkHttpClient() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private void initView(View rootView) {
        ivLogo = rootView.findViewById(R.id.iv_logo);
//        ivBanner = rootView.findViewById(R.id.iv_banner);
        llTabContainer = rootView.findViewById(R.id.ll_tab_container);
        llCategoryContainer = rootView.findViewById(R.id.ll_category_container);
        srlGoods = rootView.findViewById(R.id.srl_goods);
        rvGoods = rootView.findViewById(R.id.rv_goods);
    }

    private void initGoodsList() {
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        rvGoods.setLayoutManager(layoutManager);
        goodsAdapter = new GoodsAdapter(goodsList);
        rvGoods.setAdapter(goodsAdapter);

        srlGoods.setColorSchemeColors(Color.RED);
        srlGoods.setOnRefreshListener(() -> {
            currentPage = 1;
            getGoodsListFromApi(currentTypesId, currentPage, true);
        });
    }

    // 统一OkHttp GET请求
    private void getRequest(String urlPath, HttpCallback callback) {
        String fullUrl = BASE_URL + urlPath;
        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFailed("网络请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = response.body() != null ? response.body().string() : "";
                mainHandler.post(() -> callback.onSuccess(result));
            }
        });
    }

    private interface HttpCallback {
        void onSuccess(String result);
        void onFailed(String error);
    }

    // 1、首页Logo、Banner【标准JSON解析，删除字符串截取】
    private void getHomeImagesFromApi() {
        getRequest("/HomeFragment/GetIndexMain", new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject root = new JSONObject(result);
                    if (root.getInt("code") == 200) {
                        JSONObject data = root.getJSONObject("data");
                        String logoUrl = data.getString("Logo");
                        String bannerUrl = data.getString("Banner");
                        loadImage(logoUrl, ivLogo);
//                        loadImage(bannerUrl, ivBanner);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("解析图片失败");
                }
            }

            @Override
            public void onFailed(String error) {
                showToast("获取图片失败：" + error);
            }
        });
    }

    // 2、顶部Tab【标准JSONArray解析，废除原来split切割字符串】
    private void getTabListFromApi() {
        getRequest("/HomeFragment/GetTabs", new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject root = new JSONObject(result);
                    if (root.getInt("code") == 200) {
                        tabList.clear();
                        JSONArray arr = root.getJSONArray("data");
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.getJSONObject(i);
                            String id = item.getString("Id");
                            String name = item.getString("NavigationKey");
                            tabList.add(new TabBean(id, name));
                        }
                        addTabsToLayout();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("解析选项卡失败");
                }
            }

            @Override
            public void onFailed(String error) {
                tabList.clear();
                addTabsToLayout();
            }
        });
    }

    //3、分类
    private void getCategoryListFromApi() {
        getRequest("/HomeFragment/GetCategories", new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    parseCategoryData(result);
                    addCategoriesToLayout();
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("解析分类失败：" + e.getMessage());
                }
            }

            @Override
            public void onFailed(String error) {
                categoryList.clear();
                addCategoriesToLayout();
            }
        });
    }

    //4、商品列表
    private void getGoodsListFromApi(int typesId, int page, boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;
        srlGoods.setRefreshing(true);
        String url = "/HomeFragment/GetGoodsList?typesId=" + typesId + "&page=" + page;

        getRequest(url, new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<GoodsBean> newList = parseGoodsData(result);
                    if (isRefresh) goodsList.clear();
                    goodsList.addAll(newList);
                    goodsAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("解析商品失败：" + e.getMessage());
                } finally {
                    isLoading = false;
                    srlGoods.setRefreshing(false);
                }
            }

            @Override
            public void onFailed(String error) {
                showToast("获取商品失败：" + error);
                isLoading = false;
                srlGoods.setRefreshing(false);
            }
        });
    }

    //=====================版本更新（保留原有逻辑）=====================
    private String getLocalAppVersion() {
        try {
            PackageManager pm = requireActivity().getPackageManager();
            PackageInfo info = pm.getPackageInfo(requireActivity().getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    private boolean needUpdate(String onlineVer, String localVer) {
        try {
            String[] onlineArr = onlineVer.split("\\.");
            String[] localArr = localVer.split("\\.");
            int maxLen = Math.max(onlineArr.length, localArr.length);
            for (int i = 0; i < maxLen; i++) {
                int o = i < onlineArr.length ? Integer.parseInt(onlineArr[i]) : 0;
                int l = i < localArr.length ? Integer.parseInt(localArr[i]) : 0;
                if (o > l) return true;
                if (o < l) return false;
            }
            return false;
        } catch (Exception e) {
            return !onlineVer.equals(localVer);
        }
    }

    private void checkAppUpdate() {
        getRequest(UPDATE_API, new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject root = new JSONObject(result);
                    if (root.getInt("code") == 200) {
                        JSONObject data = root.getJSONObject("data");
                        String onlineVersion = data.getString("Version");
                        String downloadUrl = data.optString("Url", "");
                        String localVersion = getLocalAppVersion();
                        if (needUpdate(onlineVersion, localVersion)) {
                            showUpdateDialog(onlineVersion, downloadUrl);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(String error) {
            }
        });
    }

    private void showUpdateDialog(String newVer, String apkUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("发现新版本：" + newVer);
        builder.setMessage("当前版本过低，建议立即更新APP");
        // 取消按钮
        builder.setNegativeButton("取消", null);
        Log.d("HomeFragment", "showUpdateDialog: "+ apkUrl);
        // 立即更新按钮
        builder.setPositiveButton("立即更新", (dialog, which) -> {
            if (TextUtils.isEmpty(apkUrl)) {
                showToast("暂无下载地址，请联系管理员配置更新链接");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
            startActivity(intent);
        });
        builder.setCancelable(false);

        // 先显示弹窗，再获取按钮修改样式
        AlertDialog dialog = builder.show();
        // 获取取消按钮
        Button cancelBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        // 获取立即更新按钮
        Button updateBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        // 设置按钮文字颜色为蓝色
        cancelBtn.setTextColor(getResources().getColor(R.color.blue));
        updateBtn.setTextColor(getResources().getColor(R.color.blue));

        // 设置按钮文字大小 22dp
        cancelBtn.setTextSize(20);
        updateBtn.setTextSize(20);
    }

    //=====================JSON解析方法=====================
    private void parseCategoryData(String json) throws Exception {
        categoryList.clear();
        JSONObject rootObj = new JSONObject(json);
        if (rootObj.getInt("code") == 200) {
            JSONArray dataArray = rootObj.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject categoryObj = dataArray.getJSONObject(i);
                int id = categoryObj.getInt("Id");
                String typeName = categoryObj.getString("TypeName");
                String typeImage = categoryObj.getString("TypeImage");
                categoryList.add(new CategoryBean(id, typeName, typeImage));
            }
        }
    }

    private List<GoodsBean> parseGoodsData(String json) throws Exception {
        List<GoodsBean> list = new ArrayList<>();
        JSONObject rootObj = new JSONObject(json);
        if (rootObj.getInt("code") == 200) {
            JSONArray dataArray = rootObj.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject goodsObj = dataArray.getJSONObject(i);
                int id = goodsObj.getInt("Id");
                String goodsName = goodsObj.getString("GoodsName");
                String goodsAvatar = goodsObj.getString("GoodsAvatar");
                double goodsWallet = goodsObj.getDouble("GoodsWallet");
                String goodsContent = goodsObj.optString("GoodsContent", "");
                list.add(new GoodsBean(id, goodsName, goodsAvatar, goodsWallet, goodsContent));
            }
        }
        return list;
    }

    //=====================UI渲染=====================
    private void addTabsToLayout() {
        llTabContainer.removeAllViews();
        for (int i = 0; i < tabList.size(); i++) {
            TabBean tab = tabList.get(i);
            TextView tvTab = new TextView(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 30, 0);
            tvTab.setLayoutParams(params);
            tvTab.setText(tab.getName());
            tvTab.setTextColor(Color.WHITE);
            tvTab.setTextSize(18);
            tvTab.setGravity(Gravity.CENTER);
            tvTab.setPadding(5, 4, 5, 4);

            if (i == 0) {
                tvTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                        getResources().getDrawable(R.drawable.shape_tab_underline));
                tvTab.setCompoundDrawablePadding(16);
                selectedTab = tvTab;
                getCategoryListFromApi();
            }

            tvTab.setOnClickListener(v -> {
                if (selectedTab != null)
                    selectedTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                tvTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                        getResources().getDrawable(R.drawable.shape_tab_underline));
                tvTab.setCompoundDrawablePadding(16);
                selectedTab = tvTab;
                getCategoryListFromApi();
            });
            llTabContainer.addView(tvTab);
        }
    }

    private void addCategoriesToLayout() {
        llCategoryContainer.removeAllViews();
        selectedCategoryItem = null;
        for (CategoryBean category : categoryList) {
            LinearLayout categoryItem = new LinearLayout(getActivity());
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            itemParams.setMargins(20, 0, 20, 0);
            categoryItem.setLayoutParams(itemParams);
            categoryItem.setOrientation(LinearLayout.VERTICAL);
            categoryItem.setGravity(Gravity.CENTER);
            categoryItem.setPadding(50, 30, 50, 30);
            categoryItem.setBackgroundResource(R.drawable.shape_category_bg);

            ImageView ivIcon = new ImageView(getActivity());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(144, 144);
            ivIcon.setLayoutParams(iconParams);
            ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            loadImage(category.getTypeImage(), ivIcon);

            TextView tvName = new TextView(getActivity());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textParams.setMargins(0, 8, 0, 0);
            tvName.setLayoutParams(textParams);
            tvName.setText(category.getTypeName());
            tvName.setTextColor(Color.BLACK);
            tvName.setTextSize(15);
            tvName.setGravity(Gravity.CENTER);

            categoryItem.addView(ivIcon);
            categoryItem.addView(tvName);

            categoryItem.setOnClickListener(v -> {
                if (selectedCategoryItem != null) selectedCategoryItem.setSelected(false);
                categoryItem.setSelected(true);
                selectedCategoryItem = categoryItem;
                currentTypesId = category.getId();
                currentPage = 1;
                getGoodsListFromApi(currentTypesId, currentPage, true);
            });

            if (category.getId() == 1 && selectedCategoryItem == null) {
                categoryItem.setSelected(true);
                selectedCategoryItem = categoryItem;
            }
            llCategoryContainer.addView(categoryItem);
        }
    }

    //=====================工具方法=====================
    private void loadImage(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                InputStream is = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                is.close();
            } catch (Exception e) {
                mainHandler.post(() -> showToast("图片加载失败"));
            }
        }).start();
    }

    private void showToast(String msg) {
        if (TextUtils.isEmpty(msg) || getContext() == null) return;
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

    //=====================实体类=====================
    public static class TabBean {
        private String key;
        private String name;

        public TabBean(String key, String name) {
            this.key = key;
            this.name = name;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
    }

    public static class CategoryBean {
        private int id;
        private String typeName;
        private String typeImage;

        public CategoryBean(int id, String typeName, String typeImage) {
            this.id = id;
            this.typeName = typeName;
            this.typeImage = typeImage;
        }

        public int getId() { return id; }
        public String getTypeName() { return typeName; }
        public String getTypeImage() { return typeImage; }
    }

    public static class GoodsBean {
        private int id;
        private String goodsName;
        private String goodsImage;
        private double price;
        private String description;

        public GoodsBean(int id, String goodsName, String goodsImage, double price, String description) {
            this.id = id;
            this.goodsName = goodsName;
            this.goodsImage = goodsImage;
            this.price = price;
            this.description = description;
        }

        public int getId() { return id; }
        public String getGoodsName() { return goodsName; }
        public String getGoodsImage() { return goodsImage; }
        public double getPrice() { return price; }
        public String getDescription() { return description; }
    }

    //=====================商品适配器=====================
    private class GoodsAdapter extends RecyclerView.Adapter<GoodsAdapter.GoodsViewHolder> {
        private List<GoodsBean> mGoodsList;

        public GoodsAdapter(List<GoodsBean> goodsList) {
            this.mGoodsList = goodsList;
        }

        @NonNull
        @Override
        public GoodsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goods, parent, false);
            return new GoodsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GoodsViewHolder holder, int position) {
            GoodsBean goods = mGoodsList.get(position);
            holder.tvName.setText(goods.getGoodsName());
            holder.tvPrice.setText("¥" + String.format("%.2f", goods.getPrice()));
            loadImage(goods.getGoodsImage(), holder.ivImage);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), GoodsDetailsActivity.class);
                intent.putExtra("goodsId", goods.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return mGoodsList.size();
        }

        class GoodsViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvName, tvPrice;

            public GoodsViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_goods_image);
                tvName = itemView.findViewById(R.id.tv_goods_name);
                tvPrice = itemView.findViewById(R.id.tv_goods_price);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (okHttpClient != null) okHttpClient.dispatcher().cancelAll();
    }
}
package com.wisdom.clound.tabbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
    private int currentTypesId = 1; // 默认typesId=1
    private int currentPage = 1;    // 默认page=1
    private boolean isLoading = false; // 防止重复请求
    private boolean hasMoreData = true; // 是否有更多数据

    // 商品适配器
    private GoodsAdapter goodsAdapter;

    // OkHttp客户端（全局单例）
    private OkHttpClient okHttpClient;
    // 主线程Handler（用于更新UI）
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    // 服务器基础地址（必须替换为你的实际地址）
    private static final String BASE_URL = "https://api.rzkj.qyqd123.cn/Android"; // 示例：局域网IP+端口

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 初始化OkHttp客户端
        initOkHttpClient();

        // 加载布局
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化控件
        initView(rootView);

        // 初始化商品列表（RecyclerView+分页）
        initGoodsList();

        // 1. 获取首页Logo和Banner
        getHomeImagesFromApi();

        // 2. 获取顶部Tab选项卡数据
        getTabListFromApi();

        // 3. 默认加载typesId=1的第一页商品
        getGoodsListFromApi(currentTypesId, currentPage, true);

        return rootView;
    }


    /**
     * 初始化OkHttpClient
     */
    private void initOkHttpClient() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)  // 连接超时
                .readTimeout(15, TimeUnit.SECONDS)     // 读取超时
                .writeTimeout(15, TimeUnit.SECONDS)    // 写入超时
                .retryOnConnectionFailure(true)        // 连接失败重试
                .build();
    }

    /**
     * 初始化控件
     */
    private void initView(View rootView) {
        ivLogo = rootView.findViewById(R.id.iv_logo);
        ivBanner = rootView.findViewById(R.id.iv_banner);
        llTabContainer = rootView.findViewById(R.id.ll_tab_container);
        llCategoryContainer = rootView.findViewById(R.id.ll_category_container);
        srlGoods = rootView.findViewById(R.id.srl_goods);
        rvGoods = rootView.findViewById(R.id.rv_goods);
    }

    /**
     * 初始化商品列表（RecyclerView+下拉刷新+上拉加载）
     */
    private void initGoodsList() {
        // 网格布局：2列展示商品
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        rvGoods.setLayoutManager(layoutManager);

        // 初始化适配器
        goodsAdapter = new GoodsAdapter(goodsList);
        rvGoods.setAdapter(goodsAdapter);

        // 下拉刷新配置（移到外层，正常生效）
        srlGoods.setColorSchemeColors(Color.RED); // 刷新进度条颜色
        srlGoods.setOnRefreshListener(() -> {
            // 下拉刷新：重置页码为1，重新加载当前分类商品
            currentPage = 1;
            getGoodsListFromApi(currentTypesId, currentPage, true);
        });
    }

    // -------------------------- 网络请求封装 --------------------------
    /**
     * OkHttp GET请求封装
     * @param urlPath 接口路径（相对于BASE_URL）
     * @param callback 回调
     */
    private void getRequest(String urlPath, HttpCallback callback) {
        String fullUrl = BASE_URL + urlPath;
        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 子线程切换到主线程回调
                mainHandler.post(() -> callback.onFailed("网络请求失败：" + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailed("响应失败：" + response.code()));
                    return;
                }

                String result = response.body() != null ? response.body().string() : "";
                // 子线程切换到主线程回调
                mainHandler.post(() -> callback.onSuccess(result));
            }
        });
    }

    // 自定义网络请求回调接口
    private interface HttpCallback {
        void onSuccess(String result);
        void onFailed(String error);
    }

    // -------------------------- 1. 获取首页Logo和Banner图片 --------------------------
    private void getHomeImagesFromApi() {
        getRequest("/HomeFragment/GetIndexMain", new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    String logoUrl = extractUrl(result, "Logo");
                    String bannerUrl = extractUrl(result, "Banner");
                    loadImage(logoUrl, ivLogo);
                    loadImage(bannerUrl, ivBanner);
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

    // -------------------------- 2. 获取顶部Tab选项卡数据 --------------------------
    private void getTabListFromApi() {
        getRequest("/HomeFragment/GetTabs", new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    parseTabData(result);
                    addTabsToLayout();
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("解析选项卡失败");
                }
            }

            @Override
            public void onFailed(String error) {
                // 接口失败时用模拟数据
                tabList.clear();
                // tabList.add(new TabBean("self", "自营区"));
                // tabList.add(new TabBean("hot", "热销区"));
                // tabList.add(new TabBean("top", "爆款区"));
                addTabsToLayout();
            }
        });
    }

    // -------------------------- 3. 获取分类标签数据 --------------------------
    private void getCategoryListFromApi() {
        String url = "/HomeFragment/GetCategories";
        getRequest(url, new HttpCallback() {
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
                // 接口失败时用模拟数据
                categoryList.clear();
                addCategoriesToLayout();
            }
        });
    }

    // -------------------------- 4. 获取商品列表（核心：适配你的API格式） --------------------------
    private void getGoodsListFromApi(int typesId, int page, boolean isRefresh) {
        if (isLoading) return; // 防止重复请求
        isLoading = true;

        // 显示加载状态
        srlGoods.setRefreshing(true);

        // 拼接商品API地址
        String url = "/HomeFragment/GetGoodsList?typesId=" + typesId + "&page=" + page;

        getRequest(url, new HttpCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    // 解析商品数据
                    List<GoodsBean> newGoodsList = parseGoodsData(result);

                    // 刷新=清空旧数据
                    if (isRefresh) {
                        goodsList.clear();
                    }

                    // 添加新数据
                    if (newGoodsList.size() > 0) {
                        goodsList.addAll(newGoodsList);
                    } else {
                        showToast("已加载全部商品");
                    }

                    // 更新列表
                    goodsAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("解析商品失败：" + e.getMessage());
                } finally {
                    // 关闭加载状态
                    isLoading = false;
                    srlGoods.setRefreshing(false);
                }
            }

            @Override
            public void onFailed(String error) {
                showToast("获取商品失败：" + error);
                // 关闭加载状态
                isLoading = false;
                srlGoods.setRefreshing(false);
            }
        });
    }

    // -------------------------- 数据解析方法 --------------------------
    /**
     * 解析Logo/Banner地址
     */
    private String extractUrl(String json, String key) {
        String startStr = "\"" + key + "\":\"";
        int startIndex = json.indexOf(startStr) + startStr.length();
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }

    /**
     * 解析Tab选项卡数据
     */
    private void parseTabData(String json) {
        tabList.clear();
        json = json.replace("[", "").replace("]", "");
        String[] items = json.split("\\},\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String key = extractValue(item, "Id");
            String name = extractValue(item, "NavigationKey");
            tabList.add(new TabBean(key, name));
        }
    }

    /**
     * 提取Tab字段值
     */
    private String extractValue(String item, String key) {
        String startStr = "\"" + key + "\":\"";
        int startIndex = item.indexOf(startStr) + startStr.length();
        int endIndex = item.indexOf("\"", startIndex);
        return item.substring(startIndex, endIndex);
    }

    /**
     * 解析分类标签数据
     */
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

    /**
     * 解析商品列表数据（适配你的API格式：GoodsAvatar/GoodsName/GoodsWallet/Id）
     */
    private List<GoodsBean> parseGoodsData(String json) throws Exception {
        List<GoodsBean> list = new ArrayList<>();
        JSONObject rootObj = new JSONObject(json);

        // 校验返回码
        if (rootObj.getInt("code") == 200) {
            JSONArray dataArray = rootObj.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject goodsObj = dataArray.getJSONObject(i);

                // 读取你的API字段
                int id = goodsObj.getInt("Id"); // 商品ID
                String goodsName = goodsObj.getString("GoodsName"); // 商品名称
                String goodsAvatar = goodsObj.getString("GoodsAvatar"); // 商品图片
                double goodsWallet = goodsObj.getDouble("GoodsWallet"); // 商品价格
                String goodsContent = goodsObj.optString("GoodsContent", ""); // 商品描述（可选）

                // 添加到商品列表
                list.add(new GoodsBean(id, goodsName, goodsAvatar, goodsWallet, goodsContent));
            }
        }
        return list;
    }

    // -------------------------- 布局渲染方法 --------------------------
    /**
     * 渲染Tab选项卡到布局
     */
    private void addTabsToLayout() {
        llTabContainer.removeAllViews();
        for (int i = 0; i < tabList.size(); i++) {
            TabBean tab = tabList.get(i);
            TextView tvTab = new TextView(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 30, 0);
            tvTab.setLayoutParams(params);
            tvTab.setText(tab.getName());
            tvTab.setTextColor(Color.WHITE);
            tvTab.setTextSize(18);
            tvTab.setGravity(Gravity.CENTER);
            tvTab.setPadding(5, 4, 5, 4);

            // 默认选中第一个Tab
            if (i == 0) {
                tvTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                        getResources().getDrawable(R.drawable.shape_tab_underline));
                tvTab.setCompoundDrawablePadding(16);
                selectedTab = tvTab;
                getCategoryListFromApi();
            }

            // Tab点击事件
            tvTab.setOnClickListener(v -> {
                if (selectedTab != null) {
                    selectedTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
                tvTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                        getResources().getDrawable(R.drawable.shape_tab_underline));
                tvTab.setCompoundDrawablePadding(16);
                selectedTab = tvTab;
                getCategoryListFromApi();
            });

            llTabContainer.addView(tvTab);
        }
    }

    /**
     * 渲染分类标签到布局（支持横向滑动）
     */
    private void addCategoriesToLayout() {
        llCategoryContainer.removeAllViews();
        selectedCategoryItem = null; // 重置选中状态

        for (CategoryBean category : categoryList) {
            LinearLayout categoryItem = new LinearLayout(getActivity());
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            itemParams.setMargins(20, 0, 20, 0);
            categoryItem.setLayoutParams(itemParams);
            categoryItem.setOrientation(LinearLayout.VERTICAL);
            categoryItem.setGravity(Gravity.CENTER);
            categoryItem.setPadding(50, 30, 50, 30);
            categoryItem.setBackgroundResource(R.drawable.shape_category_bg);

            // 分类图标
            ImageView ivIcon = new ImageView(getActivity());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(144, 144);
            ivIcon.setLayoutParams(iconParams);
            ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // ivIcon.setImageResource(R.mipmap.ic_launcher);
            loadImage(category.getTypeImage(), ivIcon);

            // 分类文字
            TextView tvName = new TextView(getActivity());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            textParams.setMargins(0, 8, 0, 0);
            tvName.setLayoutParams(textParams);
            tvName.setText(category.getTypeName());
            tvName.setTextColor(Color.BLACK);
            tvName.setTextSize(15);
            tvName.setGravity(Gravity.CENTER);

            // 添加图标和文字到分类项
            categoryItem.addView(ivIcon);
            categoryItem.addView(tvName);

            // 分类点击事件（核心：切换分类+加载对应商品）
            categoryItem.setOnClickListener(v -> {
                // 切换选中状态
                if (selectedCategoryItem != null) {
                    selectedCategoryItem.setSelected(false);
                }
                categoryItem.setSelected(true);
                selectedCategoryItem = categoryItem;

                // 重置分页，加载对应typesId的第一页商品
                currentTypesId = category.getId();
                currentPage = 1;
                getGoodsListFromApi(currentTypesId, currentPage, true);
            });

            // 默认选中第一个分类（typesId=1）
            if (category.getId() == 1 && selectedCategoryItem == null) {
                categoryItem.setSelected(true);
                selectedCategoryItem = categoryItem;
            }

            llCategoryContainer.addView(categoryItem);
        }
    }

    // -------------------------- 工具方法 --------------------------
    /**
     * 加载网络图片（原生实现，无第三方依赖）
     */
    private void loadImage(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                InputStream is = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> showToast("图片加载失败"));
            }
        }).start();
    }

    // ********** 统一封装：黑色透明Toast + 居中 + 无图标 **********
    private void showToast(String msg) {
        if (TextUtils.isEmpty(msg) || getContext() == null) {
            return;
        }
        // 自定义纯文字TextView，无系统默认图标
        TextView textView = new TextView(getContext());
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(0xFFFFFFFF); // 白色文字
        textView.setBackgroundColor(0xCC000000); // 黑色半透明背景
        textView.setPadding(50, 25, 50, 25);
        textView.setGravity(Gravity.CENTER);

        // 创建Toast并居中显示
        Toast toast = new Toast(getContext());
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // -------------------------- 数据模型类 --------------------------
    /**
     * Tab选项卡模型
     */
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

    /**
     * 分类标签模型
     */
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

    /**
     * 商品模型（适配你的API字段）
     */
    public static class GoodsBean {
        private int id;             // 商品ID
        private String goodsName;   // 商品名称（GoodsName）
        private String goodsImage;  // 商品图片（GoodsAvatar）
        private double price;       // 商品价格（GoodsWallet）
        private String description; // 商品描述（GoodsContent）

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

    // -------------------------- 商品列表适配器 --------------------------
    private class GoodsAdapter extends RecyclerView.Adapter<GoodsAdapter.GoodsViewHolder> {
        private List<GoodsBean> mGoodsList;

        public GoodsAdapter(List<GoodsBean> goodsList) {
            this.mGoodsList = goodsList;
        }

        @NonNull
        @Override
        public GoodsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 加载商品项布局
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goods, parent, false);
            return new GoodsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GoodsViewHolder holder, int position) {
            GoodsBean goods = mGoodsList.get(position);

            // 加载中提示项
            if (goods.getId() == -1) {
                holder.tvName.setText(goods.getGoodsName());
                holder.ivImage.setImageResource(R.drawable.ic_lodding);
                holder.tvPrice.setVisibility(View.GONE);
                return;
            }

            // 正常商品项
            holder.tvName.setText(goods.getGoodsName());
            // 价格格式化：保留2位小数（适配GoodsWallet的四位小数）
            holder.tvPrice.setText("¥" + String.format("%.2f", goods.getPrice()));
            // 加载商品图片
            loadImage(goods.getGoodsImage(), holder.ivImage);
            holder.tvPrice.setVisibility(View.VISIBLE);

            // 商品点击事件（可选：可跳转到商品详情页）
            holder.itemView.setOnClickListener(v -> {
                if (getActivity() == null) return;

                // 跳转到 GoodsDetailsActivity，仅传递商品ID
                Intent intent = new Intent(getActivity(), GoodsDetailsActivity.class);
                intent.putExtra("goodsId", goods.getId());
                getActivity().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return mGoodsList.size();
        }

        /**
         * 商品ViewHolder
         */
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

    // -------------------------- 生命周期 --------------------------
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 取消所有未完成的请求
        if (okHttpClient != null) {
            okHttpClient.dispatcher().cancelAll();
        }
    }
}
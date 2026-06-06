package com.wisdom.clound.tabbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.wisdom.clound.Bean.TabBean;
import com.wisdom.clound.Bean.TabResponse;
import com.wisdom.clound.Bean.VideoItemBean;
import com.wisdom.clound.Bean.VideoListResponse;
import com.wisdom.clound.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.wisdom.clound.ui.video.VideoPlayActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoFragment extends Fragment {
    // 接口基础地址
    private static final String BASE_URL = "https://api.rzkj.qyqd123.cn/Android";

    public interface OnFilterItemClickListener {
        void onItemClick(TabBean tabBean);
    }
    // 控件声明
    private HorizontalScrollView hsvTab;          // 横向滚动Tab容器
    private LinearLayout llTabContainer;         // Tab文字容器
    private ImageView ivTabDropDown;             // Tab右侧下拉按钮
    private RecyclerView rvVideoList;            // 视频列表
    private SwipeRefreshLayout srlRefresh;       // 下拉刷新

    // 数据变量
    private List<TabBean> tabList = new ArrayList<>();       // IndexTab列表
    private List<VideoItemBean> videoList = new ArrayList<>(); // 视频列表
    private TabResponse tabResponse;             // 完整Tab数据（含FullTab）
    private TextView selectedTab;                // 当前选中的Tab
    private int currentTabId = 1;                // 默认选中第一个Tab
    private int currentPage = 1;                 // 默认页码
    private boolean isLoading = false;           // 加载状态标记
    private boolean hasMoreData = true;          // 是否有更多数据

    // OKHttp和Gson
    private OkHttpClient okHttpClient;
    private Gson gson;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        // 初始化工具类
        okHttpClient = new OkHttpClient.Builder().build();
        gson = new Gson();

        // 初始化控件
        initViews(view);
        // 加载Tab数据
        loadTabData();
        // 初始化视频列表
        initVideoList();

        return view;
    }

    /**
     * 初始化所有控件
     */
    private void initViews(View view) {
        // Tab相关控件
        hsvTab = view.findViewById(R.id.hsv_tab);
        llTabContainer = view.findViewById(R.id.ll_tab_container);
        ivTabDropDown = view.findViewById(R.id.iv_tab_dropdown);

        // 视频列表+下拉刷新
        rvVideoList = view.findViewById(R.id.rv_video_list);
        srlRefresh = view.findViewById(R.id.srl_refresh);

        // 下拉筛选按钮点击事件（弹出筛选弹窗）
        ivTabDropDown.setOnClickListener(v -> {
            if (tabResponse != null && tabResponse.getData() != null) {
                // ✅ 修复：替换为静态创建方法
                FilterBottomSheetDialog dialog = FilterBottomSheetDialog.newInstance(tabResponse.getData().getFullTab());
                dialog.setOnFilterItemClickListener(tabBean -> {
                    // 筛选选中后切换Tab
                    switchTab(tabBean.getId(), tabBean.getKeyName());
                    dialog.dismiss();
                });
                dialog.show(getChildFragmentManager(), "FilterDialog");
            } else {
                showToast("筛选数据未加载完成");
            }
        });
    }

    /**
     * 加载Tab数据（对接GetTabs接口）
     */
    private void loadTabData() {
        String tabUrl = BASE_URL + "/VideoFragment/GetTabs";
        Log.d("VideoTab", "请求地址：" + tabUrl);

        Request request = new Request.Builder()
                .url(tabUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("VideoTab", "Tab请求失败：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    showToast("分类加载失败");
                    // 模拟数据兜底
                    mockTabData();
                    renderTabLayout();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || getActivity() == null) return;

                String result = response.body().string();
                Log.d("VideoTab", "Tab返回数据：" + result);

                getActivity().runOnUiThread(() -> {
                    try {
                        // 解析Tab数据
                        tabResponse = gson.fromJson(result, TabResponse.class);
                        if (tabResponse.getCode() == 200 && tabResponse.getData() != null) {
                            tabList.clear();
                            tabList.addAll(tabResponse.getData().getIndexTab());
                        } else {
                            showToast("分类数据异常");
                            mockTabData();
                        }
                        // 渲染Tab布局
                        renderTabLayout();
                    } catch (Exception e) {
                        Log.e("VideoTab", "Tab解析失败", e);
                        showToast("分类解析失败");
                        mockTabData();
                        renderTabLayout();
                    }
                });
            }
        });
    }

    /**
     * 模拟Tab数据（接口失败时兜底）
     */
    private void mockTabData() {
        tabList.clear();
        tabList.add(new TabBean(1, "推荐"));
        tabList.add(new TabBean(2, "新剧"));
        tabList.add(new TabBean(3, "都市情感"));
        tabList.add(new TabBean(4, "都市"));
        tabList.add(new TabBean(5, "古装"));
        tabList.add(new TabBean(16, "逆袭"));
    }

    /**
     * 渲染滑动Tab选项卡（匹配参考图样式）
     */
    private void renderTabLayout() {
        if (getActivity() == null || llTabContainer == null) return;

        llTabContainer.removeAllViews();
        for (int i = 0; i < tabList.size(); i++) {
            TabBean tab = tabList.get(i);
            TextView tvTab = new TextView(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dp2px(10), dp2px(8), dp2px(10), dp2px(8)); // Tab间距
            tvTab.setLayoutParams(params);
            tvTab.setText(tab.getKeyName());
            tvTab.setTextSize(16);
            tvTab.setPadding(dp2px(8), dp2px(4), dp2px(8), dp2px(4));

            // 默认选中第一个Tab（参考图样式：绿色背景+白色文字）
            if (i == 0) {
                tvTab.setTextColor(Color.WHITE);
                tvTab.setBackgroundResource(R.drawable.shape_tab_selected);
                tvTab.setSelected(true);
                selectedTab = tvTab;
                currentTabId = tab.getId();
                loadVideoList(currentTabId, 1, true); // 加载第一个Tab的视频
            } else {
                tvTab.setTextColor(Color.parseColor("#333333"));
                tvTab.setBackgroundColor(Color.TRANSPARENT);
                tvTab.setSelected(false);
            }

            // Tab点击事件
            tvTab.setOnClickListener(v -> switchTab(tab.getId(), tab.getKeyName()));
            llTabContainer.addView(tvTab);
        }
    }

    /**
     * 切换Tab通用方法
     */
    private void switchTab(int tabId, String tabName) {
        // 取消上一个Tab选中样式
        if (selectedTab != null) {
            selectedTab.setTextColor(Color.parseColor("#333333"));
            selectedTab.setBackgroundColor(Color.TRANSPARENT);
            selectedTab.setSelected(false);
        }

        // 设置当前Tab选中样式
        TextView currentTab = (TextView) hsvTab.findViewWithTag(tabId);
        if (currentTab == null) {
            // 如果Tab不在当前显示列表中，遍历找到对应Tab
            for (int i = 0; i < llTabContainer.getChildCount(); i++) {
                TextView tabView = (TextView) llTabContainer.getChildAt(i);
                if (tabView.getText().toString().equals(tabName)) {
                    currentTab = tabView;
                    break;
                }
            }
        }

        if (currentTab != null) {
            currentTab.setTextColor(Color.WHITE);
            currentTab.setBackgroundResource(R.drawable.shape_tab_selected);
            currentTab.setSelected(true);
            selectedTab = currentTab;

            // Tab居中滚动
            int scrollX = currentTab.getLeft() - (hsvTab.getWidth() / 2 - currentTab.getWidth() / 2);
            hsvTab.smoothScrollTo(scrollX, 0);
        }

        // 更新当前TabId并加载数据
        currentTabId = tabId;
        currentPage = 1;
        loadVideoList(currentTabId, currentPage, true);
//        showToast("切换到：" + tabName);
    }

    /**
     * 初始化视频列表（一行两列网格布局）
     */
    private void initVideoList() {
        if (getActivity() == null) return;

        // 网格布局：2列
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        rvVideoList.setLayoutManager(layoutManager);
        rvVideoList.setAdapter(new VideoAdapter(videoList));

        // 下拉刷新配置
        srlRefresh.setColorSchemeColors(Color.parseColor("#00CC99"));
        srlRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadVideoList(currentTabId, currentPage, true);
        });

        // 上拉加载更多
        rvVideoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (manager == null) return;

                int lastVisibleItem = manager.findLastCompletelyVisibleItemPosition();
                // 滑动到底部 + 有更多数据 + 不在加载中 → 加载下一页
                if (lastVisibleItem == videoList.size() - 1
                        && hasMoreData
                        && !isLoading
                        && dy > 0) {
                    currentPage++;
                    Log.d("video", "currentPage: "+currentPage);
                    loadVideoList(currentTabId, currentPage, false);
                }
            }
        });
    }

    /**
     * 加载视频列表（对接GetQmVideoList接口）
     */
    private void loadVideoList(int tabId, int page, boolean isRefresh) {
        if (isLoading || getActivity() == null) return;
        isLoading = true;

        // 构建请求地址
        String videoUrl = BASE_URL + "/VideoFragment/GetQmVideoList?nid=" + tabId + "&page=" + page;
        Log.d("VideoList", "请求地址：" + videoUrl);

        // 显示加载状态
        if (isRefresh) {
            srlRefresh.setRefreshing(true);
        } else {
            // 添加加载占位符
            VideoItemBean loadingBean = new VideoItemBean();
            loadingBean.setId(-1);
            videoList.add(loadingBean);
            rvVideoList.getAdapter().notifyItemInserted(videoList.size() - 1);
        }

        Request request = new Request.Builder()
                .url(videoUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("VideoList", "视频请求失败：" + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    showToast("视频加载失败");
                    finishLoad(isRefresh, null);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || getActivity() == null) {
                    getActivity().runOnUiThread(() -> finishLoad(isRefresh, null));
                    return;
                }

                String result = response.body().string();
                Log.d("VideoList", "视频返回数据：" + result);

                getActivity().runOnUiThread(() -> {
                    try {
                        VideoListResponse videoResponse = gson.fromJson(result, VideoListResponse.class);
                        List<VideoItemBean> newList = new ArrayList<>();
                        if (videoResponse.getCode() == 200 && videoResponse.getData() != null) {
                            newList = videoResponse.getData();
                        }
                        finishLoad(isRefresh, newList);
                    } catch (Exception e) {
                        Log.e("VideoList", "视频解析失败", e);
                        showToast("视频解析失败");
                        finishLoad(isRefresh, null);
                    }
                });
            }
        });
    }

    /**
     * 完成加载后的统一处理
     */
    private void finishLoad(boolean isRefresh, List<VideoItemBean> newList) {
        isLoading = false;
        srlRefresh.setRefreshing(false);

        if (isRefresh) {
            videoList.clear();
            hasMoreData = true;
        } else {
            // 移除加载占位符
            if (!videoList.isEmpty() && videoList.get(videoList.size() - 1).getId() == -1) {
                videoList.remove(videoList.size() - 1);
                rvVideoList.getAdapter().notifyItemRemoved(videoList.size());
            }
        }

        if (newList != null && newList.size() > 0) {
            videoList.addAll(newList);
            rvVideoList.getAdapter().notifyDataSetChanged();
        } else {
            hasMoreData = false;
            if (!isRefresh) {
                showToast("已加载全部视频");
            }
        }
    }

    /**
     * 视频列表适配器（适配短剧字段）
     */
    /**
     * 视频列表适配器（修复封面格式问题）
     */
    private class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private List<VideoItemBean> mList;
        private RequestOptions coverOptions; // 封面圆角配置

        public VideoAdapter(List<VideoItemBean> list) {
            this.mList = list;
            // ✅ 修复1：添加centerCrop保证图片比例，避免拉伸变形
            coverOptions = new RequestOptions()
                    .centerCrop() // 关键：按比例裁剪，适配ImageView
                    .transform(new RoundedCorners(dp2px(8)))
                    .error(R.drawable.ic_index_logo)
                    .placeholder(R.drawable.ic_index_logo);
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video, parent, false);

            // ✅ 修复2：强制2列均分宽度 + 固定封面高宽比（建议16:9或3:2）
            ViewGroup.LayoutParams params = view.getLayoutParams();
            int itemWidth = (parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight() - dp2px(10)) / 2;
            params.width = itemWidth;
            view.setLayoutParams(params);

            // 给封面ImageView设置固定高度（关键：保证所有封面比例一致）
            ImageView ivCover = view.findViewById(R.id.iv_video_cover);
            LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (itemWidth * 1.5f) // 宽高比2:3，可根据UI调整
            );
            ivCover.setLayoutParams(coverParams);

            return new VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            VideoItemBean bean = mList.get(position);

            // 加载封面图
            Glide.with(getActivity())
                    .load(bean.getImage())
                    .apply(coverOptions)
                    .into(holder.ivCover);

            // 设置标题
            holder.tvTitle.setText(bean.getTitle());
            // 设置标签（SubTitle）
            holder.tvSubTitle.setText(bean.getSubTitle());
            // 设置集数
            holder.tvTotalNum.setText(bean.getTotalNum());

            // Item点击事件（返回VideoId）
            holder.itemView.setOnClickListener(v -> {
                String videoId = bean.getVideoId();
                if (!TextUtils.isEmpty(videoId)) {
                    if (!TextUtils.isEmpty(videoId)) {
                        // 跳转到视频播放页
                        Intent intent = new Intent(getActivity(), VideoPlayActivity.class);
                        intent.putExtra("videoId", videoId);
                        startActivity(intent);
                    }
//                    showToast("视频ID：" + videoId);
                    // 后续可跳转播放页，传入videoId
                }
            });
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        /**
         * ViewHolder
         */
        class VideoViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;      // 视频封面
            TextView tvTitle;       // 视频标题
            TextView tvSubTitle;    // 标签
            TextView tvTotalNum;    // 总集数

            public VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_video_cover);
                tvTitle = itemView.findViewById(R.id.tv_video_title);
                tvSubTitle = itemView.findViewById(R.id.tv_sub_title);
                tvTotalNum = itemView.findViewById(R.id.tv_total_num);
            }
        }
    }

    /**
     * 筛选弹窗（BottomSheet）✅ 修复：添加static + 无参构造 + 静态传参
     */
    public static class FilterBottomSheetDialog extends BottomSheetDialogFragment {
        private List<TabResponse.DataBean.FullTabBean> fullTabList;
        private OnFilterItemClickListener listener;

        public FilterBottomSheetDialog(){}

        public static FilterBottomSheetDialog newInstance(List<TabResponse.DataBean.FullTabBean> data){
            FilterBottomSheetDialog dialog = new FilterBottomSheetDialog();
            Bundle bundle = new Bundle();
            bundle.putSerializable("tab_data", new ArrayList<>(data));
            dialog.setArguments(bundle);
            return dialog;
        }

        // 弹窗自己的dp转px
        private int dp2px(int dp){
            if(getResources() == null) return dp;
            float density = getResources().getDisplayMetrics().density;
            return (int)(dp * density + 0.5f);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if(getArguments() != null){
                fullTabList = (List<TabResponse.DataBean.FullTabBean>) getArguments().getSerializable("tab_data");
            }
        }

        public void setOnFilterItemClickListener(OnFilterItemClickListener listener) {
            this.listener = listener;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_filter, container, false);

            Window window = getDialog().getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            }

            LinearLayout llFilterContainer = view.findViewById(R.id.ll_filter_container);
            ImageView ivClose = view.findViewById(R.id.iv_close);
            ivClose.setOnClickListener(v -> dismiss());

            if (fullTabList != null && !fullTabList.isEmpty()) {
                for (TabResponse.DataBean.FullTabBean fullTab : fullTabList) {
                    TextView tvTitle = new TextView(getContext());
                    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    titleParams.setMargins(dp2px(15), dp2px(15), dp2px(15), dp2px(10));
                    tvTitle.setLayoutParams(titleParams);
                    tvTitle.setText(fullTab.getFullKeyName());
                    tvTitle.setTextSize(18);
                    tvTitle.setTextColor(Color.parseColor("#333333"));
                    llFilterContainer.addView(tvTitle);

                    LinearLayout llTagRow = new LinearLayout(getContext());
                    llTagRow.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    llTagRow.setOrientation(LinearLayout.HORIZONTAL);
                    llTagRow.setPadding(dp2px(10), 0, dp2px(10), 0);

                    int currentLineWidth = 0;
                    int screenWidth = getResources().getDisplayMetrics().widthPixels - dp2px(30);

                    // 替换FilterBottomSheetDialog中onCreateView里的子标签循环代码
                    for (TabBean subTab : fullTab.getSubKeyList()) {
                        TextView tvTag = new TextView(getContext());
                        tvTag.setText(subTab.getKeyName());
                        tvTag.setTextSize(14);
                        tvTag.setTextColor(Color.parseColor("#666666"));
                        tvTag.setBackgroundResource(R.drawable.shape_filter_tag);
                        tvTag.setPadding(dp2px(12), dp2px(6), dp2px(12), dp2px(6));

                        // ✅ 修复1：先添加到临时行，再测量真实宽度（避免测量误差）
                        llTagRow.addView(tvTag);
                        tvTag.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        int tagWidth = tvTag.getMeasuredWidth() + dp2px(10); // 包含间距

                        // ✅ 修复2：如果当前行放不下，先移除当前标签，再新建行
                        if (currentLineWidth + tagWidth > screenWidth) {
                            llTagRow.removeView(tvTag); // 移除刚添加的标签
                            llFilterContainer.addView(llTagRow); // 添加满行

                            // 新建空行
                            llTagRow = new LinearLayout(getContext());
                            llTagRow.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            ));
                            llTagRow.setOrientation(LinearLayout.HORIZONTAL);
                            llTagRow.setPadding(dp2px(10), 0, dp2px(10), 0);

                            // 重新添加当前标签到新行
                            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            tagParams.setMargins(0, 0, dp2px(10), dp2px(10));
                            tvTag.setLayoutParams(tagParams);
                            llTagRow.addView(tvTag);

                            currentLineWidth = tagWidth; // 新行宽度初始化
                        } else {
                            // 正常情况设置间距
                            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            tagParams.setMargins(0, 0, dp2px(10), dp2px(10));
                            tvTag.setLayoutParams(tagParams);
                            currentLineWidth += tagWidth;
                        }

                        // 标签点击事件
                        tvTag.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onItemClick(subTab);
                            }
                        });
                    }
                    llFilterContainer.addView(llTagRow);
                }
            }
            return view;
        }
    }

    /**
     * dp转px
     */
    private int dp2px(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 统一Toast提示
     */
    private void showToast(String msg) {
        if (getActivity() == null || TextUtils.isEmpty(msg)) return;

        TextView textView = new TextView(getActivity());
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(Color.WHITE);
        textView.setPadding(dp2px(20), dp2px(12), dp2px(20), dp2px(12));
        textView.setGravity(Gravity.CENTER);

        // 代码生成圆角黑色背景，替代setRadius
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(0xCC000000);
        drawable.setCornerRadius(dp2px(4));
        textView.setBackground(drawable);

        Toast toast = new Toast(getActivity());
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 取消所有未完成的请求
        okHttpClient.dispatcher().cancelAll();
    }
}
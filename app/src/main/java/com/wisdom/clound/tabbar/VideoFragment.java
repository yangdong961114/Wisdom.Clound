package com.wisdom.clound.tabbar;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.wisdom.clound.Bean.TabBean;
import com.wisdom.clound.Bean.VideoBean;
import com.wisdom.clound.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.wisdom.clound.utils.HttpUtils;

public class VideoFragment extends Fragment {
    // 控件声明
    private LinearLayout llTabContainer; // Tab容器
    private RecyclerView rvVideoList;    // 视频列表
    private SwipeRefreshLayout srlRefresh; // 下拉刷新

    // 数据变量
    private List<TabBean> tabList = new ArrayList<>(); // Tab列表
    private List<VideoBean> videoList = new ArrayList<>(); // 视频列表
    private TextView selectedTab; // 当前选中的Tab
    private int currentTabId = 1; // 默认选中第一个Tab
    private int currentPage = 1;  // 默认页码
    private boolean isLoading = false; // 加载状态标记
    private boolean hasMoreData = true; // 是否有更多数据

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        initViews(view); // 初始化控件
        loadTabData();   // 加载Tab数据
        initVideoList(); // 初始化视频列表
        return view;
    }

    /**
     * 初始化所有控件
     */
    private void initViews(View view) {
        // Tab容器（滑动选项卡）
        llTabContainer = view.findViewById(R.id.ll_tab_container);
        // 视频列表+下拉刷新
        rvVideoList = view.findViewById(R.id.rv_video_list);
        srlRefresh = view.findViewById(R.id.srl_refresh);
    }

    /**
     * 加载Tab数据（对接指定接口）- 修复：绑定Fragment生命周期
     */
    private void loadTabData() {
        String tabUrl = "/VideoFragment/GetTabs";
        Log.d("VideoTab", "请求地址：" + tabUrl);

        // 修复点1：调用带生命周期的HttpUtils.get重载方法，传入getLifecycle()
        HttpUtils.get(tabUrl, null, getLifecycle(), new HttpUtils.HttpCallback() {
            @Override
            public void onSuccess(String result) {
                // 双重校验：Fragment是否活跃 + Context是否有效
                if (!isAdded() || getContext() == null) {
                    return;
                }
                Log.d("VideoTab", "返回数据：" + result);
                parseTabData(result); // 解析Tab数据
                renderTabLayout();    // 渲染滑动Tab
            }

            @Override
            public void onFailed(String error) {
                Log.e("VideoTab", "请求失败：" + error);
                // 修复点2：Toast改用getContext()并判空
                showToast("Tab加载失败：" + error);
                // 模拟数据兜底（接口失败时用）
                mockTabData();
                renderTabLayout();
            }
        });
    }

    /**
     * 解析Tab接口数据（适配指定返回格式）
     */
    private void parseTabData(String json) {
        // 校验Context有效性
        if (getContext() == null) return;

        tabList.clear();
        if (json == null || json.trim().isEmpty()) return;

        try {
            JSONObject rootObj = new JSONObject(json);
            int code = rootObj.getInt("code");
            if (code != 200) {
                showToast("Tab接口返回失败");
                return;
            }

            JSONArray dataArray = rootObj.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject tabObj = dataArray.getJSONObject(i);
                int tabId = tabObj.optInt("Id", 0);
                String tabName = tabObj.optString("NavigationKey", "未知分类");
                if (tabId > 0) { // 过滤无效Tab
                    tabList.add(new TabBean(tabId, tabName));
                }
            }
        } catch (JSONException e) {
            Log.e("VideoTab", "解析失败", e);
            showToast("Tab解析失败");
        }
    }

    /**
     * 模拟Tab数据（接口失败时兜底）
     */
    private void mockTabData() {
        tabList.clear();
//        tabList.add(new TabBean(1, "自营区"));
//        tabList.add(new TabBean(2, "热销区"));
//        tabList.add(new TabBean(3, "爆款区"));
//        tabList.add(new TabBean(4, "新品区"));
    }

    /**
     * 渲染滑动Tab选项卡 - 核心修复：所有Context操作先判空
     */
    private void renderTabLayout() {
        // 修复点3：先校验Fragment状态和Context有效性
        if (!isAdded() || getContext() == null || llTabContainer == null) {
            return;
        }

        llTabContainer.removeAllViews();
        for (int i = 0; i < tabList.size(); i++) {
            TabBean tab = tabList.get(i);
            // 修复点4：改用getContext()创建TextView（避免getActivity()为空）
            TextView tvTab = new TextView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(15, 5, 15, 5); // Tab间距
            tvTab.setLayoutParams(params);
            tvTab.setText(tab.getName());
            tvTab.setTextColor(Color.WHITE);
            tvTab.setTextSize(18);
            tvTab.setPadding(10, 12, 10, 8); // 扩大点击区域

            // 默认选中第一个Tab
            if (i == 0) {
                // 修复点5：getResources()改用getContext().getResources()
                tvTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                        getContext().getResources().getDrawable(R.drawable.shape_tab_underline));
                tvTab.setCompoundDrawablePadding(8);
                selectedTab = tvTab;
                currentTabId = tab.getId();
                loadVideoList(currentTabId, 1, true); // 加载第一个Tab的视频
            }

            // Tab点击事件（切换+居中滚动）
            tvTab.setOnClickListener(v -> {
                // 取消上一个Tab的选中样式
                if (selectedTab != null) {
                    selectedTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
                // 设置当前Tab选中样式
                tvTab.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                        getContext().getResources().getDrawable(R.drawable.shape_tab_underline));
                tvTab.setCompoundDrawablePadding(8);
                selectedTab = tvTab;

                // 切换Tab并刷新列表
                currentTabId = tab.getId();
                currentPage = 1;
                loadVideoList(currentTabId, currentPage, true);

                HorizontalScrollView hsvTab = (HorizontalScrollView) llTabContainer.getParent();
                if (hsvTab != null) { // 判空避免崩溃
                    int scrollX = tvTab.getLeft() - (hsvTab.getWidth() / 2 - tvTab.getWidth() / 2);
                    hsvTab.smoothScrollTo(scrollX, 0);
                }

                showToast("切换到：" + tab.getName());
            });

            llTabContainer.addView(tvTab);
        }
    }

    /**
     * 初始化视频列表（一行两列网格布局）
     */
    private void initVideoList() {
        // 校验Context有效性
        if (getContext() == null) return;

        // 网格布局：2列（和首页商品一致）
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvVideoList.setLayoutManager(layoutManager);

        // 设置适配器
        com.wisdom.clound.tabbar.VideoFragment.VideoAdapter adapter = new com.wisdom.clound.tabbar.VideoFragment.VideoAdapter(videoList);
        rvVideoList.setAdapter(adapter);

        // 下拉刷新配置
        srlRefresh.setColorSchemeColors(Color.RED);
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
                    loadVideoList(currentTabId, currentPage, false);
                }
            }
        });
    }

    /**
     * 加载视频列表（对接商品接口，types传TabId）- 修复：绑定生命周期
     */
    private void loadVideoList(int tabId, int page, boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        // 商品列表接口，types传Tab的Id
        String videoUrl = "/HomeFragment/GetGoodsList?typesId=" + tabId + "&page=" + page;
        Log.d("VideoList", "请求地址：" + videoUrl);

        // 显示加载状态
        if (isRefresh) {
            srlRefresh.setRefreshing(true);
        } else {
            // 添加加载占位符
            videoList.add(new VideoBean(-1, "加载中...", "", 0, "00:00"));
            if (rvVideoList.getAdapter() != null) {
                rvVideoList.getAdapter().notifyItemInserted(videoList.size() - 1);
            }
        }

        // 修复点6：调用带生命周期的HttpUtils.get重载方法
        HttpUtils.get(videoUrl, null, getLifecycle(), new HttpUtils.HttpCallback() {
            @Override
            public void onSuccess(String result) {
                // 校验Fragment状态
                if (!isAdded() || getContext() == null) {
                    isLoading = false;
                    srlRefresh.setRefreshing(false);
                    return;
                }

                Log.d("VideoList", "返回数据：" + result);
                try {
                    List<VideoBean> newList = parseVideoData(result);
                    if (isRefresh) {
                        videoList.clear();
                        hasMoreData = true;
                    } else {
                        // 移除加载占位符
                        if (!videoList.isEmpty() && videoList.get(videoList.size() - 1).getId() == -1) {
                            videoList.remove(videoList.size() - 1);
                        }
                    }

                    if (newList.size() > 0) {
                        videoList.addAll(newList);
                    } else {
                        hasMoreData = false;
                        if (!isRefresh) {
                            showToast("已加载全部视频");
                        }
                    }
                    if (rvVideoList.getAdapter() != null) {
                        rvVideoList.getAdapter().notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.e("VideoList", "解析失败", e);
                    showToast("视频解析失败");
                } finally {
                    isLoading = false;
                    srlRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onFailed(String error) {
                Log.e("VideoList", "请求失败：" + error);
                showToast("视频加载失败：" + error);
                isLoading = false;
                srlRefresh.setRefreshing(false);

                // 移除加载占位符
                if (!isRefresh && !videoList.isEmpty() && videoList.get(videoList.size() - 1).getId() == -1) {
                    videoList.remove(videoList.size() - 1);
                    if (rvVideoList.getAdapter() != null) {
                        rvVideoList.getAdapter().notifyItemRemoved(videoList.size());
                    }
                }
            }
        });
    }

    /**
     * 解析视频数据（适配商品接口返回格式）
     */
    private List<VideoBean> parseVideoData(String json) {
        List<VideoBean> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;

        try {
            JSONObject rootObj = new JSONObject(json);
            if (rootObj.getInt("code") == 200) {
                JSONArray dataArray = rootObj.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject itemObj = dataArray.getJSONObject(i);
                    // 适配商品接口字段，映射为视频字段
                    int id = itemObj.optInt("Id", 0);
                    String title = itemObj.optString("GoodsName", "未知视频");
                    String coverUrl = itemObj.optString("GoodsAvatar", "");
                    int playCount = itemObj.optInt("GoodsWallet", 0); // 复用商品字段
                    String duration = itemObj.optString("GoodsContent", "00:00"); // 复用商品字段

                    if (id > 0) {
                        list.add(new VideoBean(id, title, coverUrl, playCount, duration));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("VideoList", "JSON解析失败", e);
        }
        return list;
    }

    /**
     * 视频列表适配器（一行两列+圆角适配）
     */
    private class VideoAdapter extends RecyclerView.Adapter<VideoFragment.VideoAdapter.VideoViewHolder> {
        private List<VideoBean> mList;

        public VideoAdapter(List<VideoBean> list) {
            this.mList = list;
        }

        @NonNull
        @Override
        public VideoFragment.VideoAdapter.VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 修复点7：用parent.getContext()创建View（更安全）
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video, parent, false);

            // 强制2列均分宽度（和首页商品一致）
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = (parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight() - 20) / 2;
            view.setLayoutParams(params);

            return new VideoFragment.VideoAdapter.VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoFragment.VideoAdapter.VideoViewHolder holder, int position) {
            VideoBean bean = mList.get(position);
            // 判空避免崩溃
            if (holder.ivCover != null) {
                loadImage(bean.getCoverUrl(), holder.ivCover);
                // 延迟设置圆角裁剪（避免初始化异常）
                holder.ivCover.post(() -> holder.ivCover.setClipToOutline(true));
            }
            if (holder.tvTitle != null) {
                holder.tvTitle.setText(bean.getTitle());
            }
            if (holder.tvPlayCount != null) {
                holder.tvPlayCount.setText(bean.getPlayCount() + "次播放");
            }
            if (holder.tvDuration != null) {
                holder.tvDuration.setText(bean.getDuration());
            }
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        /**
         * ViewHolder（绑定item布局控件）
         */
        class VideoViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;      // 视频封面（圆角）
            TextView tvTitle;       // 视频标题
            TextView tvPlayCount;   // 播放量
            TextView tvDuration;    // 视频时长

            public VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_video_cover);
                tvTitle = itemView.findViewById(R.id.tv_video_title);
                tvPlayCount = itemView.findViewById(R.id.tv_play_count);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
        }
    }

    /**
     * 加载图片（网络/本地容错）- 修复：getActivity()改为getContext()并判空
     */
    private void loadImage(String url, ImageView iv) {
        new Thread(() -> {
            try {
                if (url == null || url.trim().isEmpty()) {
                    // 修复点8：UI操作前校验Context
                    if (getContext() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (iv != null) iv.setImageResource(R.mipmap.ic_launcher);
                        });
                    }
                    return;
                }
                java.net.URL imageUrl = new java.net.URL(url);
                java.io.InputStream is = imageUrl.openStream();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                // 校验Context有效性
                if (getContext() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (iv != null) iv.setImageBitmap(bitmap);
                    });
                }
                is.close();
            } catch (Exception e) {
                Log.e("ImageLoad", "加载失败", e);
                // 校验Context有效性
                if (getContext() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (iv != null) iv.setImageResource(R.mipmap.ic_launcher);
                    });
                }
            }
        }).start();
    }

    /**
     * ********** 统一封装：黑色透明Toast + 居中 + 无图标 **********
     */
    private void showToast(String msg) {
        if (!isAdded() || getContext() == null || TextUtils.isEmpty(msg)) {
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
}
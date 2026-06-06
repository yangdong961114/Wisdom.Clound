package com.wisdom.clound.ui.video;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;
import com.wisdom.clound.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoPlayActivity extends AppCompatActivity implements View.OnClickListener, GestureDetector.OnGestureListener {
    private static final String TAG = "VideoPlayActivity";
    private static final String API_URL = "https://api.rzkj.qyqd123.cn/Android/VideoFragment/GetQmVideoDetail?videoId=";

    // UI控件
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private TextView tvEpisode;
    private TextView tvSelectEpisode;
    private TextView tvIntroCollapse;
    private FrameLayout flMask;
    private LinearLayout llEpisodePopup;
    private TextView tvPopupTitle;
    private TextView tvIntroExpand;
    private RecyclerView rvEpisodeList;

    // 数据变量
    private String videoId;
    private VideoDetailResponse videoDetailResponse;
    private List<VideoDetailsBean> episodeList = new ArrayList<>();
    private int currentEpisodeIndex = 0;
    private EpisodeAdapter episodeAdapter;

    // 手势检测（单击暂停、上滑下一集、下滑上一集）
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        Intent intent = getIntent();
        if (intent != null) {
            videoId = intent.getStringExtra("videoId");
        }
        gestureDetector = new GestureDetector(this, this);
        initView();
        initPlayer();
        requestVideoDetail();
    }

    private void initView() {
        playerView = findViewById(R.id.player_view);

        // ==========关键：播放器触摸全部交给手势，弹窗打开屏蔽手势==========
        playerView.setOnTouchListener((v, event) -> {
            // 选集弹窗打开，不处理手势
            if (flMask.getVisibility() == View.VISIBLE) {
                return false;
            }
            return gestureDetector.onTouchEvent(event);
        });

        findViewById(R.id.iv_back).setOnClickListener(this);
        tvEpisode = findViewById(R.id.tv_episode);
        LinearLayout llSelectEpisode = findViewById(R.id.ll_select_episode);
        llSelectEpisode.setOnClickListener(this);
        tvSelectEpisode = findViewById(R.id.tv_select_episode);
        tvIntroCollapse = findViewById(R.id.tv_intro_collapse);
        tvIntroCollapse.setOnClickListener(this);

        flMask = findViewById(R.id.fl_mask);
        llEpisodePopup = findViewById(R.id.ll_episode_popup);
        findViewById(R.id.iv_close_popup).setOnClickListener(this);
        tvPopupTitle = findViewById(R.id.tv_popup_title);
        tvIntroExpand = findViewById(R.id.tv_intro_expand);

        // 设置弹窗高度80%屏幕
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenHeight = dm.heightPixels;
        int popupHeight = (int) (screenHeight * 0.8);
        ViewGroup.LayoutParams params = llEpisodePopup.getLayoutParams();
        params.height = popupHeight;
        llEpisodePopup.setLayoutParams(params);

        rvEpisodeList = findViewById(R.id.rv_episode_list);
        rvEpisodeList.setLayoutManager(new GridLayoutManager(this, 8));
        episodeAdapter = new EpisodeAdapter();
        rvEpisodeList.setAdapter(episodeAdapter);
    }

    private void initPlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        // 播放完毕自动下一集，最后一集停止
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    if (currentEpisodeIndex < episodeList.size() - 1) {
                        playEpisode(currentEpisodeIndex + 1);
                    } else {
                        exoPlayer.stop();
                        Toast.makeText(VideoPlayActivity.this, "已播放至最后一集", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void requestVideoDetail() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(API_URL + videoId)
                        .get()
                        .build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    Log.d(TAG, "API返回数据：" + json);
                    Gson gson = new Gson();
                    videoDetailResponse = gson.fromJson(json, VideoDetailResponse.class);

                    runOnUiThread(() -> {
                        if (videoDetailResponse.getCode() == 200 && videoDetailResponse.getData() != null) {
                            VideoInfoBean videoInfo = videoDetailResponse.getData().getVideoInfo();
                            episodeList = videoDetailResponse.getData().getVideoDetailses();
                            updateUI(videoInfo);
                            if (!episodeList.isEmpty()) playEpisode(0);
                        } else {
                            Toast.makeText(VideoPlayActivity.this, "获取视频详情失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(VideoPlayActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateUI(VideoInfoBean videoInfo) {
        tvSelectEpisode.setText("选集 | 全" + videoInfo.getTotalEpisodeNum() + "集");
        tvPopupTitle.setText(videoInfo.getTitle());
        String intro = videoInfo.getIntro();
        if (intro.length() > 50) {
            tvIntroCollapse.setText(intro.substring(0, 50) + "... 展开");
        } else {
            tvIntroCollapse.setText(intro);
        }
        tvIntroExpand.setText(intro);
        tvIntroExpand.setTextSize(18);
        episodeAdapter.notifyDataSetChanged();
    }

    // 切换集数
    private void playEpisode(int index) {
        if (index < 0 || index >= episodeList.size()) return;

        currentEpisodeIndex = index;
        VideoDetailsBean episode = episodeList.get(index);
        tvEpisode.setText("第" + episode.getSort() + "集");

        MediaItem mediaItem = MediaItem.fromUri(episode.getVideoHurl());
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        flMask.setVisibility(View.GONE);
        episodeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_back) {
            finish();
        } else if (id == R.id.ll_select_episode || id == R.id.tv_intro_collapse) {
            flMask.setVisibility(View.VISIBLE);
        } else if (id == R.id.iv_close_popup) {
            flMask.setVisibility(View.GONE);
        }
    }

    // 【作废】不再使用Activity的onTouchEvent
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    // 单击画面 暂停/播放
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
        } else {
            exoPlayer.play();
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }
    @Override public void onShowPress(MotionEvent e) {}
    @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {return false;}
    @Override public void onLongPress(MotionEvent e) {}

    // 上滑=下一集 下滑=上一集
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float diffY = e2.getY() - e1.getY();
        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_THRESHOLD) {
            if (diffY < 0) {
                // 上滑切换下一集
                if (currentEpisodeIndex < episodeList.size() - 1) {
                    playEpisode(currentEpisodeIndex + 1);
                } else {
                    Toast.makeText(this, "已经是最后一集啦！", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 下滑切换上一集
                if (currentEpisodeIndex > 0) {
                    playEpisode(currentEpisodeIndex - 1);
                } else {
                    Toast.makeText(this, "已经是第一集啦！", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return true;
    }

    // 选集适配器
    private class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {
        @NonNull
        @Override
        public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tvEpisode = new TextView(VideoPlayActivity.this);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            tvEpisode.setLayoutParams(params);
            tvEpisode.setPadding(20, 12, 20, 12);
            tvEpisode.setTextSize(20);
            tvEpisode.setTextColor(Color.WHITE);
            tvEpisode.setGravity(Gravity.CENTER);
            tvEpisode.setBackgroundResource(R.drawable.shape_episode_unselected);
            return new EpisodeViewHolder(tvEpisode);
        }

        @Override
        public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
            VideoDetailsBean bean = episodeList.get(position);
            holder.tvEpisode.setText(bean.getSort());

            if (position == currentEpisodeIndex) {
                holder.tvEpisode.setTextColor(Color.WHITE);
                holder.tvEpisode.setBackgroundResource(R.drawable.shape_episode_selected);
            } else {
                holder.tvEpisode.setTextColor(Color.WHITE);
                holder.tvEpisode.setBackgroundResource(R.drawable.shape_episode_unselected);
            }

            holder.tvEpisode.setOnClickListener(v -> playEpisode(position));
        }

        @Override
        public int getItemCount() {
            return episodeList.size();
        }

        class EpisodeViewHolder extends RecyclerView.ViewHolder {
            TextView tvEpisode;
            public EpisodeViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEpisode = (TextView) itemView;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) exoPlayer.play();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    // JSON实体
    public static class VideoDetailResponse {
        private int code;
        private int count;
        private String msg;
        private VideoDetailData data;
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public VideoDetailData getData() { return data; }
        public void setData(VideoDetailData data) { this.data = data; }
    }

    public static class VideoDetailData {
        private VideoInfoBean VideoInfo;
        private List<VideoDetailsBean> VideoDetailses;
        public VideoInfoBean getVideoInfo() { return VideoInfo; }
        public void setVideoInfo(VideoInfoBean VideoInfo) { this.VideoInfo = VideoInfo; }
        public List<VideoDetailsBean> getVideoDetailses() { return VideoDetailses; }
        public void setVideoDetailses(List<VideoDetailsBean> VideoDetailses) { this.VideoDetailses = VideoDetailses; }
    }

    public static class VideoInfoBean {
        private String Intro;
        private String Title;
        private String TotalEpisodeNum;
        public String getIntro() { return Intro; }
        public String getTitle() { return Title; }
        public String getTotalEpisodeNum() { return TotalEpisodeNum; }
    }

    public static class VideoDetailsBean {
        private String Sort;
        private String VideoHurl;
        public String getSort() { return Sort; }
        public String getVideoHurl() { return VideoHurl; }
    }
}
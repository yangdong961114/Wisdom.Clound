package com.wisdom.clound.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.wisdom.clound.Bean.ShareListBean;
import com.wisdom.clound.R;

import java.util.List;

public class ShareListAdapter extends RecyclerView.Adapter<ShareListAdapter.ViewHolder> {
    private Context mContext;
    private List<ShareListBean.DataBean> mDataList;
    private OnTransferClickListener listener;

    public ShareListAdapter(Context context, List<ShareListBean.DataBean> dataList) {
        this.mContext = context;
        this.mDataList = dataList;
    }

    public void setOnTransferClickListener(OnTransferClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_share_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShareListBean.DataBean bean = mDataList.get(position);

        String userName = bean.getUserName() == null || bean.getUserName().isEmpty()
                ? "未知用户" : bean.getUserName();
        holder.tvUserName.setText(userName);
        holder.tvLoginName.setText("登录名：" + (bean.getUserLoginName() == null ? "无" : bean.getUserLoginName()));
        // 核心修改：绑定推荐人信息
        holder.tvShareUserName.setText(bean.getShareUserName());
        holder.tvRegisterDate.setText("注册时间：" + bean.getStrRegisterDate());
        holder.tvPlatform.setText("状态：" + bean.getStrStatus());

        String avatarUrl = bean.getUserAvatar() == null || bean.getUserAvatar().isEmpty()
                ? "https://api.rzkj.qyqd123.cn/Content/Avatar/youke.png" : bean.getUserAvatar();
        Glide.with(mContext)
                .load(avatarUrl)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(holder.ivAvatar);

        holder.btnTransfer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransferClick(bean);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    public void updateData(List<ShareListBean.DataBean> newData, boolean isLoadMore) {
        if (isLoadMore) {
            mDataList.addAll(newData);
        } else {
            mDataList.clear();
            mDataList.addAll(newData);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName,tvLoginName,tvShareUserName,tvRegisterDate,tvPlatform;
        Button btnTransfer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvLoginName = itemView.findViewById(R.id.tv_login_name);
            tvShareUserName = itemView.findViewById(R.id.tv_share_user_name); // 新增控件绑定
            tvRegisterDate = itemView.findViewById(R.id.tv_register_date);
            tvPlatform = itemView.findViewById(R.id.tv_platform);
            btnTransfer = itemView.findViewById(R.id.btn_transfer);
        }
    }

    public interface OnTransferClickListener {
        void onTransferClick(ShareListBean.DataBean bean);
    }
}
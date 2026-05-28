package com.wisdom.clound.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wisdom.clound.Bean.ActivateResponse;
import com.wisdom.clound.R;
import com.wisdom.clound.ui.activate.ActivateIndexActivity;

import java.util.List;

public class ActivateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_UNUSED = 0;
    private static final int TYPE_USED = 1;

    private final Context context;
    private final List<ActivateResponse.UsersBean> list;
    // 激活回调接口
    private OnActivateListener onActivateListener;

    // 构造方法
    public ActivateAdapter(Context context, List<ActivateResponse.UsersBean> list) {
        this.context = context;
        this.list = list;
    }

    // 设置回调监听
    public void setOnActivateListener(OnActivateListener onActivateListener) {
        this.onActivateListener = onActivateListener;
    }

    // 回调接口定义
    public interface OnActivateListener {
        void onActivate(int userActivateId, String userLoginName);
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).getIsStatus() == 0 ? TYPE_UNUSED : TYPE_USED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_UNUSED) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_activate_unused, parent, false);
            return new UnUsedHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_activate_used, parent, false);
            return new UsedHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ActivateResponse.UsersBean bean = list.get(position);
        if (holder instanceof UnUsedHolder) {
            UnUsedHolder unUsedHolder = (UnUsedHolder) holder;
            unUsedHolder.tvCode.setText(bean.getActivateCode());

            // 立即激活按钮点击
            unUsedHolder.btnActivate.setOnClickListener(v -> {
                String account = unUsedHolder.etAccount.getText().toString().trim();
                if (TextUtils.isEmpty(account)) {
                    // 调用Activity的Toast提示
                    if (context instanceof ActivateIndexActivity) {
                        ((ActivateIndexActivity) context).showToast("请输入激活账号");
                    }
                    return;
                }
                // 回调：传递激活码ID + 输入的账号
                if (onActivateListener != null) {
                    onActivateListener.onActivate(bean.getId(), account);
                }
            });
        } else {
            UsedHolder usedHolder = (UsedHolder) holder;
            usedHolder.tvCode.setText(bean.getActivateCode());
            usedHolder.tvInfo.setText("账号：" + bean.getUserLoginName() + "  昵称：" + bean.getUserName());
            usedHolder.tvTime.setText("激活时间：" + bean.getCreateDate());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // 未激活
    static class UnUsedHolder extends RecyclerView.ViewHolder {
        TextView tvCode;
        EditText etAccount;
        Button btnActivate;

        public UnUsedHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_code);
            etAccount = itemView.findViewById(R.id.et_account);
            btnActivate = itemView.findViewById(R.id.btn_activate);
        }
    }

    // 已激活
    static class UsedHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvInfo, tvTime;

        public UsedHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_code);
            tvInfo = itemView.findViewById(R.id.tv_info);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
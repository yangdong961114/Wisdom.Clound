package com.wisdom.clound.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wisdom.clound.R;
import com.wisdom.clound.Bean.UserAddress;
import java.util.List;

/**
 * 收货地址列表适配器
 */
public class UserAddressAdapter extends RecyclerView.Adapter<UserAddressAdapter.AddressViewHolder> {

    private Context mContext;
    private List<UserAddress> mAddressList;
    private OnAddressOperateListener mOperateListener;

    // 操作事件回调
    public interface OnAddressOperateListener {
        void onEdit(UserAddress address);  // 修改地址
        void onDelete(UserAddress address); // 删除地址
    }

    public UserAddressAdapter(Context context, List<UserAddress> addressList, OnAddressOperateListener operateListener) {
        this.mContext = context;
        this.mAddressList = addressList;
        this.mOperateListener = operateListener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_user_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        UserAddress address = mAddressList.get(position);
        // 绑定数据
        holder.tvUserName.setText(address.getUserName());
        holder.tvUserPhone.setText(address.getUserPhone());
        holder.tvAddress.setText(address.getCityDesc() + " " + address.getAddress());

        // 默认地址标签显示
        if (address.getIsDefault()) {
            holder.tvDefaultTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvDefaultTag.setVisibility(View.GONE);
        }

        // 修改按钮点击
        holder.btnEdit.setOnClickListener(v -> {
            if (mOperateListener != null) {
                mOperateListener.onEdit(address);
            }
        });

        // 删除按钮点击
        holder.btnDelete.setOnClickListener(v -> {
            if (mOperateListener != null) {
                mOperateListener.onDelete(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAddressList == null ? 0 : mAddressList.size();
    }

    // 视图持有者
    static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvDefaultTag;
        TextView tvUserName;
        TextView tvUserPhone;
        TextView tvAddress;
        Button btnEdit;
        Button btnDelete;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDefaultTag = itemView.findViewById(R.id.tv_default_tag);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserPhone = itemView.findViewById(R.id.tv_user_phone);
            tvAddress = itemView.findViewById(R.id.tv_address);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    // 更新列表数据
    public void updateData(List<UserAddress> newList) {
        this.mAddressList = newList;
        notifyDataSetChanged();
    }
}
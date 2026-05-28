package com.wisdom.clound.ui.goods;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.wisdom.clound.R;
import com.wisdom.clound.utils.SPUtils;

public class OrderIndexActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // 选项卡标题 + 对应ID
    private final String[] tabTitles = {"待付款", "已付款", "已发货", "已收货"};
    private final int[] typeIds = {1, 2, 3, 4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_index);

        // 校验登录
        if (SPUtils.getUserId(this) == null || SPUtils.getUserId(this).isEmpty()) {
            // 替换为自定义Toast
            showToast("请先登录");
            finish();
            return;
        }

        initView();
        initViewPager();
    }

    // 初始化控件
    private void initView() {
        btnBack = findViewById(R.id.btn_back);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());
    }

    // 初始化ViewPager + 选项卡联动
    private void initViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // 每个Fragment携带对应typeId
                return OrderListFragment.newInstance(typeIds[position]);
            }

            @Override
            public int getItemCount() {
                return tabTitles.length;
            }
        });

        // TabLayout 绑定 ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabTitles[position]);
        }).attach();

        // 默认选中 待付款(1)
        viewPager.setCurrentItem(0, false);
    }

    // ==================== 自定义Toast（完全按照你的要求） ====================
    private void showToast(String msg) {
        if (msg == null || msg.isEmpty()) return;
        TextView textView = new TextView(this);
        textView.setText(msg);
        textView.setTextSize(14);
        textView.setTextColor(0xFFFFFFFF);
        textView.setBackgroundColor(0xCC000000);
        textView.setPadding(50, 25, 50, 25);
        textView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(this);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
package com.wisdom.clound;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.wisdom.clound.adapter.ViewPagerAdapter;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 绑定适配器
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 禁止ViewPager滑动（可选，保留则左右滑动切换Tab）
        // viewPager.setUserInputEnabled(false);

        // Tab点击切换页面
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0);
            } else if (itemId == R.id.nav_short) {
                viewPager.setCurrentItem(1);
            } else if (itemId == R.id.nav_cart) {
                viewPager.setCurrentItem(2);
            } else if (itemId == R.id.nav_main) {
                viewPager.setCurrentItem(3);
            }
            return true;
        });

        // 页面滑动切换Tab
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNavigation.getMenu().getItem(position).setChecked(true);
            }
        });

        // 新增：处理登录页跳转过来的参数（首次创建时）
        handleJumpIntent(getIntent());
    }

    // 新增：处理登录跳转的核心方法
    private void handleJumpIntent(Intent intent) {
        if (intent != null && intent.hasExtra("target_fragment")) {
            String target = intent.getStringExtra("target_fragment");
            // 匹配登录页传递的"mine"标识，切换到我的页面（position=3）
            if ("mine".equals(target)) {
                viewPager.setCurrentItem(3); // 对应nav_mine的位置
                // 清空参数，避免Activity重建时重复处理
                intent.removeExtra("target_fragment");
            }
            if ("switch_to_cart".equals(target)) {
                viewPager.setCurrentItem(2); // 对应nav_mine的位置
                // 清空参数，避免Activity重建时重复处理
                intent.removeExtra("target_fragment");
            }
        }
    }

    // 新增：处理Activity复用的场景（比如MainActivity已打开，登录后跳转）
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 更新当前Intent，避免后续获取到旧参数
        setIntent(intent);
        // 处理跳转逻辑
        handleJumpIntent(intent);
    }
}
package cn.edu.gzhu.amap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * 路线收藏/历史页面
 */
public class RouteHistoryActivity extends AppCompatActivity implements RouteHistoryAdapter.OnRouteInteractionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ImageButton globalBackButton, navMapView, navRouteRecommendation, navRouteHistory;

    // 浮动操作菜单
    private CardView routeActionMenu;
    private TextView actionRename, actionDelete;
    private RouteRecord currentRoute;
    private int currentPosition;

    private List<RouteRecord> routeList = new ArrayList<>();
    private RouteHistoryAdapter adapter;
    private RouteStorageHelper storageHelper;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_route_history);

        initViews();
        initListeners();
        setSystemBarPadding();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoutes();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.route_history_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        globalBackButton = findViewById(R.id.global_back_button);
        navMapView = findViewById(R.id.nav_map_view);
        navRouteRecommendation = findViewById(R.id.nav_route_recommendation);
        navRouteHistory = findViewById(R.id.nav_route_history);

        // 浮动操作菜单
        routeActionMenu = findViewById(R.id.route_action_menu);
        actionRename = findViewById(R.id.action_rename);
        actionDelete = findViewById(R.id.action_delete);

        storageHelper = new RouteStorageHelper(this);
        adapter = new RouteHistoryAdapter(routeList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void initListeners() {
        globalBackButton.setOnClickListener(v -> finish());
        navMapView.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        navRouteRecommendation.setOnClickListener(v -> {
            startActivity(new Intent(this, RouteRecommendationActivity.class));
            finish();
        });
        navRouteHistory.setOnClickListener(v -> Toast.makeText(this, "已在路线收藏页面", Toast.LENGTH_SHORT).show());

        // 操作菜单按钮事件
        actionRename.setOnClickListener(v -> {
            hideActionMenu();
            showRenameDialog(currentRoute, currentPosition);
        });

        actionDelete.setOnClickListener(v -> {
            hideActionMenu();
            showDeleteDialog(currentRoute, currentPosition);
        });

        // 点击空白区域隐藏菜单
        findViewById(R.id.route_history_root).setOnClickListener(v -> hideActionMenu());
    }

    private void loadRoutes() {
        routeList.clear();
        routeList.addAll(storageHelper.getAllRoutes());
        adapter.notifyDataSetChanged();

        // 显示空状态或列表
        if (routeList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setSystemBarPadding() {
        View header = findViewById(R.id.header);

        // 保存原始的顶部padding，避免重复累加
        final int originalTopPadding = header.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.route_history_root), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            // 使用原始padding + 系统栏高度，而不是累加
            header.setPadding(header.getPaddingLeft(), originalTopPadding + top,
                    header.getPaddingRight(), header.getPaddingBottom());

            return insets;
        });
    }

    @Override
    public void onRouteClick(RouteRecord route) {
        // 先隐藏菜单
        hideActionMenu();
        // 跳转到路径推荐页面并加载路线
        Intent intent = new Intent(this, RouteRecommendationActivity.class);
        intent.putExtra("route_data", gson.toJson(route));
        startActivity(intent);
    }

    @Override
    public void onMoreClick(View anchor, RouteRecord route, int position) {
        // 保存当前操作的路线信息
        currentRoute = route;
        currentPosition = position;

        // 如果菜单已显示则隐藏
        if (routeActionMenu.getVisibility() == View.VISIBLE) {
            routeActionMenu.setVisibility(View.GONE);
            return;
        }

        // 获取内容区域的引用
        View contentArea = findViewById(R.id.content_area);

        // 计算按钮相对于内容区域的位置
        int[] anchorLocation = new int[2];
        int[] contentLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);
        contentArea.getLocationOnScreen(contentLocation);

        // 先显示菜单（让系统测量尺寸）
        routeActionMenu.setVisibility(View.VISIBLE);

        // 强制测量菜单尺寸
        routeActionMenu.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        int menuWidth = routeActionMenu.getMeasuredWidth();
        int contentWidth = contentArea.getWidth();
        float margin = 16 * getResources().getDisplayMetrics().density;

        // 菜单右边对齐，留出边距
        float adjustedX = contentWidth - menuWidth - margin;

        // Y坐标：锚点下方
        float adjustedY = anchorLocation[1] - contentLocation[1] + anchor.getHeight();

        routeActionMenu.setTranslationX(adjustedX);
        routeActionMenu.setTranslationY(adjustedY);
    }

    @Override
    public void onFavoriteToggle(RouteRecord route, int position) {
        hideActionMenu();
        storageHelper.toggleFavorite(route.getId());
        route.setFavorite(!route.isFavorite());
        adapter.notifyItemChanged(position);
        Toast.makeText(this, route.isFavorite() ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
    }

    /**
     * 隐藏操作菜单
     */
    private void hideActionMenu() {
        if (routeActionMenu != null && routeActionMenu.getVisibility() == View.VISIBLE) {
            routeActionMenu.setVisibility(View.GONE);
        }
    }

    /**
     * 显示重命名对话框
     */
    private void showRenameDialog(RouteRecord route, int position) {
        EditText input = new EditText(this);
        input.setText(route.getName());
        input.setSelectAllOnFocus(true);
        input.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(this)
                .setTitle("重命名路线")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        storageHelper.renameRoute(route.getId(), newName);
                        route.setName(newName);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "已重命名", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteDialog(RouteRecord route, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除路线")
                .setMessage("确定要删除路线「" + route.getName() + "」吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    storageHelper.deleteRoute(route.getId());
                    routeList.remove(position);
                    adapter.notifyItemRemoved(position);
                    if (routeList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        if (routeActionMenu != null && routeActionMenu.getVisibility() == View.VISIBLE) {
            hideActionMenu();
        } else {
            super.onBackPressed();
        }
    }
}

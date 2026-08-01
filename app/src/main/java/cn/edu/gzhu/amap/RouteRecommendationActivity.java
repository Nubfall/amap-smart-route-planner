package cn.edu.gzhu.amap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import androidx.appcompat.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.FrameLayout;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.edu.gzhu.amap.overlay.BusRouteOverlay;
import cn.edu.gzhu.amap.overlay.DrivingRouteOverlay;
import cn.edu.gzhu.amap.overlay.RideRouteOverlay;
import cn.edu.gzhu.amap.overlay.WalkRouteOverlay;
import cn.edu.gzhu.amap.overlay.RouteOverlay;

import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.core.PoiItem;
import android.animation.ObjectAnimator;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.graphics.Color;
import android.text.TextUtils;
import android.widget.CheckBox;
import com.google.gson.Gson;

public class RouteRecommendationActivity extends AppCompatActivity implements TextWatcher, Inputtips.InputtipsListener,
        AMap.OnMapClickListener, AMap.OnMarkerClickListener, GeocodeSearch.OnGeocodeSearchListener,
        WaypointsAdapter.OnWaypointInteractionListener,
        RouteSearch.OnRouteSearchListener, PoiSearch.OnPoiSearchListener {

    // Constants for route types
    private static final int ROUTE_TYPE_DRIVE = 0;
    private static final int ROUTE_TYPE_WALK = 1;
    private static final int ROUTE_TYPE_RIDE = 2;
    private static final int ROUTE_TYPE_BUS = 3;
    private static final int ROUTE_TYPE_TRUCK = 4;
    private static final int ROUTE_TYPE_EBIKE = 5;

    public static class Waypoint {
        private final String name;
        private final LatLonPoint point;
        private boolean isChecked = true;

        public Waypoint(String name, LatLonPoint point) {
            this.name = name;
            this.point = point;
        }

        public String getName() {
            return name;
        }

        public LatLonPoint getPoint() {
            return point;
        }

        public boolean isChecked() {
            return isChecked;
        }

        public void setChecked(boolean checked) {
            isChecked = checked;
        }
    }

    // Views
    private LinearLayout rootLayout;
    private FrameLayout startPointWrapper;
    private ImageButton globalBackButton, navMapView, navRouteRecommendation, navRouteHistory, selectOnMapButton,
            addWaypointButton,
            viewWaypointsButton, btnSaveRoute;
    private AutoCompleteTextView startPointInput;
    private CardView waypointsMenu, routePlanningControls;
    private RecyclerView waypointRecyclerView;
    private MapView previewMapView;
    private AMap previewMap;
    private ImageButton planDriveButton, planWalkButton, planRideButton, planBusButton, planTruckButton,
            planEbikeButton;
    private CheckBox cbTspOptimize;

    // Services
    private GeocodeSearch geocodeSearch;
    private RouteSearch routeSearch;

    // Data
    private Waypoint currentSelectedWaypoint;
    private ArrayList<Waypoint> waypointsList = new ArrayList<>();
    private WaypointsAdapter waypointsAdapter;
    private List<Tip> currentTipList;
    private boolean isProgrammaticChange = false;
    private ItemTouchHelper itemTouchHelper;

    // Overlays
    private DrivingRouteOverlay drivingRouteOverlay;
    private WalkRouteOverlay walkRouteOverlay;
    private RideRouteOverlay rideRouteOverlay;
    private BusRouteOverlay busRouteOverlay;

    // Multi-segment routing state
    private boolean isMultiSegmentSearch = false;
    private int currentSegmentIndex = 0;
    private List<RouteSearch.FromAndTo> segmentList = new ArrayList<>();
    private List<RouteOverlay> multiRouteOverlays = new ArrayList<>();
    private int currentRouteTypeForMultiSegment = -1;

    // Intelligent Recommendation Views
    private LinearLayout recommendationHeader, recommendationBody;
    private ImageView recommendationToggleArrow;
    private Button btnStartRecommendation, btnClearRecommendation;
    private EditText etSearchRadius;
    private Spinner spinnerSearchCategory, spinnerSearchTarget;
    private boolean isRecommendationExpanded = false;

    // 路线详情面板视图
    private CardView routeDetailsCard;
    private LinearLayout routeDetailsHeader, routeDetailsBody;
    private ImageView routeDetailsToggleArrow;
    private TextView tvRouteMode, tvRouteDistance, tvRouteDuration, tvRouteWaypoints;
    private boolean isRouteDetailsExpanded = false;

    // 多段路径累加数据
    private float totalMultiSegmentDistance = 0;
    private long totalMultiSegmentDuration = 0;

    // Intelligent Recommendation Data
    private Circle bufferCircle;
    private List<Marker> poiMarkers = new ArrayList<>();
    private PoiSearch.Query poiQuery;
    private PoiSearch poiSearch;
    private List<String> spinnerPointNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> poiCategoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private PoiItem currentSelectedPoi;
    private Marker pendingMapClickMarker;

    // 路线收藏相关
    private RouteStorageHelper storageHelper;
    private int lastRouteType = -1;

    // API请求防抖（防止QPS超限，错误码10021）
    private long lastRouteRequestTime = 0;
    private static final long ROUTE_REQUEST_INTERVAL = 1000; // 请求间隔1秒
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_route_recommendation);

        initViews(savedInstanceState);
        initServices();
        initPreviewMap();
        initRecyclerView();
        initListeners();
        setSystemBarPadding();

        // 初始化存储帮助类
        storageHelper = new RouteStorageHelper(this);

        // 检查是否有传入的路线数据
        loadRouteFromIntent();
    }

    private void initViews(Bundle savedInstanceState) {
        rootLayout = findViewById(R.id.route_recommendation_root_layout);
        startPointWrapper = findViewById(R.id.start_point_wrapper);
        globalBackButton = findViewById(R.id.global_back_button);
        navMapView = findViewById(R.id.nav_map_view);
        navRouteRecommendation = findViewById(R.id.nav_route_recommendation);
        navRouteHistory = findViewById(R.id.nav_route_history);
        startPointInput = findViewById(R.id.start_point_input);
        selectOnMapButton = findViewById(R.id.select_on_map_button);
        addWaypointButton = findViewById(R.id.add_waypoint_button);
        viewWaypointsButton = findViewById(R.id.view_waypoints_button);
        btnSaveRoute = findViewById(R.id.btn_save_route);
        waypointsMenu = findViewById(R.id.waypoints_menu);
        waypointRecyclerView = findViewById(R.id.waypoint_recycler_view);
        cbTspOptimize = findViewById(R.id.cb_tsp_optimize);
        previewMapView = findViewById(R.id.preview_map_view);
        previewMapView.onCreate(savedInstanceState);

        routePlanningControls = findViewById(R.id.route_planning_controls);
        planDriveButton = findViewById(R.id.plan_drive_button);
        planWalkButton = findViewById(R.id.plan_walk_button);
        planRideButton = findViewById(R.id.plan_ride_button);
        planBusButton = findViewById(R.id.plan_bus_button);
        planTruckButton = findViewById(R.id.plan_truck_button);
        planEbikeButton = findViewById(R.id.plan_ebike_button);

        Drawable dropdownBg = ContextCompat.getDrawable(this, R.drawable.bg_search_suggestion_dropdown);
        startPointInput.setDropDownBackgroundDrawable(dropdownBg);

        initRecommendationPanel();
        initRouteDetailsPanel();
    }

    private void initRecommendationPanel() {
        recommendationHeader = findViewById(R.id.recommendation_header);
        recommendationBody = findViewById(R.id.recommendation_body);
        recommendationToggleArrow = findViewById(R.id.recommendation_toggle_arrow);
        btnStartRecommendation = findViewById(R.id.btn_start_recommendation);
        btnClearRecommendation = findViewById(R.id.btn_clear_recommendation);
        etSearchRadius = findViewById(R.id.et_search_radius);
        spinnerSearchCategory = findViewById(R.id.spinner_search_category);
        spinnerSearchTarget = findViewById(R.id.spinner_search_target);

        // Toggle Expand/Collapse
        recommendationHeader.setOnClickListener(v -> toggleRecommendationPanel());

        // Start Recommendation
        btnStartRecommendation.setOnClickListener(v -> doIntelligentRecommendation());

        // Clear Recommendation Results
        btnClearRecommendation.setOnClickListener(v -> clearRecommendationResults());

        // 初始化POI类别列表
        initPoiCategories();
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, poiCategoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchCategory.setAdapter(categoryAdapter);

        // 初始化查询对象Spinner
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerPointNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchTarget.setAdapter(spinnerAdapter);
        updateSpinnerData();
    }

    private void toggleRecommendationPanel() {
        isRecommendationExpanded = !isRecommendationExpanded;

        // 1. Toggle Body Visibility
        recommendationBody.setVisibility(isRecommendationExpanded ? View.VISIBLE : View.GONE);

        // 2. Rotate Arrow
        float startRotation = isRecommendationExpanded ? 0f : 180f;
        float endRotation = isRecommendationExpanded ? 180f : 0f;
        ObjectAnimator animator = ObjectAnimator.ofFloat(recommendationToggleArrow, "rotation", startRotation,
                endRotation);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    /**
     * 初始化路线详情面板
     */
    private void initRouteDetailsPanel() {
        routeDetailsCard = findViewById(R.id.route_details_card);
        routeDetailsHeader = findViewById(R.id.route_details_header);
        routeDetailsBody = findViewById(R.id.route_details_body);
        routeDetailsToggleArrow = findViewById(R.id.route_details_toggle_arrow);
        tvRouteMode = findViewById(R.id.tv_route_mode);
        tvRouteDistance = findViewById(R.id.tv_route_distance);
        tvRouteDuration = findViewById(R.id.tv_route_duration);
        tvRouteWaypoints = findViewById(R.id.tv_route_waypoints);

        // 点击Header切换展开/收起
        routeDetailsHeader.setOnClickListener(v -> toggleRouteDetailsPanel());
    }

    /**
     * 切换路线详情面板的展开/收起状态
     */
    private void toggleRouteDetailsPanel() {
        isRouteDetailsExpanded = !isRouteDetailsExpanded;

        // 1. 切换 Body 可见性
        routeDetailsBody.setVisibility(isRouteDetailsExpanded ? View.VISIBLE : View.GONE);

        // 2. 箭头旋转动画
        float startRotation = isRouteDetailsExpanded ? 0f : 180f;
        float endRotation = isRouteDetailsExpanded ? 180f : 0f;
        ObjectAnimator animator = ObjectAnimator.ofFloat(routeDetailsToggleArrow, "rotation", startRotation,
                endRotation);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    /**
     * 强制展开路线详情面板（路径规划成功后调用）
     */
    private void expandRouteDetailsPanel() {
        routeDetailsCard.setVisibility(View.VISIBLE);
        if (!isRouteDetailsExpanded) {
            toggleRouteDetailsPanel();
        }
    }

    /**
     * 更新路线详情面板的显示内容
     * 
     * @param mode          出行方式名称
     * @param distance      总距离（米）
     * @param duration      总时间（秒）
     * @param waypointCount 途经点数量
     */
    private void updateRouteDetails(String mode, float distance, long duration, int waypointCount) {
        tvRouteMode.setText(mode);
        tvRouteDistance.setText(formatDistance(distance));
        tvRouteDuration.setText(formatDuration(duration));
        tvRouteWaypoints.setText(waypointCount + " 个");
    }

    /**
     * 格式化距离显示
     * 
     * @param meters 原始距离（米）
     * @return 格式化后的字符串
     */
    private String formatDistance(float meters) {
        if (meters >= 1000) {
            return String.format("%.1f 公里", meters / 1000);
        } else {
            return String.format("%.0f 米", meters);
        }
    }

    /**
     * 格式化时间显示
     * 
     * @param seconds 原始时间（秒）
     * @return 格式化后的字符串
     */
    private String formatDuration(long seconds) {
        long minutes = (seconds + 59) / 60; // 向上取整
        if (minutes >= 60) {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + " 小时 " + remainingMinutes + " 分钟";
        } else {
            return minutes + " 分钟";
        }
    }

    /**
     * 初始化POI类别列表
     * 使用高德地图POI大类名称，搜索时会自动匹配该大类下的所有子类
     */
    private void initPoiCategories() {
        poiCategoryNames.clear();
        poiCategoryNames.add("餐饮服务");
        poiCategoryNames.add("购物服务");
        poiCategoryNames.add("生活服务");
        poiCategoryNames.add("交通设施服务");
        poiCategoryNames.add("住宿服务");
        poiCategoryNames.add("风景名胜");
        poiCategoryNames.add("体育休闲服务");
        poiCategoryNames.add("医疗保健服务");
        poiCategoryNames.add("汽车服务");
        poiCategoryNames.add("金融保险服务");
        poiCategoryNames.add("科教文化服务");
        poiCategoryNames.add("公共设施");
    }

    private void updateSpinnerData() {
        spinnerPointNames.clear();
        for (Waypoint wp : waypointsList) {
            spinnerPointNames.add(wp.getName());
        }
        spinnerAdapter.notifyDataSetChanged();
        if (!spinnerPointNames.isEmpty()) {
            spinnerSearchTarget.setSelection(spinnerPointNames.size() - 1); // Default to last added
        }
    }

    private void doIntelligentRecommendation() {
        // Validation
        String radiusStr = etSearchRadius.getText().toString().trim();
        if (TextUtils.isEmpty(radiusStr)) {
            Toast.makeText(this, "请输入查找半径", Toast.LENGTH_SHORT).show();
            return;
        }
        int radius = Integer.parseInt(radiusStr);
        if (radius <= 0 || radius > 10000) {
            Toast.makeText(this, "半径范围建议在 1-10000 米之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 从Spinner获取选中的类别
        int selectedCategoryPos = spinnerSearchCategory.getSelectedItemPosition();
        if (selectedCategoryPos < 0 || selectedCategoryPos >= poiCategoryNames.size()) {
            Toast.makeText(this, "请选择查找类别", Toast.LENGTH_SHORT).show();
            return;
        }
        String category = poiCategoryNames.get(selectedCategoryPos);

        if (waypointsList.isEmpty()) {
            Toast.makeText(this, "请先添加至少一个途径点", Toast.LENGTH_SHORT).show();
            return;
        }
        int selectedSpinnerPos = spinnerSearchTarget.getSelectedItemPosition();
        if (selectedSpinnerPos < 0 || selectedSpinnerPos >= waypointsList.size()) {
            Toast.makeText(this, "目标点无效", Toast.LENGTH_SHORT).show();
            return;
        }
        Waypoint targetWaypoint = waypointsList.get(selectedSpinnerPos);

        // Clear previous results before new search
        clearRecommendationResults();
        // Draw Circle
        LatLonPoint centerPoint = targetWaypoint.getPoint();
        LatLng centerLatLng = new LatLng(centerPoint.getLatitude(), centerPoint.getLongitude());

        bufferCircle = previewMap.addCircle(new CircleOptions()
                .center(centerLatLng)
                .radius(radius)
                .strokeWidth(3)
                .strokeColor(Color.parseColor("#666666"))
                .fillColor(Color.parseColor("#20333333"))); // Semi-transparent gray

        previewMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, 14));

        // Start POI Search
        poiQuery = new PoiSearch.Query("", category, "");
        poiQuery.setPageSize(20);
        poiQuery.setPageNum(0);

        try {
            poiSearch = new PoiSearch(this, poiQuery);
            poiSearch.setOnPoiSearchListener(this);
            poiSearch.setBound(new PoiSearch.SearchBound(centerPoint, radius));
            poiSearch.searchPOIAsyn();
            Toast.makeText(this, "正在进行" + category + "缓冲区分析...", Toast.LENGTH_SHORT).show();
        } catch (AMapException e) {
            e.printStackTrace();
            Toast.makeText(this, "搜索初始化失败: " + e.getErrorMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (poiResult != null && poiResult.getQuery() != null && poiResult.getPois() != null
                    && !poiResult.getPois().isEmpty()) {
                List<PoiItem> poiItems = poiResult.getPois();
                for (PoiItem item : poiItems) {
                    LatLonPoint point = item.getLatLonPoint();
                    LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
                    Marker marker = previewMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(item.getTitle())
                            .snippet(item.getSnippet()));
                    marker.setObject(item); // Store POI data in marker
                    poiMarkers.add(marker);
                }
                Toast.makeText(this, "找到 " + poiItems.size() + " 个结果", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "周边未找到匹配设施", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "搜索失败: " + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
        // Need for interface implementation
    }

    /**
     * 清除智能推荐生成的记录（缓冲区圆和POI标记），保留途径点数据
     */
    private void clearRecommendationResults() {
        if (bufferCircle != null) {
            bufferCircle.remove();
            bufferCircle = null;
        }
        for (Marker m : poiMarkers) {
            m.remove();
        }
        poiMarkers.clear();
    }

    private void initServices() {
        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
            routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListeners() {
        globalBackButton.setOnClickListener(v -> finish());
        navMapView.setOnClickListener(v -> finish());
        navRouteRecommendation.setOnClickListener(v -> Toast.makeText(this, "已在路径推荐页面", Toast.LENGTH_SHORT).show());
        navRouteHistory.setOnClickListener(v -> startActivity(new Intent(this, RouteHistoryActivity.class)));
        previewMap.setOnMapClickListener(this);
        previewMap.setOnMarkerClickListener(this);
        startPointInput.addTextChangedListener(this);

        planDriveButton.setOnClickListener(v -> showStrategyMenu(v, R.menu.drive_strategy_menu, ROUTE_TYPE_DRIVE));
        planWalkButton.setOnClickListener(v -> showStrategyMenu(v, R.menu.walk_strategy_menu, ROUTE_TYPE_WALK));
        planRideButton.setOnClickListener(v -> showStrategyMenu(v, R.menu.ride_strategy_menu, ROUTE_TYPE_RIDE));
        planBusButton.setOnClickListener(v -> showStrategyMenu(v, R.menu.bus_strategy_menu, ROUTE_TYPE_BUS));
        planTruckButton.setOnClickListener(v -> showStrategyMenu(v, R.menu.truck_strategy_menu, ROUTE_TYPE_TRUCK));
        planEbikeButton.setOnClickListener(v -> showStrategyMenu(v, R.menu.ebike_strategy_menu, ROUTE_TYPE_EBIKE));

        startPointInput.setOnFocusChangeListener(
                (v, hasFocus) -> selectOnMapButton.setVisibility(hasFocus ? View.VISIBLE : View.GONE));
        startPointInput.setOnItemClickListener((parent, view, position, id) -> {
            if (currentTipList != null && position < currentTipList.size()) {
                Tip selectedTip = currentTipList.get(position);
                currentSelectedWaypoint = new Waypoint(selectedTip.getName(), selectedTip.getPoint());
                setAutoCompleteText(currentSelectedWaypoint.getName());
            }
        });

        selectOnMapButton.setOnClickListener(v -> Toast.makeText(this, "请直接在下方地图上点击选择位置", Toast.LENGTH_SHORT).show());
        addWaypointButton.setOnClickListener(v -> addWaypoint());
        viewWaypointsButton.setOnClickListener(v -> waypointsMenu
                .setVisibility(waypointsMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        // 收藏按钮点击事件
        btnSaveRoute.setOnClickListener(v -> saveCurrentRoute());
    }

    private void showStrategyMenu(View anchor, int menuResId, final int routeType) {
        Context wrapper = new ContextThemeWrapper(this, R.style.AppPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, anchor);
        popup.getMenuInflater().inflate(menuResId, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int strategy = getStrategyForMenuItem(item, routeType);
            planRoute(routeType, strategy);
            return true;
        });

        popup.show();
    }

    private int getStrategyForMenuItem(MenuItem item, int routeType) {
        int id = item.getItemId();
        if (routeType == ROUTE_TYPE_DRIVE) {
            if (id == R.id.strategy_drive_fastest)
                return RouteSearch.DRIVING_SINGLE_DEFAULT;
            if (id == R.id.strategy_drive_shortest)
                return RouteSearch.DRIVING_SINGLE_SHORTEST;
            if (id == R.id.strategy_drive_save_money)
                return RouteSearch.DRIVING_SINGLE_SAVE_MONEY;
            if (id == R.id.strategy_drive_no_highways)
                return RouteSearch.DRIVING_SINGLE_NO_EXPRESSWAYS;
            if (id == R.id.strategy_drive_avoid_congestion)
                return RouteSearch.DRIVING_SINGLE_AVOID_CONGESTION;
        } else if (routeType == ROUTE_TYPE_BUS) {
            if (id == R.id.strategy_bus_fastest)
                return RouteSearch.BUS_DEFAULT;
            if (id == R.id.strategy_bus_least_transfer)
                return RouteSearch.BUS_LEASE_CHANGE;
            if (id == R.id.strategy_bus_least_walk)
                return RouteSearch.BUS_LEASE_WALK;
            if (id == R.id.strategy_bus_no_subway)
                return RouteSearch.BUS_NO_SUBWAY;
            if (id == R.id.strategy_bus_save_money)
                return RouteSearch.BUS_SAVE_MONEY;
        }
        return 0; // Default strategy
    }

    private void planRoute(int routeType, int strategy) {
        // 防抖检查：防止快速连续请求导致QPS超限（错误码10021）
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRouteRequestTime < ROUTE_REQUEST_INTERVAL) {
            Toast.makeText(this, "请求过于频繁，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        lastRouteRequestTime = currentTime;

        lastRouteType = routeType; // 保存路线类型用于收藏

        // 如果勾选了TSP优化，先优化途径点顺序
        if (cbTspOptimize != null && cbTspOptimize.isChecked() && waypointsList.size() > 2) {
            List<Waypoint> optimizedList = TspOptimizer.optimize(waypointsList);
            waypointsList.clear();
            waypointsList.addAll(optimizedList);
            waypointsAdapter.notifyDataSetChanged();
            updateSpinnerData();
            Toast.makeText(this, "已优化途径点顺序", Toast.LENGTH_SHORT).show();
        }

        List<Waypoint> checkedWaypoints = waypointsList.stream().filter(Waypoint::isChecked)
                .collect(Collectors.toList());

        if (checkedWaypoints.size() < 2) {
            Toast.makeText(this, "请至少勾选两个点（起点和终点）", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLonPoint startPoint = checkedWaypoints.get(0).getPoint();
        LatLonPoint endPoint = checkedWaypoints.get(checkedWaypoints.size() - 1).getPoint();
        List<LatLonPoint> passByPoints = new ArrayList<>();
        boolean hasWaypoints = checkedWaypoints.size() > 2;
        if (hasWaypoints) {
            for (int i = 1; i < checkedWaypoints.size() - 1; i++) {
                passByPoints.add(checkedWaypoints.get(i).getPoint());
            }
        }

        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(startPoint, endPoint);

        switch (routeType) {
            case ROUTE_TYPE_DRIVE:
                RouteSearch.DriveRouteQuery driveQuery = new RouteSearch.DriveRouteQuery(fromAndTo, strategy,
                        passByPoints, null, "");
                routeSearch.calculateDriveRouteAsyn(driveQuery);
                break;
            case ROUTE_TYPE_WALK:
                if (hasWaypoints) {
                    startMultiSegmentSearch(ROUTE_TYPE_WALK, startPoint, endPoint, passByPoints);
                } else {
                    RouteSearch.WalkRouteQuery walkQuery = new RouteSearch.WalkRouteQuery(fromAndTo);
                    routeSearch.calculateWalkRouteAsyn(walkQuery);
                }
                break;
            case ROUTE_TYPE_RIDE:
            case ROUTE_TYPE_EBIKE:
                if (hasWaypoints) {
                    startMultiSegmentSearch(ROUTE_TYPE_RIDE, startPoint, endPoint, passByPoints);
                } else {
                    RouteSearch.RideRouteQuery rideQuery = new RouteSearch.RideRouteQuery(fromAndTo);
                    routeSearch.calculateRideRouteAsyn(rideQuery);
                }
                break;
            case ROUTE_TYPE_BUS:
                if (hasWaypoints) {
                    startMultiSegmentSearch(ROUTE_TYPE_BUS, startPoint, endPoint, passByPoints);
                } else {
                    RouteSearch.BusRouteQuery busQuery = new RouteSearch.BusRouteQuery(fromAndTo, strategy, "", 0);
                    routeSearch.calculateBusRouteAsyn(busQuery);
                }
                break;
            case ROUTE_TYPE_TRUCK:
                RouteSearch.TruckRouteQuery truckQuery = new RouteSearch.TruckRouteQuery(fromAndTo, strategy,
                        passByPoints, RouteSearch.TRUCK_SIZE_LIGHT);
                routeSearch.calculateTruckRouteAsyn(truckQuery);
                break;
        }
        Toast.makeText(this, "正在规划路线...", Toast.LENGTH_SHORT).show();
    }

    private void addWaypoint() {
        if (currentSelectedWaypoint != null) {
            waypointsList.add(currentSelectedWaypoint);
            waypointsAdapter.notifyItemInserted(waypointsList.size() - 1);
            updateSpinnerData(); // Update spinner UI
            updatePreviewMap(currentSelectedWaypoint.getPoint(), currentSelectedWaypoint.getName());
            Toast.makeText(this, "已添加途径点: " + currentSelectedWaypoint.getName(), Toast.LENGTH_SHORT).show();
            currentSelectedWaypoint = null;
            startPointInput.setText("");
        } else {
            Toast.makeText(this, "请先选择一个有效的地点", Toast.LENGTH_SHORT).show();
        }
    }

    private void startMultiSegmentSearch(int routeType, LatLonPoint start, LatLonPoint end,
            List<LatLonPoint> waypoints) {
        isMultiSegmentSearch = true;
        currentSegmentIndex = 0;
        currentRouteTypeForMultiSegment = routeType;
        segmentList.clear();
        clearRouteOverlays(); // Clear existing overlays

        // 重置多段路径累加数据
        totalMultiSegmentDistance = 0;
        totalMultiSegmentDuration = 0;

        // Construct segments
        LatLonPoint previousPoint = start;
        for (LatLonPoint waypoint : waypoints) {
            segmentList.add(new RouteSearch.FromAndTo(previousPoint, waypoint));
            previousPoint = waypoint;
        }
        segmentList.add(new RouteSearch.FromAndTo(previousPoint, end));

        searchNextSegment();
    }

    private void searchNextSegment() {
        if (currentSegmentIndex >= segmentList.size()) {
            // All segments searched
            isMultiSegmentSearch = false;
            Toast.makeText(this, "路径规划完成", Toast.LENGTH_SHORT).show();
            // Zoom to fit all overlays (optional: calculate bounds of all segments)
            if (!multiRouteOverlays.isEmpty()) {
                multiRouteOverlays.get(0).zoomToSpan(); // Simple approximation
            }
            return;
        }

        // 添加500ms延迟，防止高德API QPS超限（错误码10021）
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            RouteSearch.FromAndTo segment = segmentList.get(currentSegmentIndex);
            if (currentRouteTypeForMultiSegment == ROUTE_TYPE_WALK) {
                RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(segment);
                routeSearch.calculateWalkRouteAsyn(query);
            } else if (currentRouteTypeForMultiSegment == ROUTE_TYPE_RIDE) {
                RouteSearch.RideRouteQuery query = new RouteSearch.RideRouteQuery(segment);
                routeSearch.calculateRideRouteAsyn(query);
            } else if (currentRouteTypeForMultiSegment == ROUTE_TYPE_BUS) {
                RouteSearch.BusRouteQuery query = new RouteSearch.BusRouteQuery(segment, RouteSearch.BUS_DEFAULT, "",
                        0);
                routeSearch.calculateBusRouteAsyn(query);
            }
        }, 500); // 500ms延迟
    }

    private void clearRouteOverlays() {
        if (drivingRouteOverlay != null)
            drivingRouteOverlay.removeFromMap();
        if (walkRouteOverlay != null)
            walkRouteOverlay.removeFromMap();
        if (rideRouteOverlay != null)
            rideRouteOverlay.removeFromMap();
        if (busRouteOverlay != null)
            busRouteOverlay.removeFromMap();

        for (RouteOverlay overlay : multiRouteOverlays) {
            overlay.removeFromMap();
        }
        multiRouteOverlays.clear();
    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int rCode) {
        clearRouteOverlays();
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null && !result.getPaths().isEmpty()) {
                final DrivePath drivePath = result.getPaths().get(0);
                drivingRouteOverlay = new DrivingRouteOverlay(
                        this,
                        previewMap,
                        drivePath,
                        result.getStartPos(),
                        result.getTargetPos(),
                        null);
                drivingRouteOverlay.setNodeIconVisibility(false);
                drivingRouteOverlay.setIsColorfulline(true);
                drivingRouteOverlay.addToMap();
                drivingRouteOverlay.zoomToSpan();
                showSaveButton(); // 显示收藏按钮

                // 更新路线详情面板
                updateRouteDetails("驾车", drivePath.getDistance(), drivePath.getDuration(), waypointsList.size());
                expandRouteDetailsPanel();
            } else {
                Toast.makeText(this, "没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "路径规划失败: " + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult result, int rCode) {
        if (!isMultiSegmentSearch)
            clearRouteOverlays();

        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null && !result.getPaths().isEmpty()) {
                final WalkPath walkPath = result.getPaths().get(0);
                WalkRouteOverlay overlay = new WalkRouteOverlay(
                        this,
                        previewMap,
                        walkPath,
                        result.getStartPos(),
                        result.getTargetPos());
                overlay.setNodeIconVisibility(false);

                if (isMultiSegmentSearch) {
                    // 累加距离和时间
                    totalMultiSegmentDistance += walkPath.getDistance();
                    totalMultiSegmentDuration += walkPath.getDuration();

                    // Hide end marker for all but the last segment
                    if (currentSegmentIndex < segmentList.size() - 1) {
                        overlay.setEndPointVisible(false);
                    }
                    // Hide start marker for all but the first segment
                    if (currentSegmentIndex > 0) {
                        overlay.setStartPointVisible(false);
                    }
                    multiRouteOverlays.add(overlay);
                    overlay.addToMap();

                    currentSegmentIndex++;
                    // 最后一段完成后更新面板
                    if (currentSegmentIndex >= segmentList.size()) {
                        updateRouteDetails("步行", totalMultiSegmentDistance, totalMultiSegmentDuration,
                                waypointsList.size());
                        expandRouteDetailsPanel();
                        showSaveButton();
                    }
                    searchNextSegment();
                } else {
                    walkRouteOverlay = overlay;
                    walkRouteOverlay.addToMap();
                    walkRouteOverlay.zoomToSpan();
                    showSaveButton(); // 显示收藏按钮

                    // 更新路线详情面板
                    updateRouteDetails("步行", walkPath.getDistance(), walkPath.getDuration(), waypointsList.size());
                    expandRouteDetailsPanel();
                }
            } else {
                Toast.makeText(this, "没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "路径规划失败: " + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRideRouteSearched(RideRouteResult result, int rCode) {
        if (!isMultiSegmentSearch)
            clearRouteOverlays();

        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null && !result.getPaths().isEmpty()) {
                final RidePath ridePath = result.getPaths().get(0);
                RideRouteOverlay overlay = new RideRouteOverlay(
                        this,
                        previewMap,
                        ridePath,
                        result.getStartPos(),
                        result.getTargetPos());
                overlay.setNodeIconVisibility(false);

                if (isMultiSegmentSearch) {
                    // 累加距离和时间
                    totalMultiSegmentDistance += ridePath.getDistance();
                    totalMultiSegmentDuration += ridePath.getDuration();

                    // Hide end marker for all but the last segment
                    if (currentSegmentIndex < segmentList.size() - 1) {
                        overlay.setEndPointVisible(false);
                    }
                    // Hide start marker for all but the first segment
                    if (currentSegmentIndex > 0) {
                        overlay.setStartPointVisible(false);
                    }
                    multiRouteOverlays.add(overlay);
                    overlay.addToMap();

                    currentSegmentIndex++;
                    // 最后一段完成后更新面板
                    if (currentSegmentIndex >= segmentList.size()) {
                        updateRouteDetails("骑行", totalMultiSegmentDistance, totalMultiSegmentDuration,
                                waypointsList.size());
                        expandRouteDetailsPanel();
                        showSaveButton();
                    }
                    searchNextSegment();
                } else {
                    rideRouteOverlay = overlay;
                    rideRouteOverlay.addToMap();
                    rideRouteOverlay.zoomToSpan();
                    showSaveButton(); // 显示收藏按钮

                    // 更新路线详情面板
                    updateRouteDetails("骑行", ridePath.getDistance(), ridePath.getDuration(), waypointsList.size());
                    expandRouteDetailsPanel();
                }
            } else {
                Toast.makeText(this, "没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "路径规划失败: " + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBusRouteSearched(BusRouteResult result, int rCode) {
        if (!isMultiSegmentSearch)
            clearRouteOverlays();

        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null && !result.getPaths().isEmpty()) {
                final BusPath busPath = result.getPaths().get(0);
                BusRouteOverlay overlay = new BusRouteOverlay(
                        this,
                        previewMap,
                        busPath,
                        result.getStartPos(),
                        result.getTargetPos());
                overlay.setNodeIconVisibility(false);

                // 公交路线距离 = 步行距离 + 公交距离
                float busDistance = busPath.getWalkDistance() + busPath.getBusDistance();

                if (isMultiSegmentSearch) {
                    // 累加距离和时间
                    totalMultiSegmentDistance += busDistance;
                    totalMultiSegmentDuration += busPath.getDuration();

                    // Hide end marker for all but the last segment
                    if (currentSegmentIndex < segmentList.size() - 1) {
                        overlay.setEndPointVisible(false);
                    }
                    // Hide start marker for all but the first segment
                    if (currentSegmentIndex > 0) {
                        overlay.setStartPointVisible(false);
                    }
                    multiRouteOverlays.add(overlay);
                    overlay.addToMap();

                    currentSegmentIndex++;
                    // 最后一段完成后更新面板
                    if (currentSegmentIndex >= segmentList.size()) {
                        updateRouteDetails("公交", totalMultiSegmentDistance, totalMultiSegmentDuration,
                                waypointsList.size());
                        expandRouteDetailsPanel();
                        showSaveButton();
                    }
                    searchNextSegment();
                } else {
                    busRouteOverlay = overlay;
                    busRouteOverlay.addToMap();
                    busRouteOverlay.zoomToSpan();
                    showSaveButton(); // 显示收藏按钮

                    // 更新路线详情面板
                    updateRouteDetails("公交", busDistance, busPath.getDuration(), waypointsList.size());
                    expandRouteDetailsPanel();
                }
            } else {
                Toast.makeText(this, "没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "公交路径规划失败: " + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    // ... (rest of the existing methods like initPreviewMap, initRecyclerView,
    // etc.)
    private void initPreviewMap() {
        if (previewMap == null) {
            previewMap = previewMapView.getMap();
        }
        previewMap.getUiSettings().setAllGesturesEnabled(true);
        previewMap.getUiSettings().setZoomControlsEnabled(false);

        // 设置定位样式，定位一次后停止定位（与MainActivity一致）
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        previewMap.setMyLocationStyle(myLocationStyle);
        previewMap.setMyLocationEnabled(true);

        // Set custom InfoWindow adapter
        previewMap.setInfoWindowAdapter(new AMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.layout_poi_info_window, null);
                TextView titleTv = view.findViewById(R.id.poi_title);
                TextView snippetTv = view.findViewById(R.id.poi_snippet);
                titleTv.setText(marker.getTitle());
                String snippet = marker.getSnippet();
                if (snippet != null && !snippet.isEmpty()) {
                    snippetTv.setText(snippet);
                    snippetTv.setVisibility(View.VISIBLE);
                } else {
                    snippetTv.setVisibility(View.GONE);
                }
                return view;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
    }

    private void initRecyclerView() {
        waypointsAdapter = new WaypointsAdapter(waypointsList, this);
        waypointRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        waypointRecyclerView.setAdapter(waypointsAdapter);

        ItemTouchHelper.Callback callback = new WaypointItemTouchHelperCallback();
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(waypointRecyclerView);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onDeleteRequest(int position) {
        showDeleteConfirmationDialog(position);
    }

    private void showDeleteConfirmationDialog(final int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("确认删除")
                .setMessage("您确定要移除这个途径点吗？")
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("确认", (dialog, which) -> {
                    waypointsList.remove(position);
                    waypointsAdapter.notifyItemRemoved(position);
                    redrawMapMarkers();
                    updateSpinnerData(); // Update spinner UI
                })
                .show();
    }

    private void setSystemBarPadding() {
        final int initialPaddingTop = startPointWrapper.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            startPointWrapper.setPadding(startPointWrapper.getPaddingLeft(),
                    initialPaddingTop + statusBarHeight,
                    startPointWrapper.getPaddingRight(),
                    startPointWrapper.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void updatePreviewMap(LatLonPoint point, String name) {
        if (previewMap != null && point != null) {
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            previewMap.addMarker(new MarkerOptions().position(latLng).title(name));
            previewMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    private void redrawMapMarkers() {
        clearRouteOverlays(); // Also clear routes when redrawing markers
        previewMap.clear(); // Clear all markers
        for (Waypoint waypoint : waypointsList) {
            updatePreviewMap(waypoint.getPoint(), waypoint.getName());
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Remove previous marker if exists
        if (pendingMapClickMarker != null) {
            pendingMapClickMarker.remove();
        }

        LatLonPoint point = new LatLonPoint(latLng.latitude, latLng.longitude);
        RegeocodeQuery query = new RegeocodeQuery(point, 200, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);

        // Add new marker with custom icon
        pendingMapClickMarker = previewMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("正在获取地址...")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_pin)));
        currentSelectedPoi = null; // Reset POI selection on map click
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getObject() instanceof PoiItem) {
            currentSelectedPoi = (PoiItem) marker.getObject();
            marker.showInfoWindow();

            // Auto fill the input logic
            currentSelectedWaypoint = new Waypoint(currentSelectedPoi.getTitle(), currentSelectedPoi.getLatLonPoint());
            setAutoCompleteText(currentSelectedPoi.getTitle());
            Toast.makeText(this, "已选中: " + currentSelectedPoi.getTitle() + "\n点击 + 号可添加", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String addressName = result.getRegeocodeAddress().getFormatAddress();
                LatLonPoint point = result.getRegeocodeQuery().getPoint();
                currentSelectedWaypoint = new Waypoint(addressName, point);
                setAutoCompleteText(addressName);

                // Update the pending marker's title
                if (pendingMapClickMarker != null) {
                    pendingMapClickMarker.setTitle(addressName);
                    pendingMapClickMarker.showInfoWindow();
                }
            } else {
                Toast.makeText(this, "没有找到结果", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "逆地理编码失败: " + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        /* no-op */ }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    private void setAutoCompleteText(String text) {
        isProgrammaticChange = true;
        startPointInput.setText(text);
        startPointInput.setSelection(text.length());
        isProgrammaticChange = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (isProgrammaticChange)
            return;
        String newText = s.toString().trim();
        InputtipsQuery inputquery = new InputtipsQuery(newText, "");
        inputquery.setCityLimit(true);
        Inputtips inputTips = new Inputtips(this, inputquery);
        inputTips.setInputtipsListener(this);
        inputTips.requestInputtipsAsyn();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        if (rCode == 1000) {
            currentTipList = tipList;
            List<String> listString = new ArrayList<>();
            for (Tip tip : tipList) {
                listString.add(tip.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_search_suggestion, listString);
            startPointInput.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    // MapView lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        previewMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        previewMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        previewMapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        previewMapView.onSaveInstanceState(outState);
    }

    // Helper class for ItemTouchHelper
    private class WaypointItemTouchHelperCallback extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            Collections.swap(waypointsList, fromPosition, toPosition);
            waypointsAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            /* no-op */ }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            redrawMapMarkers(); // Redraw markers after drag-and-drop is complete
        }
    }

    /**
     * 保存当前路线到收藏
     */
    private void saveCurrentRoute() {
        List<Waypoint> checkedWaypoints = waypointsList.stream().filter(Waypoint::isChecked)
                .collect(Collectors.toList());

        if (checkedWaypoints.size() < 2) {
            Toast.makeText(this, "请至少勾选两个途径点", Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成路线名称
        String routeName = checkedWaypoints.get(0).getName() + " → " +
                checkedWaypoints.get(checkedWaypoints.size() - 1).getName();

        RouteRecord record = new RouteRecord(routeName, checkedWaypoints, lastRouteType);
        record.setFavorite(true);
        storageHelper.saveRoute(record);

        Toast.makeText(this, "路线已收藏", Toast.LENGTH_SHORT).show();
    }

    /**
     * 从Intent加载路线数据
     */
    private void loadRouteFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("route_data")) {
            String routeJson = intent.getStringExtra("route_data");
            if (routeJson != null) {
                try {
                    RouteRecord record = gson.fromJson(routeJson, RouteRecord.class);
                    if (record != null && record.getWaypoints() != null) {
                        // 清空现有途径点
                        waypointsList.clear();

                        // 加载途径点
                        for (RouteRecord.WaypointData wpData : record.getWaypoints()) {
                            Waypoint wp = new Waypoint(wpData.name, wpData.toLatLonPoint());
                            wp.setChecked(true);
                            waypointsList.add(wp);
                        }

                        waypointsAdapter.notifyDataSetChanged();
                        updateSpinnerData();
                        redrawMapMarkers();

                        // 自动规划路线
                        if (waypointsList.size() >= 2) {
                            lastRouteType = record.getRouteType();
                            planRoute(lastRouteType, 0);
                        }

                        Toast.makeText(this, "已加载路线: " + record.getName(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 显示收藏按钮（路线规划成功后调用）
     */
    private void showSaveButton() {
        if (btnSaveRoute != null) {
            btnSaveRoute.setVisibility(View.VISIBLE);
        }
    }
}

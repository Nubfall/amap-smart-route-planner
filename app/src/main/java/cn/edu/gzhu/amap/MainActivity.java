package cn.edu.gzhu.amap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, PoiSearch.OnPoiSearchListener, Inputtips.InputtipsListener,
        AMap.OnMyLocationChangeListener, GeocodeSearch.OnGeocodeSearchListener, AMap.InfoWindowAdapter {

    private MapView mapView;
    private AMap aMap;
    private AutoCompleteTextView searchInput;
    private Button searchButton;
    private RelativeLayout rootLayout;
    private FrameLayout searchWrapper;
    private LinearLayout searchLayout; // For theme switching

    // New UI elements for layers control
    private Button layersButton;
    private CardView layersMenu;
    private RadioGroup mapTypeGroup;
    private SwitchMaterial trafficSwitch;

    // Bottom navigation
    private ImageButton navMapView;
    private ImageButton navRouteRecommendation;
    private ImageButton navRouteHistory;

    // Custom zoom controls
    private ImageButton zoomInButton;
    private ImageButton zoomOutButton;

    private PoiSearch.Query query;
    private PoiSearch poiSearch;
    private GeocodeSearch geocoderSearch;
    private String mCurrentCity;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState); // 此方法必须重写

        init();
        setSystemBarPadding();
        requestLocationPermission();
    }

    /**
     * 初始化
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        // Hide default zoom controls
        aMap.getUiSettings().setZoomControlsEnabled(false);

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setOnMyLocationChangeListener(this);
        aMap.setInfoWindowAdapter(this);

        rootLayout = findViewById(R.id.root_layout);
        searchWrapper = findViewById(R.id.search_wrapper);

        // Initialize search components
        searchLayout = findViewById(R.id.search_layout);
        searchInput = findViewById(R.id.search_input);
        // 通过代码设置下拉背景，解决XML中的编译错误
        Drawable dropdownBg = ContextCompat.getDrawable(this, R.drawable.bg_search_suggestion_dropdown);
        searchInput.setDropDownBackgroundDrawable(dropdownBg);
        searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);

        // Geocoder
        try {
            geocoderSearch = new GeocodeSearch(this);
            geocoderSearch.setOnGeocodeSearchListener(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }

        // Initialize new layers control UI
        layersButton = findViewById(R.id.layers_button);
        layersMenu = findViewById(R.id.layers_menu);
        mapTypeGroup = findViewById(R.id.map_type_group);
        trafficSwitch = findViewById(R.id.traffic_switch);

        // Bottom navigation
        navMapView = findViewById(R.id.nav_map_view);
        navRouteRecommendation = findViewById(R.id.nav_route_recommendation);
        navRouteHistory = findViewById(R.id.nav_route_history);

        // Initialize custom zoom controls
        zoomInButton = findViewById(R.id.zoom_in_button);
        zoomOutButton = findViewById(R.id.zoom_out_button);

        // Set listeners for the new UI
        layersButton.setOnClickListener(v -> {
            if (layersMenu.getVisibility() == View.VISIBLE) {
                layersMenu.setVisibility(View.GONE);
            } else {
                layersMenu.setVisibility(View.VISIBLE);
            }
        });

        mapTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.map_type_normal) {
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                setSearchBarLightTheme();
            } else if (checkedId == R.id.map_type_satellite) {
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                setSearchBarLightTheme();
            } else if (checkedId == R.id.map_type_night) {
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                setSearchBarDarkTheme();
            } else if (checkedId == R.id.map_type_navi) {
                aMap.setMapType(AMap.MAP_TYPE_NAVI);
                setSearchBarLightTheme();
            }
        });

        trafficSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> aMap.setTrafficEnabled(isChecked));

        // Setup search input listeners
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = s.toString().trim();
                if (newText.length() > 0) {
                    InputtipsQuery inputquery = new InputtipsQuery(newText, mCurrentCity);
                    Inputtips inputTips = new Inputtips(MainActivity.this, inputquery);
                    inputTips.setInputtipsListener(MainActivity.this);
                    inputTips.requestInputtipsAsyn();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = (String) parent.getItemAtPosition(position);
            searchInput.setText(selectedText);
            searchInput.post(() -> startSearch(selectedText));
        });

        // Set listeners for bottom navigation
        navMapView.setOnClickListener(v -> Toast.makeText(this, "已在地图浏览页面", Toast.LENGTH_SHORT).show());
        navRouteRecommendation
                .setOnClickListener(v -> startActivity(new Intent(this, RouteRecommendationActivity.class)));
        navRouteHistory.setOnClickListener(v -> startActivity(new Intent(this, RouteHistoryActivity.class)));

        // Set listeners for custom zoom controls
        zoomInButton.setOnClickListener(v -> aMap.animateCamera(CameraUpdateFactory.zoomIn()));
        zoomOutButton.setOnClickListener(v -> aMap.animateCamera(CameraUpdateFactory.zoomOut()));
    }

    private void setSearchBarLightTheme() {
        if (searchLayout != null && searchInput != null) {
            searchLayout.setBackgroundResource(R.drawable.bg_search_bar_light);
            searchInput.setTextColor(Color.BLACK);
            searchInput.setHintTextColor(Color.GRAY);
        }
    }

    private void setSearchBarDarkTheme() {
        if (searchLayout != null && searchInput != null) {
            searchLayout.setBackgroundResource(R.drawable.bg_search_bar_dark);
            searchInput.setTextColor(Color.WHITE);
            searchInput.setHintTextColor(Color.LTGRAY);
        }
    }

    private void setSystemBarPadding() {
        final int initialPaddingTop = searchWrapper.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            searchWrapper.setPadding(searchWrapper.getPaddingLeft(),
                    initialPaddingTop + statusBarHeight,
                    searchWrapper.getPaddingRight(),
                    searchWrapper.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            aMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                aMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search_button) {
            doSearchQuery();
        }
    }

    private void doSearchQuery() {
        String keyWord = searchInput.getText().toString().trim();
        startSearch(keyWord);
    }

    private void startSearch(String keyWord) {
        if (keyWord == null || "".equals(keyWord)) {
            Toast.makeText(this, "请输入搜索关键字", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboardAndClearFocus();

        query = new PoiSearch.Query(keyWord, "", mCurrentCity);
        query.setPageSize(10);
        query.setPageNum(0);

        try {
            poiSearch = new PoiSearch(this, query);
            poiSearch.setOnPoiSearchListener(this);
            poiSearch.searchPOIAsyn();
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    private void hideKeyboardAndClearFocus() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        if (searchInput != null) {
            searchInput.clearFocus();
        }
        if (rootLayout != null) {
            rootLayout.requestFocus();
        }
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        if (rCode == 1000) {
            if (poiResult != null && poiResult.getQuery() != null) {
                if (poiResult.getQuery().equals(query)) {
                    aMap.clear();
                    List<PoiItem> poiItems = poiResult.getPois();
                    if (poiItems != null && poiItems.size() > 0) {
                        for (int i = 0; i < poiItems.size(); i++) {
                            PoiItem poiItem = poiItems.get(i);
                            LatLonPoint latLonPoint = poiItem.getLatLonPoint();
                            aMap.addMarker(new MarkerOptions()
                                    .position(new com.amap.api.maps.model.LatLng(latLonPoint.getLatitude(),
                                            latLonPoint.getLongitude()))
                                    .title(poiItem.getTitle())
                                    .snippet(poiItem.getSnippet()));
                        }
                        if (poiItems.size() > 0) {
                            PoiItem item = poiItems.get(0);
                            LatLonPoint latLonPoint = item.getLatLonPoint();
                            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new com.amap.api.maps.model.LatLng(
                                    latLonPoint.getLatitude(), latLonPoint.getLongitude()), 15));

                            MyLocationStyle myLocationStyle = aMap.getMyLocationStyle();
                            if (myLocationStyle == null) {
                                myLocationStyle = new MyLocationStyle();
                            }
                            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
                            aMap.setMyLocationStyle(myLocationStyle);
                        }
                    } else {
                        Toast.makeText(this, "无搜索结果", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "无搜索结果", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "搜索失败，错误码：" + rCode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        if (rCode == 1000) {
            List<String> listString = new ArrayList<>();
            for (int i = 0; i < tipList.size(); i++) {
                listString.add(tipList.get(i).getName());
            }
            ArrayAdapter<String> aAdapter = new ArrayAdapter<>(
                    this,
                    R.layout.item_search_suggestion, listString);
            searchInput.setAdapter(aAdapter);
            aAdapter.notifyDataSetChanged();
        } else {
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (location != null && mCurrentCity == null) {
            LatLonPoint latLonPoint = new LatLonPoint(location.getLatitude(), location.getLongitude());
            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
            geocoderSearch.getFromLocationAsyn(query);
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        if (i == 1000) {
            if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null
                    && regeocodeResult.getRegeocodeAddress().getFormatAddress() != null) {
                mCurrentCity = regeocodeResult.getRegeocodeAddress().getCity();
                Log.d("MainActivity", "Current city: " + mCurrentCity);
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public View getInfoWindow(Marker marker) {
        View infoWindow = LayoutInflater.from(this).inflate(R.layout.layout_info_window, null);
        TextView title = infoWindow.findViewById(R.id.info_window_title);
        TextView snippet = infoWindow.findViewById(R.id.info_window_snippet);
        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());
        return infoWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}

package cn.edu.gzhu.amap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

public class MapSelectionActivity extends AppCompatActivity implements AMap.OnMapClickListener, GeocodeSearch.OnGeocodeSearchListener {

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_NAME = "extra_name";

    private RelativeLayout rootLayout;
    private FrameLayout backButtonWrapper;
    private ImageButton globalBackButton;
    private MapView mapView;
    private AMap aMap;
    private GeocodeSearch geocodeSearch;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_map_selection);

        initViews(savedInstanceState);
        initMap();
        initListeners();
        setSystemBarPadding();
    }

    private void initViews(Bundle savedInstanceState) {
        rootLayout = findViewById(R.id.map_selection_root_layout);
        backButtonWrapper = findViewById(R.id.back_button_wrapper);
        globalBackButton = findViewById(R.id.global_back_button);
        mapView = findViewById(R.id.map_view_selection);
        mapView.onCreate(savedInstanceState); // 此方法必须重写
    }

    private void setSystemBarPadding() {
        final int initialPaddingTop = backButtonWrapper.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            backButtonWrapper.setPadding(backButtonWrapper.getPaddingLeft(),
                    initialPaddingTop + statusBarHeight,
                    backButtonWrapper.getPaddingRight(),
                    backButtonWrapper.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }

        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
    }

    private void initListeners() {
        globalBackButton.setOnClickListener(v -> finish());
        aMap.setOnMapClickListener(this);

        aMap.setOnInfoWindowClickListener(marker -> {
            Intent resultIntent = new Intent();
            LatLng position = marker.getPosition();
            resultIntent.putExtra(EXTRA_LATITUDE, position.latitude);
            resultIntent.putExtra(EXTRA_LONGITUDE, position.longitude);
            resultIntent.putExtra(EXTRA_NAME, marker.getTitle());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // 发起逆地理编码请求
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 200, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);

        // 在地图上添加标记
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
        selectedMarker = aMap.addMarker(new MarkerOptions().position(latLng).title("正在获取地址..."));
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String addressName = result.getRegeocodeAddress().getFormatAddress();
                if(selectedMarker != null) {
                    selectedMarker.setTitle(addressName);
                    selectedMarker.setSnippet("点击确认该起点");
                    selectedMarker.showInfoWindow();
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
        // no-op
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
}


package cn.edu.gzhu.amap.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import java.util.ArrayList;
import java.util.List;
import cn.edu.gzhu.amap.R;

public class RouteOverlay {
    protected List<Marker> stationMarkers = new ArrayList<>();
    protected List<Polyline> allPolyLines = new ArrayList<>();
    protected Marker startMarker;
    protected Marker endMarker;
    protected LatLng startPoint;
    protected LatLng endPoint;
    protected AMap mAMap;
    private Context mContext;
    private Bitmap startBit, endBit, busBit, walkBit, driveBit;
    protected boolean nodeIconVisible = true;

    public RouteOverlay(Context context) {
        mContext = context;
    }

    public void removeFromMap() {
        if (startMarker != null) {
            startMarker.remove();
        }
        if (endMarker != null) {
            endMarker.remove();
        }
        for (Marker marker : stationMarkers) {
            marker.remove();
        }
        for (Polyline line : allPolyLines) {
            line.remove();
        }
        stationMarkers.clear();
        allPolyLines.clear();
    }

    protected BitmapDescriptor getStartBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_select_location);
    }

    protected BitmapDescriptor getEndBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_select_location);
    }

    protected BitmapDescriptor getBusBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_bus);
    }

    protected BitmapDescriptor getWalkBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_walk);
    }

    protected BitmapDescriptor getDriveBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_car);
    }

    protected void addStartAndEndMarker() {
        if (startPoint != null && startPointVisible) {
            startMarker = mAMap.addMarker((new MarkerOptions())
                    .position(startPoint).icon(getStartBitmapDescriptor())
                    .title("\u8d77\u70b9"));
        }
        if (endPoint != null && endPointVisible) {
            endMarker = mAMap.addMarker((new MarkerOptions()).position(endPoint)
                    .icon(getEndBitmapDescriptor()).title("\u7ec8\u70b9"));
        }
    }

    public void zoomToSpan() {
        if (startPoint == null || mAMap == null) {
            return;
        }
        try {
            LatLngBounds.Builder b = LatLngBounds.builder();
            b.include(startPoint);
            b.include(endPoint);
            mAMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void addStationMarker(MarkerOptions options) {
        if (options == null) {
            return;
        }
        Marker marker = mAMap.addMarker(options);
        if (marker != null) {
            stationMarkers.add(marker);
        }
    }

    protected void addPolyLine(PolylineOptions options) {
        if (options == null) {
            return;
        }
        Polyline polyline = mAMap.addPolyline(options);
        if (polyline != null) {
            allPolyLines.add(polyline);
        }
    }

    protected float getRouteWidth() {
        return 18f;
    }

    protected int getWalkColor() {
        return Color.parseColor("#6db74d");
    }

    protected int getBusColor() {
        return Color.parseColor("#537edc");
    }

    protected int getDriveColor() {
        return Color.parseColor("#537edc");
    }

    public void setNodeIconVisibility(boolean visible) {
        nodeIconVisible = visible;
        for (Marker marker : stationMarkers) {
            marker.setVisible(visible);
        }
    }

    protected boolean startPointVisible = true;
    protected boolean endPointVisible = true;

    public void setStartPointVisible(boolean visible) {
        this.startPointVisible = visible;
    }

    public void setEndPointVisible(boolean visible) {
        this.endPointVisible = visible;
    }
}

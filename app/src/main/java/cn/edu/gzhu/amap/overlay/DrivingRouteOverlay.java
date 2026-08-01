
package cn.edu.gzhu.amap.overlay;

import android.content.Context;
import android.graphics.Color;
import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.TMC;

import java.util.ArrayList;
import java.util.List;

import cn.edu.gzhu.amap.R;

public class DrivingRouteOverlay extends RouteOverlay {

    private DrivePath drivePath;
    private List<LatLonPoint> throughPointList;
    private List<MarkerOptions> throughPointMarkerOptions;
    private boolean isColorfulline = true;
    private float routeWidth = 25f;
    private List<LatLng> latLngsOfPath;

    public void setIsColorfulline(boolean isColorfulline) {
        this.isColorfulline = isColorfulline;
    }

    public DrivingRouteOverlay(Context context, AMap amap, DrivePath path,
                               LatLonPoint start, LatLonPoint end, List<LatLonPoint> throughPointList) {
        super(context);
        this.mAMap = amap;
        this.drivePath = path;
        this.startPoint = new LatLng(start.getLatitude(), start.getLongitude());
        this.endPoint = new LatLng(end.getLatitude(), end.getLongitude());
        this.throughPointList = throughPointList;
    }

    public void addToMap() {
        initPolylineOptions();
        try {
            if (mAMap == null) {
                return;
            }

            if (routeWidth == 0 || drivePath == null) {
                return;
            }
            latLngsOfPath = new ArrayList<>();
            List<DriveStep> driveSteps = drivePath.getSteps();
            for (DriveStep step : driveSteps) {
                List<LatLonPoint> latlonpoints = step.getPolyline();
                List<TMC> tmcs = step.getTMCs();
                if (tmcs != null && tmcs.size() > 0 && isColorfulline) {
                    addDrivingStationPassThroughPoints(latlonpoints, tmcs);
                } else {
                    addDrivingStationPassThroughPoints(latlonpoints, null);
                }
                for (LatLonPoint latlonpoint : latlonpoints) {
                    latLngsOfPath.add(convertToLatLng(latlonpoint));
                }
            }
            addStartAndEndMarker();
            addThroughPointMarker();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void addDrivingStationPassThroughPoints(List<LatLonPoint> latlonpoints, List<TMC> tmcs) {
        if (latlonpoints == null || latlonpoints.size() < 1) {
            return;
        }
        if (tmcs != null && tmcs.size() >= 1) {
            for (TMC tmc : tmcs) {
                LatLonPoint start = tmc.getPolyline().get(0);
                LatLonPoint end = tmc.getPolyline().get(tmc.getPolyline().size() - 1);
                int startIndex = -1, endIndex = -1;
                for (int i = 0; i < latlonpoints.size(); i++) {
                    if (latlonpoints.get(i).equals(start)) {
                        startIndex = i;
                    }
                    if (latlonpoints.get(i).equals(end)) {
                        endIndex = i;
                    }
                }
                if (startIndex >= 0 && endIndex > startIndex) {
                    List<LatLonPoint> subList = latlonpoints.subList(startIndex, endIndex + 1);
                    addPolylineWithPoints(subList, getcolor(tmc.getStatus()));
                }
            }
        } else {
            addPolylineWithPoints(latlonpoints, getDriveColor());
        }
    }

    private void addPolylineWithPoints(List<LatLonPoint> points, int color) {
        PolylineOptions options = new PolylineOptions();
        options.width(routeWidth);
        options.color(color);
        for (LatLonPoint point : points) {
            options.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }
        addPolyLine(options);
    }

    private int getcolor(String status) {
        switch (status) {
            case "畅通":
                return Color.GREEN;
            case "缓行":
                return Color.YELLOW;
            case "拥堵":
                return Color.RED;
            case "严重拥堵":
                return Color.parseColor("#990033");
            default:
                return getDriveColor();
        }
    }

    private LatLng convertToLatLng(LatLonPoint point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    private void addThroughPointMarker() {
        if (this.throughPointList != null && this.throughPointList.size() > 0) {
            LatLonPoint latLonPoint;
            for (int i = 0; i < this.throughPointList.size(); i++) {
                latLonPoint = this.throughPointList.get(i);
                if (latLonPoint != null) {
                    addStationMarker(new MarkerOptions()
                            .position(new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()))
                            .visible(nodeIconVisible)
                            .icon(getThroughPointBitDes())    
                    );
                }
            }
        }
    }
    
    private BitmapDescriptor getThroughPointBitDes() {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_select_location);
    }

    private void initPolylineOptions() {
        throughPointMarkerOptions = new ArrayList<>();
    }
}

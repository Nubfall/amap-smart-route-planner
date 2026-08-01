package cn.edu.gzhu.amap.overlay;

import android.content.Context;
import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.RouteBusLineItem;

import java.util.List;

public class BusRouteOverlay extends RouteOverlay {

    private BusPath busPath;

    public BusRouteOverlay(Context context, AMap amap, BusPath path, LatLonPoint start, LatLonPoint end) {
        super(context);
        this.mAMap = amap;
        this.busPath = path;
        this.startPoint = new LatLng(start.getLatitude(), start.getLongitude());
        this.endPoint = new LatLng(end.getLatitude(), end.getLongitude());
    }

    public void addToMap() {
        try {
            if (mAMap == null || busPath == null) {
                return;
            }
            List<BusStep> busSteps = busPath.getSteps();
            for (BusStep step : busSteps) {
                if (step.getBusLines() != null && !step.getBusLines().isEmpty()) {
                    for (RouteBusLineItem busLine : step.getBusLines()) {
                        if (busLine != null && busLine.getPolyline() != null) {
                            List<LatLonPoint> latLonPoints = busLine.getPolyline();
                            PolylineOptions options = new PolylineOptions();
                            options.color(getBusColor());
                            options.width(getRouteWidth());
                            for (LatLonPoint point : latLonPoints) {
                                options.add(new LatLng(point.getLatitude(), point.getLongitude()));
                            }
                            addPolyLine(options);
                        }
                    }
                }
            }
            addStartAndEndMarker();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

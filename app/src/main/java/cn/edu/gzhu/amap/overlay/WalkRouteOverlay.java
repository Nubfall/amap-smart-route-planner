
package cn.edu.gzhu.amap.overlay;

import android.content.Context;
import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkStep;

import java.util.List;

public class WalkRouteOverlay extends RouteOverlay {

    private WalkPath walkPath;

    public WalkRouteOverlay(Context context, AMap amap, WalkPath path, LatLonPoint start, LatLonPoint end) {
        super(context);
        this.mAMap = amap;
        this.walkPath = path;
        this.startPoint = new LatLng(start.getLatitude(), start.getLongitude());
        this.endPoint = new LatLng(end.getLatitude(), end.getLongitude());
    }

    public void addToMap() {
        try {
            if (mAMap == null || walkPath == null) {
                return;
            }
            List<WalkStep> walkSteps = walkPath.getSteps();
            for (WalkStep step : walkSteps) {
                List<LatLonPoint> latLonPoints = step.getPolyline();
                if (latLonPoints == null) {
                    continue;
                }
                PolylineOptions options = new PolylineOptions();
                options.color(getWalkColor());
                options.width(getRouteWidth());
                for (LatLonPoint point : latLonPoints) {
                    options.add(new LatLng(point.getLatitude(), point.getLongitude()));
                }
                addPolyLine(options);

                // Add station markers
                if (nodeIconVisible) {
                     addStationMarker(new MarkerOptions()
                        .position(new LatLng(latLonPoints.get(0).getLatitude(), latLonPoints.get(0).getLongitude()))
                        .icon(getWalkBitmapDescriptor()));
                }
            }
            addStartAndEndMarker();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

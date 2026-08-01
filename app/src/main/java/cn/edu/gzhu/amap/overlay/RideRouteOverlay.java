
package cn.edu.gzhu.amap.overlay;

import android.content.Context;
import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideStep;

import java.util.List;

public class RideRouteOverlay extends RouteOverlay {

    private RidePath ridePath;

    public RideRouteOverlay(Context context, AMap amap, RidePath path, LatLonPoint start, LatLonPoint end) {
        super(context);
        this.mAMap = amap;
        this.ridePath = path;
        this.startPoint = new LatLng(start.getLatitude(), start.getLongitude());
        this.endPoint = new LatLng(end.getLatitude(), end.getLongitude());
    }

    public void addToMap() {
        try {
            if (mAMap == null || ridePath == null) {
                return;
            }
            List<RideStep> rideSteps = ridePath.getSteps();
            for (RideStep step : rideSteps) {
                List<LatLonPoint> latLonPoints = step.getPolyline();
                if (latLonPoints == null) {
                    continue;
                }
                PolylineOptions options = new PolylineOptions();
                options.color(getDriveColor());
                options.width(getRouteWidth());
                for (LatLonPoint point : latLonPoints) {
                    options.add(new LatLng(point.getLatitude(), point.getLongitude()));
                }
                addPolyLine(options);

                // Add station markers
                if (nodeIconVisible) {
                    addStationMarker(new MarkerOptions()
                        .position(new LatLng(latLonPoints.get(0).getLatitude(), latLonPoints.get(0).getLongitude()))
                        .icon(getDriveBitmapDescriptor()));
                }
            }
            addStartAndEndMarker();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

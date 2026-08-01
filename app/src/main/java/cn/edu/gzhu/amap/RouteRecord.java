package cn.edu.gzhu.amap;

import com.amap.api.services.core.LatLonPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 路线记录数据类
 * 用于保存和加载收藏/历史路线
 */
public class RouteRecord {
    private String id;
    private String name;
    private List<WaypointData> waypoints;
    private long createdAt;
    private boolean isFavorite;
    private int routeType; // 0-驾车, 1-步行, 2-骑行, 3-公交

    public RouteRecord() {
        this.waypoints = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.id = String.valueOf(createdAt);
    }

    public RouteRecord(String name, List<RouteRecommendationActivity.Waypoint> waypointList, int routeType) {
        this();
        this.name = name;
        this.routeType = routeType;
        for (RouteRecommendationActivity.Waypoint wp : waypointList) {
            this.waypoints.add(new WaypointData(wp.getName(),
                    wp.getPoint().getLatitude(),
                    wp.getPoint().getLongitude()));
        }
    }

    // 内部类：用于序列化途径点数据
    public static class WaypointData {
        public String name;
        public double latitude;
        public double longitude;

        public WaypointData() {
        }

        public WaypointData(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public LatLonPoint toLatLonPoint() {
            return new LatLonPoint(latitude, longitude);
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WaypointData> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<WaypointData> waypoints) {
        this.waypoints = waypoints;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public int getRouteType() {
        return routeType;
    }

    public void setRouteType(int routeType) {
        this.routeType = routeType;
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    /**
     * 获取路线类型的中文描述
     */
    public String getRouteTypeDescription() {
        switch (routeType) {
            case 0:
                return "驾车";
            case 1:
                return "步行";
            case 2:
                return "骑行";
            case 3:
                return "公交";
            case 4:
                return "货车";
            case 5:
                return "电瓶车";
            default:
                return "未知";
        }
    }
}

package cn.edu.gzhu.amap;

import com.amap.api.services.core.LatLonPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * TSP (Traveling Salesperson Problem) 路径优化器
 * <p>
 * <b>核心功能：</b>
 * 该类用于解决多途径点的路径排序问题。在给定的 N 个地点中，寻找一条闭合或非闭合的路径，
 * 使得访问所有地点的总距离最短。
 * <p>
 * <b>算法模型：最近邻算法 (Nearest Neighbor Algorithm)</b>
 * <br>
 * 由于 TSP 问题属于 NP-Hard 问题，在移动端需考虑计算性能与实时响应能力。
 * 本项目采用启发式搜索中的“贪心策略”：
 * <ol>
 * <li>从起点开始，将其标记为“当前点”。</li>
 * <li>在所有尚未访问的点中，计算与“当前点”距离最近的点。</li>
 * <li>将该最近点加入路径，并将其更新为新的“当前点”。</li>
 * <li>重复步骤 2-3，直到所有点都被访问。</li>
 * </ol>
 * <p>
 * <b>时间复杂度：</b> O(N^2)，适用于移动端中小规模（N < 50）的点位排序。
 */
public class TspOptimizer {

    /**
     * 执行路径优化
     *
     * @param waypoints 原始途径点列表（包含用户添加的无序点）
     * @return 优化后的途径点列表（按访问顺序排序）
     */
    public static List<RouteRecommendationActivity.Waypoint> optimize(
            List<RouteRecommendationActivity.Waypoint> waypoints) {

        if (waypoints == null || waypoints.size() <= 2) {
            return waypoints; // 2个及以下的点无需优化
        }

        // 1. 提取已勾选的途径点（预处理）
        List<RouteRecommendationActivity.Waypoint> checkedWaypoints = new ArrayList<>();
        for (RouteRecommendationActivity.Waypoint wp : waypoints) {
            if (wp.isChecked()) {
                checkedWaypoints.add(wp);
            }
        }

        if (checkedWaypoints.size() <= 2) {
            return waypoints; // 勾选的点不足，无需优化
        }

        // 2. 核心算法调用：使用最近邻算法优化顺序
        List<RouteRecommendationActivity.Waypoint> optimized = nearestNeighbor(checkedWaypoints);

        // 3. 结果重建：保留未勾选点的位置不变，将已勾选部分替换为优化后的顺序
        List<RouteRecommendationActivity.Waypoint> result = new ArrayList<>();
        int optimizedIndex = 0;
        for (RouteRecommendationActivity.Waypoint wp : waypoints) {
            if (wp.isChecked()) {
                result.add(optimized.get(optimizedIndex++));
            } else {
                result.add(wp);
            }
        }

        return result;
    }

    /**
     * 最近邻算法 (Nearest Neighbor Implementation)
     * <p>
     * 策略：贪心策略 (Greedy Strategy)
     * 始终选择局部最优解（即离当前点最近的下一个点），虽然不一定保证全局最优，
     * 但在地理路径规划场景下通常能获得非常实用的结果。
     *
     * @param waypoints 待排序的点集
     * @return 排序后的点集
     */
    private static List<RouteRecommendationActivity.Waypoint> nearestNeighbor(
            List<RouteRecommendationActivity.Waypoint> waypoints) {

        List<RouteRecommendationActivity.Waypoint> result = new ArrayList<>();
        List<RouteRecommendationActivity.Waypoint> remaining = new ArrayList<>(waypoints);

        // 步骤 1: 确立起点 (通常列表第一个点为用户设定的起始位置，固定不变)
        RouteRecommendationActivity.Waypoint current = remaining.remove(0);
        result.add(current);

        // 步骤 2: 迭代寻找最近邻居
        while (!remaining.isEmpty()) {
            RouteRecommendationActivity.Waypoint nearest = null;
            double minDistance = Double.MAX_VALUE;

            // 遍历剩余集合，寻找距离最小的候选点
            for (RouteRecommendationActivity.Waypoint candidate : remaining) {
                double distance = calculateDistance(current.getPoint(), candidate.getPoint());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = candidate;
                }
            }

            // 步骤 3: 状态更新
            if (nearest != null) {
                result.add(nearest); // 加入结果集
                remaining.remove(nearest); // 从剩余集中移除
                current = nearest; // 移动到该点，作为下一次查找的起点
            }
        }

        return result;
    }

    /**
     * 计算两点之间的球面距离
     * <p>
     * <b>数学模型：Haversine 公式</b>
     * <br>
     * 用于计算球面上任意两点间的大圆距离 (Great-circle distance)。
     * 公式如下：
     * 
     * <pre>
     * a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
     * c = 2 ⋅ atan2( √a, √(1−a) )
     * d = R ⋅ c
     * </pre>
     * 
     * 其中：
     * <ul>
     * <li>φ (phi): 纬度 (弧度制)</li>
     * <li>λ (lambda): 经度 (弧度制)</li>
     * <li>R: 地球平均半径 (约 6371km)</li>
     * </ul>
     *
     * @param p1 点A的经纬度
     * @param p2 点B的经纬度
     * @return 两点间的物理距离，单位：米
     */
    private static double calculateDistance(LatLonPoint p1, LatLonPoint p2) {
        final double R = 6371000; // 地球半径（米）

        // 将角度转换为弧度
        double lat1 = Math.toRadians(p1.getLatitude()); // φ1
        double lat2 = Math.toRadians(p2.getLatitude()); // φ2
        double deltaLat = Math.toRadians(p2.getLatitude() - p1.getLatitude()); // Δφ
        double deltaLon = Math.toRadians(p2.getLongitude() - p1.getLongitude()); // Δλ

        // 计算 Haversine 函数值 a
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        // 计算角距离 c (弧度)
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 计算物理距离 d
        return R * c;
    }

    /**
     * 计算路径总长度
     * 
     * @param waypoints 途径点列表
     * @return 总距离（米）
     */
    public static double calculateTotalDistance(List<RouteRecommendationActivity.Waypoint> waypoints) {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            // 仅计算已勾选点之间的连线距离
            if (waypoints.get(i).isChecked() && waypoints.get(i + 1).isChecked()) {
                total += calculateDistance(
                        waypoints.get(i).getPoint(),
                        waypoints.get(i + 1).getPoint());
            }
        }
        return total;
    }
}

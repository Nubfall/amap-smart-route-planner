package cn.edu.gzhu.amap;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 路线存储工具类
 * 使用 SharedPreferences + Gson 存储路线数据
 */
public class RouteStorageHelper {
    private static final String PREF_NAME = "route_history";
    private static final String KEY_ROUTES = "routes";

    private final SharedPreferences prefs;
    private final Gson gson;

    public RouteStorageHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * 保存路线记录
     */
    public void saveRoute(RouteRecord record) {
        List<RouteRecord> routes = getAllRoutes();
        // 检查是否已存在相同ID的记录
        routes.removeIf(r -> r.getId().equals(record.getId()));
        routes.add(0, record); // 添加到列表开头
        saveAllRoutes(routes);
    }

    /**
     * 获取所有路线记录
     */
    public List<RouteRecord> getAllRoutes() {
        String json = prefs.getString(KEY_ROUTES, "[]");
        Type type = new TypeToken<List<RouteRecord>>() {
        }.getType();
        List<RouteRecord> routes = gson.fromJson(json, type);
        return routes != null ? routes : new ArrayList<>();
    }

    /**
     * 删除路线记录
     */
    public void deleteRoute(String routeId) {
        List<RouteRecord> routes = getAllRoutes();
        routes.removeIf(r -> r.getId().equals(routeId));
        saveAllRoutes(routes);
    }

    /**
     * 切换收藏状态
     */
    public void toggleFavorite(String routeId) {
        List<RouteRecord> routes = getAllRoutes();
        for (RouteRecord route : routes) {
            if (route.getId().equals(routeId)) {
                route.setFavorite(!route.isFavorite());
                break;
            }
        }
        saveAllRoutes(routes);
    }

    /**
     * 重命名路线
     */
    public void renameRoute(String routeId, String newName) {
        List<RouteRecord> routes = getAllRoutes();
        for (RouteRecord route : routes) {
            if (route.getId().equals(routeId)) {
                route.setName(newName);
                break;
            }
        }
        saveAllRoutes(routes);
    }

    /**
     * 获取收藏的路线
     */
    public List<RouteRecord> getFavoriteRoutes() {
        List<RouteRecord> allRoutes = getAllRoutes();
        List<RouteRecord> favorites = new ArrayList<>();
        for (RouteRecord route : allRoutes) {
            if (route.isFavorite()) {
                favorites.add(route);
            }
        }
        return favorites;
    }

    /**
     * 清空所有记录
     */
    public void clearAll() {
        prefs.edit().remove(KEY_ROUTES).apply();
    }

    private void saveAllRoutes(List<RouteRecord> routes) {
        String json = gson.toJson(routes);
        prefs.edit().putString(KEY_ROUTES, json).apply();
    }
}

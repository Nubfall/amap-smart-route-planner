package cn.edu.gzhu.amap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 路线历史记录适配器
 */
public class RouteHistoryAdapter extends RecyclerView.Adapter<RouteHistoryAdapter.ViewHolder> {

    private final List<RouteRecord> routes;
    private final OnRouteInteractionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public interface OnRouteInteractionListener {
        void onRouteClick(RouteRecord route);

        void onMoreClick(View anchor, RouteRecord route, int position);

        void onFavoriteToggle(RouteRecord route, int position);
    }

    public RouteHistoryAdapter(List<RouteRecord> routes, OnRouteInteractionListener listener) {
        this.routes = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteRecord route = routes.get(position);

        holder.routeName.setText(route.getName());
        holder.routeType.setText(route.getRouteTypeDescription());
        holder.waypointCount.setText(route.getWaypointCount() + "个途径点");
        holder.createdTime.setText(dateFormat.format(new Date(route.getCreatedAt())));

        // 收藏状态
        holder.favoriteIcon.setAlpha(route.isFavorite() ? 1.0f : 0.3f);

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteClick(route);
            }
        });

        // 收藏按钮
        holder.favoriteIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteToggle(route, holder.getAdapterPosition());
            }
        });

        // 更多操作按钮
        holder.moreButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick(v, route, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView favoriteIcon;
        TextView routeName;
        TextView routeType;
        TextView waypointCount;
        TextView createdTime;
        ImageView moreButton;

        public ViewHolder(View view) {
            super(view);
            favoriteIcon = view.findViewById(R.id.item_favorite_icon);
            routeName = view.findViewById(R.id.item_route_name);
            routeType = view.findViewById(R.id.item_route_type);
            waypointCount = view.findViewById(R.id.item_waypoint_count);
            createdTime = view.findViewById(R.id.item_created_time);
            moreButton = view.findViewById(R.id.item_more_button);
        }
    }
}

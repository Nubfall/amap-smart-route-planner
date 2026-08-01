package cn.edu.gzhu.amap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WaypointsAdapter extends RecyclerView.Adapter<WaypointsAdapter.ViewHolder> {

    private final List<RouteRecommendationActivity.Waypoint> waypoints;
    private final OnWaypointInteractionListener interactionListener;

    public interface OnWaypointInteractionListener {
        void onDeleteRequest(int position);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public WaypointsAdapter(List<RouteRecommendationActivity.Waypoint> waypoints, OnWaypointInteractionListener listener) {
        this.waypoints = waypoints;
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waypoint, parent, false);
        return new ViewHolder(view, interactionListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RouteRecommendationActivity.Waypoint waypoint = waypoints.get(position);
        holder.waypointName.setText(waypoint.getName());

        // 1. 先移除监听器，避免在程序化设置选中状态时触发不必要的回调
        holder.waypointCheckbox.setOnCheckedChangeListener(null);

        // 2. 根据数据模型的状态，同步更新UI（复选框）
        holder.waypointCheckbox.setChecked(waypoint.isChecked());

        // 3. 重新设置监听器，以便在用户点击复选框时，能及时更新数据模型
        holder.waypointCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            waypoint.setChecked(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return waypoints.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CheckBox waypointCheckbox;
        public TextView waypointName;
        public ImageView dragHandle;

        public ViewHolder(View view, final OnWaypointInteractionListener listener) {
            super(view);
            waypointCheckbox = view.findViewById(R.id.waypoint_checkbox);
            waypointName = view.findViewById(R.id.waypoint_name);
            dragHandle = view.findViewById(R.id.drag_handle);

            dragHandle.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteRequest(position);
                    }
                }
            });

            dragHandle.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onStartDrag(this);
                }
                return true; // 返回true，消费长按事件，避免触发单击
            });
        }
    }
}

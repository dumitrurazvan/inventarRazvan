package ro.cnpr.inventar.ui.location;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ro.cnpr.inventar.R;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    public interface OnLocationClickListener {
        void onLocationClick(LocationWithRoomCount location);
    }

    private final List<LocationWithRoomCount> locations;
    private final OnLocationClickListener listener;

    public LocationAdapter(List<LocationWithRoomCount> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        LocationWithRoomCount location = locations.get(position);
        holder.bind(location, listener);
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {

        private final TextView locationNameTextView;
        private final TextView roomCountTextView;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            locationNameTextView = itemView.findViewById(R.id.location_name_text_view);
            roomCountTextView = itemView.findViewById(R.id.location_room_count_text_view);
        }

        public void bind(LocationWithRoomCount location, OnLocationClickListener listener) {
            locationNameTextView.setText(location.getLocationName());
            roomCountTextView.setText(String.format("%d rooms", location.getRoomCount()));
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLocationClick(location);
                }
            });
        }
    }
}

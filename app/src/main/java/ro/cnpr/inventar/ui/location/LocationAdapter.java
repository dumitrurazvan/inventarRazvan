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
        void onLocationClick(String location);
    }

    private final List<String> locations;
    private final OnLocationClickListener listener;

    public LocationAdapter(List<String> locations, OnLocationClickListener listener) {
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
        String location = locations.get(position);
        holder.bind(location, listener);
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvLocationName;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
        }

        public void bind(String location, OnLocationClickListener listener) {
            tvLocationName.setText(location);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLocationClick(location);
                }
            });
        }
    }
}

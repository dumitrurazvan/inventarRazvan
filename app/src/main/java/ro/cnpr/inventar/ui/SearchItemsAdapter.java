package ro.cnpr.inventar.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.AssetDto;

public class SearchItemsAdapter extends RecyclerView.Adapter<SearchItemsAdapter.ViewHolder> {

    private final List<AssetDto> items;

    public SearchItemsAdapter(List<AssetDto> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetDto item = items.get(position);
        holder.tvNrInventar.setText(item.getNrInventar());
        holder.tvDenumire.setText(item.getDenumireObiect());
        holder.tvLocatie.setText(item.getRoomDisplayName());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNrInventar;
        TextView tvDenumire;
        TextView tvLocatie;

        ViewHolder(View view) {
            super(view);
            tvNrInventar = view.findViewById(R.id.tvNrInventar);
            tvDenumire = view.findViewById(R.id.tvDenumire);
            tvLocatie = view.findViewById(R.id.tvLocatie);
        }
    }
}

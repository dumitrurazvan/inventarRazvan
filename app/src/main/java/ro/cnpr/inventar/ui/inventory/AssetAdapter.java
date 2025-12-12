package ro.cnpr.inventar.ui.inventory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Set;

import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.AssetDto;

public class AssetAdapter extends RecyclerView.Adapter<AssetAdapter.AssetViewHolder> {

    public interface OnSelectionChangedListener {
        void onAssetSelectionChanged(AssetDto asset, boolean selected);
    }

    public interface OnValidateClickListener {
        void onValidateClick(AssetDto asset);
    }

    public interface OnAssetClickListener {
        void onAssetClick(AssetDto asset);
    }

    private final List<AssetDto> assets;
    private final Set<Long> selectedAssetIds;
    private final String roomDisplayName;
    private final OnSelectionChangedListener selectionListener;
    private final OnValidateClickListener validateClickListener;
    private final OnAssetClickListener assetClickListener;

    public AssetAdapter(List<AssetDto> assets,
                        Set<Long> selectedAssetIds,
                        String roomDisplayName,
                        OnSelectionChangedListener selectionListener,
                        OnValidateClickListener validateClickListener,
                        OnAssetClickListener assetClickListener) {
        this.assets = assets;
        this.selectedAssetIds = selectedAssetIds;
        this.roomDisplayName = roomDisplayName;
        this.selectionListener = selectionListener;
        this.validateClickListener = validateClickListener;
        this.assetClickListener = assetClickListener;
    }

    @NonNull
    @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asset, parent, false);
        return new AssetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        AssetDto asset = assets.get(position);
        holder.bind(asset, selectedAssetIds, roomDisplayName, selectionListener, validateClickListener, assetClickListener);
    }

    @Override
    public int getItemCount() {
        return assets != null ? assets.size() : 0;
    }

    static class AssetViewHolder extends RecyclerView.ViewHolder {

        private final View viewStatusBar;
        private final View assetItemLayout;
        private final TextView tvTitle;
        private final TextView tvNrInventar;
        private final TextView tvSubtitle;
        private final TextView tvOwner;
        private final CheckBox cbAsset;
        private final Button btnValidate;

        public AssetViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
            assetItemLayout = itemView.findViewById(R.id.asset_item_layout);
            tvTitle = itemView.findViewById(R.id.tvAssetTitle);
            tvNrInventar = itemView.findViewById(R.id.tvAssetNrInventar);
            tvSubtitle = itemView.findViewById(R.id.tvAssetSubtitle);
            tvOwner = itemView.findViewById(R.id.tvAssetOwner);
            cbAsset = itemView.findViewById(R.id.cbAsset);
            btnValidate = itemView.findViewById(R.id.btnValidate);
        }

        public void bind(AssetDto asset,
                         Set<Long> selectedAssetIds,
                         String roomDisplayName,
                         OnSelectionChangedListener selectionListener,
                         OnValidateClickListener validateClickListener,
                         OnAssetClickListener assetClickListener) {

            String title = notEmpty(asset.getCaracteristiciObiect())
                    ? asset.getCaracteristiciObiect()
                    : asset.getDenumireObiect();
            if (!notEmpty(title)) {
                title = "(fără denumire)";
            }
            tvTitle.setText(title);

            tvNrInventar.setText(asset.getNrInventar());
            tvSubtitle.setText(roomDisplayName);

            String owner = asset.getGestionarActual();
            tvOwner.setText(notEmpty(owner) ? owner : "");

            boolean identified = asset.isIdentified();

            cbAsset.setOnCheckedChangeListener(null);
            if (identified) {
                cbAsset.setChecked(true);
                cbAsset.setEnabled(false);
            } else {
                cbAsset.setEnabled(true);
                boolean selected = selectedAssetIds.contains(asset.getId());
                cbAsset.setChecked(selected);
            }

            cbAsset.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!identified && selectionListener != null) {
                    selectionListener.onAssetSelectionChanged(asset, isChecked);
                }
            });

            if (identified) {
                viewStatusBar.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark));
                assetItemLayout.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_light));
            } else {
                viewStatusBar.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                assetItemLayout.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.item_background));
            }

            btnValidate.setEnabled(true);
            if (identified) {
                btnValidate.setText("Validat");
            } else {
                btnValidate.setText("Validează și printează");
            }
            btnValidate.setOnClickListener(v -> {
                if (validateClickListener != null) {
                    validateClickListener.onValidateClick(asset);
                }
            });

            itemView.setOnClickListener(v -> {
                if (assetClickListener != null) {
                    assetClickListener.onAssetClick(asset);
                }
            });
        }

        private boolean notEmpty(String s) {
            return s != null && s.trim().length() > 0;
        }
    }
}

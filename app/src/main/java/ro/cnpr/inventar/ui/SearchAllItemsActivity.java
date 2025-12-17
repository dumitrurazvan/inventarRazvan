package ro.cnpr.inventar.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.AssetDto;
import ro.cnpr.inventar.model.AssetUpdateRequest;
import ro.cnpr.inventar.network.ApiClient;
import ro.cnpr.inventar.network.ApiService;
import ro.cnpr.inventar.prefs.PrefsManager;
import ro.cnpr.inventar.print.PrinterHelper;
import ro.cnpr.inventar.ui.asset.AssetDetailActivity;
import ro.cnpr.inventar.ui.inventory.AssetAdapter;

public class SearchAllItemsActivity extends AppCompatActivity {

    private static final int REQ_ASSET_DETAIL = 1003;

    private EditText etSearchNrInv;
    private Button btnSearch;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBarSearch;
    private TextView tvSearchStatus;

    private AssetAdapter adapter;
    private final List<AssetDto> searchResults = new ArrayList<>();
    private final Set<Long> selectedAssetIds = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_all_items);

        etSearchNrInv = findViewById(R.id.etSearchNrInv);
        btnSearch = findViewById(R.id.btnSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssetAdapter(
                searchResults,
                selectedAssetIds,
                null,
                (asset, selected) -> {
                    if (selected) {
                        selectedAssetIds.add(asset.getId());
                    } else {
                        selectedAssetIds.remove(asset.getId());
                    }
                },
                this::onValidateButtonClicked,
                this::openAssetDetails
        );
        rvSearchResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String query = etSearchNrInv.getText().toString();
            if (!TextUtils.isEmpty(query)) {
                search(query);
            }
        });
    }

    private void search(String query) {
        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this, "IP/port lipsă. Revino la ecranul de conectare.", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService apiService = ApiClient.create("http://" + ip + ":" + port + "/api/").create(ApiService.class);

        progressBarSearch.setVisibility(View.VISIBLE);
        tvSearchStatus.setVisibility(View.GONE);

        apiService.searchAssets(query).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                progressBarSearch.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    AssetDto asset = response.body();
                    searchResults.clear();
                    searchResults.add(asset);
                    adapter.notifyDataSetChanged();
                    tvSearchStatus.setVisibility(View.GONE);
                } else {
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                    tvSearchStatus.setVisibility(View.VISIBLE);
                    Toast.makeText(SearchAllItemsActivity.this, "Eroare server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                progressBarSearch.setVisibility(View.GONE);
                searchResults.clear();
                adapter.notifyDataSetChanged();
                tvSearchStatus.setVisibility(View.VISIBLE);
                Toast.makeText(SearchAllItemsActivity.this, "Eroare rețea: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onValidateButtonClicked(AssetDto asset) {
        if (asset == null) return;

        if (!asset.isIdentified()) {
            validateAsset(asset);
        } else {
            confirmInvalidateAsset(asset);
        }
    }

    private void validateAsset(AssetDto asset) {
        String nrInv = asset.getNrInventar();
        if (TextUtils.isEmpty(nrInv)) {
            Toast.makeText(this, "Nu există număr de inventar. Nu se poate valida.", Toast.LENGTH_LONG).show();
            return;
        }

        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this, "IP/port lipsă. Revino la ecranul de conectare.", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService apiService = ApiClient.create("http://" + ip + ":" + port + "/api/").create(ApiService.class);

        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setIdentified(Boolean.TRUE);
        req.setLocatia(asset.getLocatia());
        req.setCamera(asset.getCamera());

        progressBarSearch.setVisibility(View.VISIBLE);

        apiService.updateAssetByNr(nrInv, req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                progressBarSearch.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    Toast.makeText(SearchAllItemsActivity.this, "Eroare la validare: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    Toast.makeText(SearchAllItemsActivity.this, "Serverul a răspuns fără date.", Toast.LENGTH_LONG).show();
                    return;
                }

                applyUpdatedAsset(updated);
                Toast.makeText(SearchAllItemsActivity.this, "Validare reușită pentru " + nrInv, Toast.LENGTH_SHORT).show();

                String labelTitle = buildLabelTitle(updated);
                PrinterHelper.printLabel(
                        SearchAllItemsActivity.this,
                        updated.getNrInventar(),
                        labelTitle,
                        updated.getRoomDisplayName(),
                        updated.getGestionarActual()
                );
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                progressBarSearch.setVisibility(View.GONE);
                Toast.makeText(SearchAllItemsActivity.this, "Nu s-a putut valida bunul: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmInvalidateAsset(AssetDto asset) {
        String nrInv = asset.getNrInventar();
        new AlertDialog.Builder(this)
                .setTitle("Invalidează bunul?")
                .setMessage("Ești sigur că vrei să marchezi acest bun ca NEIDENTIFICAT?\n\nNr inventar: " + nrInv)
                .setPositiveButton("Da, invalidează", (dialog, which) -> invalidateAsset(asset))
                .setNegativeButton("Renunță", null)
                .show();
    }

    private void invalidateAsset(AssetDto asset) {
        String nrInv = asset.getNrInventar();
        if (TextUtils.isEmpty(nrInv)) {
            Toast.makeText(this, "Nu există număr de inventar. Nu se poate invalida.", Toast.LENGTH_LONG).show();
            return;
        }

        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this, "IP/port lipsă. Revino la ecranul de conectare.", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService apiService = ApiClient.create("http://" + ip + ":" + port + "/api/").create(ApiService.class);

        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setIdentified(Boolean.FALSE);

        progressBarSearch.setVisibility(View.VISIBLE);

        apiService.updateAssetByNr(nrInv, req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                progressBarSearch.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    Toast.makeText(SearchAllItemsActivity.this, "Eroare la invalidare: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    Toast.makeText(SearchAllItemsActivity.this, "Serverul a răspuns fără date.", Toast.LENGTH_LONG).show();
                    return;
                }

                applyUpdatedAsset(updated);
                Toast.makeText(SearchAllItemsActivity.this, "Invalidare reușită pentru " + nrInv, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                progressBarSearch.setVisibility(View.GONE);
                Toast.makeText(SearchAllItemsActivity.this, "Nu s-a putut invalida bunul: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyUpdatedAsset(AssetDto updated) {
        for (int i = 0; i < searchResults.size(); i++) {
            AssetDto current = searchResults.get(i);
            if (current.getId() == updated.getId()) {
                searchResults.set(i, updated);
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String buildLabelTitle(AssetDto asset) {
        if (asset == null) return "";
        String title = asset.getCaracteristiciObiect();
        if (title == null || title.trim().isEmpty()) {
            title = asset.getDenumireObiect();
        }
        if (title == null || title.trim().isEmpty()) {
            title = "";
        }
        return title;
    }

    private void openAssetDetails(AssetDto asset) {
        if (asset == null) return;
        Intent intent = new Intent(this, AssetDetailActivity.class);
        intent.putExtra(AssetDetailActivity.EXTRA_ASSET, asset);
        startActivityForResult(intent, REQ_ASSET_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ASSET_DETAIL && resultCode == RESULT_OK && data != null) {
            boolean deleted = data.getBooleanExtra(AssetDetailActivity.EXTRA_RESULT_DELETED, false);
            AssetDto resultAsset = (AssetDto) data.getSerializableExtra(AssetDetailActivity.EXTRA_RESULT_ASSET);
            if (resultAsset == null) {
                return;
            }

            if (deleted || !resultAsset.isActive()) {
                removeAssetFromList(resultAsset);
            } else {
                applyUpdatedAsset(resultAsset);
            }
        }
    }

    private void removeAssetFromList(AssetDto asset) {
        long id = asset.getId();
        for (int i = 0; i < searchResults.size(); i++) {
            if (searchResults.get(i).getId() == id) {
                searchResults.remove(i);
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }
}

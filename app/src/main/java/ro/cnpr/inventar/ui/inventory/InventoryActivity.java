package ro.cnpr.inventar.ui.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.AssetDto;
import ro.cnpr.inventar.model.AssetUpdateRequest;
import ro.cnpr.inventar.network.ApiClient;
import ro.cnpr.inventar.network.ApiService;
import ro.cnpr.inventar.prefs.PrefsManager;
import ro.cnpr.inventar.print.PrinterHelper;
import ro.cnpr.inventar.ui.asset.AssetDetailActivity;

public class InventoryActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "extra_room_id";
    public static final String EXTRA_LOCATIE = "extra_locatie";
    public static final String EXTRA_CAMERA = "extra_camera";
    public static final String EXTRA_ROOM_DISPLAY_NAME = "extra_room_display_name";

    private static final int REQ_ASSET_DETAIL = 1001;

    private TextView tvRoomInfo;
    private TextView tvStatus;
    private TextView tvCounts;
    private ProgressBar progressBar;
    private RecyclerView rvAssets;
    private EditText etSearch;
    private Button btnPrintSelected;
    private CheckBox cbOnlyUnidentified;
    private final List<AssetDto> allAssets = new ArrayList<>();
    private final List<AssetDto> assets = new ArrayList<>();
    private final Set<Long> selectedAssetIds = new HashSet<>();
    private AssetAdapter adapter;
    private long roomId;
    private String locatie;
    private String camera;
    private String roomDisplayName;
    private String currentSearchQuery = "";
    private boolean showOnlyUnidentified = false;
    private boolean batchInProgress = false;

    private final Comparator<AssetDto> assetComparator = new Comparator<AssetDto>() {
        @Override
        public int compare(AssetDto a1, AssetDto a2) {
            int cmpIdent = Boolean.compare(a1.isIdentified(), a2.isIdentified());
            if (cmpIdent != 0) {
                return cmpIdent;
            }
            String k1 = sortKey(a1);
            String k2 = sortKey(a2);
            return k1.compareToIgnoreCase(k2);
        }

        private String sortKey(AssetDto a) {
            if (!isEmpty(a.getNrInventar())) {
                return a.getNrInventar();
            }
            if (!isEmpty(a.getNrCrt())) {
                return a.getNrCrt();
            }
            return String.valueOf(a.getId());
        }

        private boolean isEmpty(String s) {
            return s == null || s.trim().isEmpty();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        TextView tvTitle = findViewById(R.id.tvInventoryTitle);
        tvRoomInfo = findViewById(R.id.tvInventoryRoomInfo);
        tvStatus = findViewById(R.id.tvInventoryStatus);
        tvCounts = findViewById(R.id.tvInventoryCounts);
        progressBar = findViewById(R.id.progressBarInventory);
        rvAssets = findViewById(R.id.rvAssets);
        etSearch = findViewById(R.id.etSearchNrInv);
        btnPrintSelected = findViewById(R.id.btnPrintSelected);
        cbOnlyUnidentified = findViewById(R.id.cbOnlyUnidentified);

        roomId = getIntent().getLongExtra(EXTRA_ROOM_ID, -1L);
        locatie = getIntent().getStringExtra(EXTRA_LOCATIE);
        camera = getIntent().getStringExtra(EXTRA_CAMERA);
        roomDisplayName = getIntent().getStringExtra(EXTRA_ROOM_DISPLAY_NAME);

        if (locatie == null) locatie = "-";
        if (camera == null) camera = "-";
        if (roomDisplayName == null) {
            roomDisplayName = locatie + " - " + camera;
        }

        tvTitle.setText("Inventar cameră");
        String info = "Room ID: " + roomId + "   |   " + roomDisplayName;
        tvRoomInfo.setText(info);

        rvAssets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssetAdapter(
                assets,
                selectedAssetIds,
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
        rvAssets.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString() : "";
                applyFilter();
            }
        });

        cbOnlyUnidentified.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showOnlyUnidentified = isChecked;
            applyFilter();
        });

        btnPrintSelected.setOnClickListener(v -> startBatchPrintSelected());

        loadAssetsForRoom();
    }

    private void loadAssetsForRoom() {
        if (roomId <= 0) {
            tvStatus.setText("Room ID invalid.");
            Toast.makeText(this, "Room ID invalid", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            tvStatus.setText("Configurare server lipsă.");
            Toast.makeText(this,
                    "IP/port lipsă. Revino la ecranul de conectare.",
                    Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        String baseUrl = "http://" + ip + ":" + port + "/api/";
        Retrofit retrofit = ApiClient.create(baseUrl);
        ApiService apiService = retrofit.create(ApiService.class);

        tvStatus.setText("Se încarcă bunurile...");
        progressBar.setVisibility(View.VISIBLE);

        apiService.getAssetsInRoom(roomId).enqueue(new Callback<List<AssetDto>>() {
            @Override
            public void onResponse(Call<List<AssetDto>> call, Response<List<AssetDto>> response) {
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    tvStatus.setText("Eroare la /rooms/" + roomId + "/assets (cod " + response.code() + ")");
                    Toast.makeText(InventoryActivity.this,
                            "Eroare server: " + response.code(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                List<AssetDto> list = response.body();
                if (list == null || list.isEmpty()) {
                    tvStatus.setText("Nu există bunuri active în această cameră.");
                    allAssets.clear();
                    assets.clear();
                    selectedAssetIds.clear();
                    adapter.notifyDataSetChanged();
                    updateCounts();
                    return;
                }

                List<AssetDto> activeAssets = new ArrayList<>();
                for (AssetDto a : list) {
                    if (a.isActive()) {
                        activeAssets.add(a);
                    }
                }

                Collections.sort(activeAssets, assetComparator);

                allAssets.clear();
                allAssets.addAll(activeAssets);
                selectedAssetIds.clear();
                currentSearchQuery = etSearch.getText() != null
                        ? etSearch.getText().toString()
                        : "";
                applyFilter();
            }

            @Override
            public void onFailure(Call<List<AssetDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("Eroare de rețea la încărcarea bunurilor");
                Toast.makeText(InventoryActivity.this,
                        "Nu s-au putut încărca bunurile: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilter() {
        assets.clear();

        String q = null;
        if (!TextUtils.isEmpty(currentSearchQuery)) {
            q = currentSearchQuery.trim().toUpperCase(Locale.ROOT);
        }

        if (q == null && !showOnlyUnidentified) {
            assets.addAll(allAssets);
            tvStatus.setText("Bunuri încărcate: " + assets.size()
                    + " (neidentificate primele)");
        } else {
            for (AssetDto a : allAssets) {
                if (showOnlyUnidentified && a.isIdentified()) {
                    continue;
                }

                if (q != null) {
                    String nr = a.getNrInventar();
                    String crt = a.getNrCrt();
                    boolean matchNr = nr != null
                            && nr.toUpperCase(Locale.ROOT).contains(q);
                    boolean matchCrt = crt != null
                            && crt.toUpperCase(Locale.ROOT).contains(q);
                    if (!matchNr && !matchCrt) {
                        continue;
                    }
                }

                assets.add(a);
            }

            String base = "Filtru: " + assets.size()
                    + " rezultate (din " + allAssets.size() + ")";
            if (showOnlyUnidentified) {
                base += " | doar neidentificate";
            }
            tvStatus.setText(base);
        }

        adapter.notifyDataSetChanged();
        updateCounts();
    }

    private void updateCounts() {
        int total = allAssets.size();
        int identifiedCount = 0;
        for (AssetDto a : allAssets) {
            if (a.isIdentified()) {
                identifiedCount++;
            }
        }
        int remaining = total - identifiedCount;
        tvCounts.setText("Identificate: " + identifiedCount + " / " + total
                + "  |  Restant: " + remaining);
    }

    private void onValidateButtonClicked(AssetDto asset) {
        if (asset == null) return;
        if (batchInProgress) {
            Toast.makeText(this,
                    "Nu poți modifica starea în timp ce rulează batch-ul.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!asset.isIdentified()) {
            validateAsset(asset);
        } else {
            confirmInvalidateAsset(asset);
        }
    }

    private void validateAsset(AssetDto asset) {
        String nrInv = asset.getNrInventar();
        if (TextUtils.isEmpty(nrInv)) {
            Toast.makeText(this,
                    "Nu există număr de inventar. Nu se poate valida.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this,
                    "IP/port lipsă. Revino la ecranul de conectare.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String baseUrl = "http://" + ip + ":" + port + "/api/";
        Retrofit retrofit = ApiClient.create(baseUrl);
        ApiService apiService = retrofit.create(ApiService.class);

        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setIdentified(Boolean.TRUE);
        req.setLocatia(locatie);
        req.setCamera(camera);

        tvStatus.setText("Se validează bunul " + nrInv + "...");
        progressBar.setVisibility(View.VISIBLE);

        apiService.updateAssetByNr(nrInv, req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    tvStatus.setText("Eroare la validare (cod " + response.code() + ")");
                    Toast.makeText(InventoryActivity.this,
                            "Eroare la validare: " + response.code(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    tvStatus.setText("Răspuns gol de la server.");
                    Toast.makeText(InventoryActivity.this,
                            "Serverul a răspuns fără date.", Toast.LENGTH_LONG).show();
                    return;
                }

                applyUpdatedAsset(updated);
                tvStatus.setText("Bunul " + nrInv + " a fost marcat identificat.");
                Toast.makeText(InventoryActivity.this,
                        "Validare reușită pentru " + nrInv,
                        Toast.LENGTH_SHORT).show();

                String labelTitle = buildLabelTitle(updated);
                String locCam = locatie + " - " + camera;
                boolean printOk = PrinterHelper.printLabel(
                        InventoryActivity.this,
                        updated.getNrInventar(),
                        labelTitle,
                        locCam
                );
                if (!printOk) {
                    Toast.makeText(InventoryActivity.this,
                            "Tipărire eșuată, dar bunul este marcat identificat în sistem.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("Eroare de rețea la validare");
                Toast.makeText(InventoryActivity.this,
                        "Nu s-a putut valida bunul: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
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
            Toast.makeText(this,
                    "Nu există număr de inventar. Nu se poate invalida.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this,
                    "IP/port lipsă. Revino la ecranul de conectare.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String baseUrl = "http://" + ip + ":" + port + "/api/";
        Retrofit retrofit = ApiClient.create(baseUrl);
        ApiService apiService = retrofit.create(ApiService.class);

        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setIdentified(Boolean.FALSE);

        tvStatus.setText("Se invalidează bunul " + nrInv + "...");
        progressBar.setVisibility(View.VISIBLE);

        apiService.updateAssetByNr(nrInv, req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    tvStatus.setText("Eroare la invalidare (cod " + response.code() + ")");
                    Toast.makeText(InventoryActivity.this,
                            "Eroare la invalidare: " + response.code(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    tvStatus.setText("Răspuns gol de la server.");
                    Toast.makeText(InventoryActivity.this,
                            "Serverul a răspuns fără date.", Toast.LENGTH_LONG).show();
                    return;
                }

                applyUpdatedAsset(updated);
                tvStatus.setText("Bunul " + nrInv + " a fost marcat NEidentificat.");
                Toast.makeText(InventoryActivity.this,
                        "Invalidare reușită pentru " + nrInv,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("Eroare de rețea la invalidare");
                Toast.makeText(InventoryActivity.this,
                        "Nu s-a putut invalida bunul: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyUpdatedAsset(AssetDto updated) {
        for (int i = 0; i < allAssets.size(); i++) {
            AssetDto current = allAssets.get(i);
            if (current.getId() == updated.getId()) {
                allAssets.set(i, updated);
                break;
            }
        }

        Collections.sort(allAssets, assetComparator);
        applyFilter();
    }

    private void removeAssetFromLists(AssetDto asset) {
        long id = asset.getId();
        for (int i = 0; i < allAssets.size(); i++) {
            if (allAssets.get(i).getId() == id) {
                allAssets.remove(i);
                break;
            }
        }
        selectedAssetIds.remove(id);
        applyFilter();
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

            boolean movedRoom = resultAsset.getRoomId() != null && resultAsset.getRoomId() != roomId;
            if (deleted || !resultAsset.isActive() || movedRoom) {
                removeAssetFromLists(resultAsset);
            } else {
                applyUpdatedAsset(resultAsset);
            }
        }
    }

    private void startBatchPrintSelected() {
        if (batchInProgress) {
            Toast.makeText(this,
                    "Un batch de tipărire este deja în curs.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<AssetDto> batchList = new ArrayList<>();
        for (AssetDto a : allAssets) {
            if (selectedAssetIds.contains(a.getId()) && !a.isIdentified()) {
                batchList.add(a);
            }
        }

        if (batchList.isEmpty()) {
            Toast.makeText(this,
                    "Nu există bunuri selectate neidentificate.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this,
                    "IP/port lipsă. Revino la ecranul de conectare.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String baseUrl = "http://" + ip + ":" + port + "/api/";
        Retrofit retrofit = ApiClient.create(baseUrl);
        ApiService apiService = retrofit.create(ApiService.class);

        batchInProgress = true;
        btnPrintSelected.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Pornire batch tipărire pentru " + batchList.size() + " bunuri...");

        runBatchValidation(apiService, batchList, 0);
    }

    private void runBatchValidation(ApiService apiService, List<AssetDto> batchList, int index) {
        if (index >= batchList.size()) {
            batchInProgress = false;
            btnPrintSelected.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Batch finalizat: " + batchList.size() + " bunuri procesate.");
            return;
        }

        AssetDto asset = batchList.get(index);
        String nrInv = asset.getNrInventar();

        if (TextUtils.isEmpty(nrInv)) {
            Toast.makeText(this,
                    "S-a sărit un bun fără număr de inventar (id=" + asset.getId() + ")",
                    Toast.LENGTH_SHORT).show();
            runBatchValidation(apiService, batchList, index + 1);
            return;
        }

        tvStatus.setText("Batch [" + (index + 1) + "/" + batchList.size()
                + "]: se validează " + nrInv + "...");

        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setIdentified(Boolean.TRUE);
        req.setLocatia(locatie);
        req.setCamera(camera);

        apiService.updateAssetByNr(nrInv, req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(InventoryActivity.this,
                            "Eroare la validarea " + nrInv + " (cod " + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    runBatchValidation(apiService, batchList, index + 1);
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    Toast.makeText(InventoryActivity.this,
                            "Răspuns gol la validarea " + nrInv,
                            Toast.LENGTH_LONG).show();
                    runBatchValidation(apiService, batchList, index + 1);
                    return;
                }

                applyUpdatedAsset(updated);

                String labelTitle = buildLabelTitle(updated);
                String locCam = locatie + " - " + camera;
                boolean printOk = PrinterHelper.printLabel(
                        InventoryActivity.this,
                        updated.getNrInventar(),
                        labelTitle,
                        locCam
                );
                if (!printOk) {
                    Toast.makeText(InventoryActivity.this,
                            "Tipărire eșuată pentru " + nrInv
                                    + ", dar bunul este marcat identificat.",
                            Toast.LENGTH_LONG).show();
                }

                runBatchValidation(apiService, batchList, index + 1);
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                Toast.makeText(InventoryActivity.this,
                        "Eroare de rețea la validarea " + nrInv + ": " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                runBatchValidation(apiService, batchList, index + 1);
            }
        });
    }
}

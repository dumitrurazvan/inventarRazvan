package ro.cnpr.inventar.ui.asset;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

public class AssetDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET = "extra_asset";
    public static final String EXTRA_RESULT_ASSET = "extra_result_asset";
    public static final String EXTRA_RESULT_DELETED = "extra_result_deleted";
    private TextView tvTitle;
    private TextView tvNrInv;
    private TextView tvType;
    private TextView tvNrCrt;
    private TextView tvFlags;
    private TextView tvRoomInfo;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private EditText etDenumire;
    private EditText etCaracteristici;
    private EditText etGestActual;
    private EditText etCompGest;
    private EditText etCustodie;
    private EditText etCompCustodie;
    private EditText etLocatie;
    private EditText etCamera;
    private Button btnValidate;
    private Button btnPrint;
    private Button btnUpdate;
    private Button btnDelete;
    private AssetDto asset;
    private ApiService apiService;

    private boolean resultSet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_detail);

        tvTitle = findViewById(R.id.tvAssetDetailTitle);
        tvNrInv = findViewById(R.id.tvAssetDetailNrInv);
        tvType = findViewById(R.id.tvAssetDetailType);
        tvNrCrt = findViewById(R.id.tvAssetDetailNrCrt);
        tvFlags = findViewById(R.id.tvAssetDetailFlags);
        tvRoomInfo = findViewById(R.id.tvAssetDetailRoom);
        tvStatus = findViewById(R.id.tvAssetDetailStatus);
        progressBar = findViewById(R.id.progressBarAssetDetail);

        etDenumire = findViewById(R.id.etAssetDetailDenumire);
        etCaracteristici = findViewById(R.id.etAssetDetailCaracteristici);
        etGestActual = findViewById(R.id.etAssetDetailGestionarActual);
        etCompGest = findViewById(R.id.etAssetDetailCompartimentGestionar);
        etCustodie = findViewById(R.id.etAssetDetailCustodie);
        etCompCustodie = findViewById(R.id.etAssetDetailCompartimentCustodie);
        etLocatie = findViewById(R.id.etAssetDetailLocatie);
        etCamera = findViewById(R.id.etAssetDetailCamera);

        btnValidate = findViewById(R.id.btnDetailValidate);
        btnPrint = findViewById(R.id.btnDetailPrint);
        btnUpdate = findViewById(R.id.btnDetailUpdate);
        btnDelete = findViewById(R.id.btnDetailDelete);

        asset = (AssetDto) getIntent().getSerializableExtra(EXTRA_ASSET);
        if (asset == null) {
            Toast.makeText(this, "Datele bunului nu sunt disponibile.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initApiService();
        bindAssetToViews();

        btnValidate.setOnClickListener(v -> onValidateClicked());
        btnPrint.setOnClickListener(v -> onPrintClicked());
        btnUpdate.setOnClickListener(v -> onUpdateClicked());
        btnDelete.setOnClickListener(v -> onDeleteClicked());
    }

    private void initApiService() {
        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);
        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this,
                    "IP/port lipsă. Revino la ecranul de conectare.",
                    Toast.LENGTH_LONG).show();
            apiService = null;
            btnValidate.setEnabled(false);
            btnUpdate.setEnabled(false);
            btnDelete.setEnabled(false);
            btnPrint.setEnabled(false);
            return;
        }
//testt
        String baseUrl = "http://" + ip + ":" + port + "/api/";
        Retrofit retrofit = ApiClient.create(baseUrl);
        apiService = retrofit.create(ApiService.class);
    }
//test12342
    private void bindAssetToViews() {
        String title = asset.getCaracteristiciObiect();
        if (isEmpty(title)) {
            title = asset.getDenumireObiect();
        }
        if (isEmpty(title)) {
            title = "(fără denumire)";
        }
        tvTitle.setText(title);
        tvNrInv.setText(safe(asset.getNrInventar()));
        tvType.setText(safe(asset.getType()));
        tvNrCrt.setText(safe(asset.getNrCrt()));
        etDenumire.setText(safeOrEmpty(asset.getDenumireObiect()));
        etCaracteristici.setText(safeOrEmpty(asset.getCaracteristiciObiect()));
        etGestActual.setText(safeOrEmpty(asset.getGestionarActual()));
        etCompGest.setText(safeOrEmpty(asset.getCompartimentGestionar()));
        etCustodie.setText(safeOrEmpty(asset.getCustodie()));
        etCompCustodie.setText(safeOrEmpty(asset.getCompartimentCustodie()));
        etLocatie.setText(safeOrEmpty(asset.getLocatia()));
        etCamera.setText(safeOrEmpty(asset.getCamera()));

        String flags = "Activ: " + asset.isActive()
                + "\nIdentificat: " + asset.isIdentified()
                + "\nPropus casare flag: " + asset.isPropusCasareFlag()
                + "\nPropus casare: " + safe(asset.getPropusCasare());
        tvFlags.setText("Stare: " + flags);

        Long roomId = asset.getRoomId();
        String roomName = safe(asset.getRoomDisplayName());
        String etaj = safe(asset.getEtaj());
        String loc = safe(asset.getLocatia());
        String cam = safe(asset.getCamera());

        String roomInfo = "Locație: " + loc + "\ncameră: " + cam + " (etaj " + etaj + ")\n"
                + "Room ID: " + (roomId != null ? roomId : "-") + "\n" + roomName;
        tvRoomInfo.setText(roomInfo);

        if (asset.isIdentified()) {
            SpannableString text = new SpannableString("Marchează NEidentificat");
            text.setSpan(new ForegroundColorSpan(Color.RED), 9, 12, 0);
            btnValidate.setText(text);
        } else {
            btnValidate.setText("Validează");
        }

        tvStatus.setText(" ");
        progressBar.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading, String statusText) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
        tvStatus.setText(statusText != null ? statusText : " ");
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return isEmpty(s) ? "-" : s.trim();
    }

    private String safeOrEmpty(String s) {
        return s == null ? "" : s;
    }

    private void returnAssetResult(boolean deleted) {
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT_DELETED, deleted);
        data.putExtra(EXTRA_RESULT_ASSET, asset);
        setResult(RESULT_OK, data);
        resultSet = true;
    }

    @Override
    public void finish() {
        if (!resultSet && asset != null) {
            returnAssetResult(false);
        }
        super.finish();
    }

    private void onValidateClicked() {
        if (apiService == null) {
            Toast.makeText(this,
                    "Configurarea serverului lipsește.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        final String nrInv = asset.getNrInventar();
        if (TextUtils.isEmpty(nrInv)) {
            Toast.makeText(this,
                    "Nu există număr de inventar pentru acest bun.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!asset.isIdentified()) {
            new AlertDialog.Builder(this)
                    .setTitle("Validează bunul?")
                    .setMessage("Ești sigur că vrei să marchezi acest bun ca IDENTIFICAT?\n\nNr inventar: " + nrInv)
                    .setPositiveButton("Da, validează", (dialog, which) -> sendValidateRequest(true))
                    .setNegativeButton("Renunță", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Invalidează bunul?")
                    .setMessage("Ești sigur că vrei să marchezi acest bun ca NEIDENTIFICAT?\n\nNr inventar: " + nrInv)
                    .setPositiveButton("Da, invalidează", (dialog, which) -> sendValidateRequest(false))
                    .setNegativeButton("Renunță", null)
                    .show();
        }
    }

    private void sendValidateRequest(boolean identified) {
        if (apiService == null) return;

        String nrInv = asset.getNrInventar();
        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setIdentified(identified);

        if (identified) {
            String loc = etLocatie.getText().toString();
            String cam = etCamera.getText().toString();
            req.setLocatia(loc);
            req.setCamera(cam);
        }

        setLoading(true, identified
                ? "Se validează bunul " + nrInv + "..."
                : "Se invalidează bunul " + nrInv + "...");

        apiService.updateAssetByNr(nrInv, req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                setLoading(false, null);

                if (!response.isSuccessful()) {
                    Toast.makeText(AssetDetailActivity.this,
                            "Eroare la actualizare (cod " + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    Toast.makeText(AssetDetailActivity.this,
                            "Serverul a răspuns fără date.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                asset = updated;
                bindAssetToViews();

                if (identified) {
                    Toast.makeText(AssetDetailActivity.this,
                            "Bunul a fost marcat IDENTIFICAT.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AssetDetailActivity.this,
                            "Bunul a fost marcat NEidentificat.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                setLoading(false, null);
                Toast.makeText(AssetDetailActivity.this,
                        "Eroare de rețea: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onPrintClicked() {
        String nrInv = asset.getNrInventar();
        if (TextUtils.isEmpty(nrInv)) {
            Toast.makeText(this,
                    "Nu există număr de inventar. Nu se poate tipări eticheta.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String title = asset.getCaracteristiciObiect();
        if (isEmpty(title)) {
            title = asset.getDenumireObiect();
        }
        if (title == null) title = "";

        String loc = etLocatie.getText().toString();
        String cam = etCamera.getText().toString();
        String locCam = loc + " - " + cam;

        boolean ok = PrinterHelper.printLabel(
                this,
                nrInv,
                title,
                locCam
        );

        if (ok) {
            Toast.makeText(this,
                    "Etichetă trimisă la imprimantă.",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Eroare la tipărire.", Toast.LENGTH_LONG).show();
        }
    }

    private void onUpdateClicked() {
        if (apiService == null) {
            Toast.makeText(this,
                    "Configurarea serverului lipsește.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AssetUpdateRequest req = new AssetUpdateRequest();
        req.setDenumireObiect(etDenumire.getText().toString());
        req.setCaracteristiciObiect(etCaracteristici.getText().toString());
        req.setGestionarActual(etGestActual.getText().toString());
        req.setCompartimentGestionar(etCompGest.getText().toString());
        req.setCustodie(etCustodie.getText().toString());
        req.setCompartimentCustodie(etCompCustodie.getText().toString());
        req.setLocatia(etLocatie.getText().toString());
        req.setCamera(etCamera.getText().toString());

        setLoading(true, "Se actualizează datele...");

        apiService.updateAssetByNr(asset.getNrInventar(), req).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                setLoading(false, null);

                if (!response.isSuccessful()) {
                    Toast.makeText(AssetDetailActivity.this,
                            "Eroare la actualizare (cod " + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                AssetDto updated = response.body();
                if (updated == null) {
                    Toast.makeText(AssetDetailActivity.this,
                            "Serverul a răspuns fără date.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                asset = updated;
                bindAssetToViews();
                Toast.makeText(AssetDetailActivity.this,
                        "Datele au fost actualizate.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                setLoading(false, null);
                Toast.makeText(AssetDetailActivity.this,
                        "Eroare de rețea: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onDeleteClicked() {
        if (apiService == null) {
            Toast.makeText(this, "Configurarea serverului lipsește.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Șterge bunul?")
                .setMessage("Ești sigur că vrei să ștergi acest bun? Acțiunea este ireversibilă.\n\nNr inventar: "
                        + asset.getNrInventar())
                .setPositiveButton("Da, șterge", (dialog, which) -> sendDeleteRequest())
                .setNegativeButton("Renunță", null)
                .show();
    }

    private void sendDeleteRequest() {
        if (apiService == null) return;

        setLoading(true, "Se șterge bunul...");

        apiService.deleteAssetByNr(asset.getNrInventar()).enqueue(new Callback<AssetDto>() {
            @Override
            public void onResponse(Call<AssetDto> call, Response<AssetDto> response) {
                setLoading(false, null);

                if (!response.isSuccessful()) {
                    Toast.makeText(AssetDetailActivity.this,
                            "Eroare la ștergere (cod " + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(AssetDetailActivity.this,
                        "Bunul a fost șters.",
                        Toast.LENGTH_SHORT).show();

                returnAssetResult(true);
                finish();
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                setLoading(false, null);
                Toast.makeText(AssetDetailActivity.this,
                        "Eroare de rețea: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}

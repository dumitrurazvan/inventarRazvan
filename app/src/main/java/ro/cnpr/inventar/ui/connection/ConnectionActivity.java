package ro.cnpr.inventar.ui.connection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.HealthResponse;
import ro.cnpr.inventar.network.ApiClient;
import ro.cnpr.inventar.network.ApiService;
import ro.cnpr.inventar.prefs.PrefsManager;
import ro.cnpr.inventar.print.HprtPrinterManager;
import ro.cnpr.inventar.ui.location.LocationSelectorActivity;

public class ConnectionActivity extends AppCompatActivity {

    private static final String TAG = "ConnectionActivity";
    private static final String PRINTER_NAME = "HM-A300-0F44";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 101;

    private EditText etIp;
    private EditText etPort;
    private Button btnConnect;
    private TextView tvStatus;
    private ProgressBar progressBar;

    private HprtPrinterManager printerManager;
    private ExecutorService executorService;
    private Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        // Server connection UI
        etIp = findViewById(R.id.etIp);
        etPort = findViewById(R.id.etPort);
        btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);

        // Printer UI
        Button connectPrinterButton = findViewById(R.id.connect_printer_button);
        Button disconnectPrinterButton = findViewById(R.id.disconnect_printer_button);
        Button printButton = findViewById(R.id.print_button);

        // 1. Try to get configuration from MDM (Bento)
        loadMdmConfigurations();

        // 2. If not MDM, load from local storage
        String savedIp = PrefsManager.getServerIp(this);
        String savedPort = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(etIp.getText()) && !TextUtils.isEmpty(savedIp)) {
            etIp.setText(savedIp);
        }
        if (TextUtils.isEmpty(etPort.getText()) && !TextUtils.isEmpty(savedPort)) {
            etPort.setText(savedPort);
        }

        btnConnect.setOnClickListener(v -> attemptConnect());

        // Printer logic
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        printerManager = HprtPrinterManager.getInstance();

        connectPrinterButton.setOnClickListener(v -> connectPrinter());
        disconnectPrinterButton.setOnClickListener(v -> disconnectPrinter());
        printButton.setOnClickListener(v -> printTestLabel());

        connectPrinter(); 
    }

    /**
     * Reads restrictions set by MDM (Bento).
     * Admin can set "server_ip" and "server_port" in Bento console.
     */
    private void loadMdmConfigurations() {
        RestrictionsManager restrictionsManager = (RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager != null) {
            Bundle restrictions = restrictionsManager.getApplicationRestrictions();
            if (restrictions != null) {
                if (restrictions.containsKey("server_ip")) {
                    String mdmIp = restrictions.getString("server_ip");
                    if (!TextUtils.isEmpty(mdmIp)) etIp.setText(mdmIp);
                }
                if (restrictions.containsKey("server_port")) {
                    String mdmPort = restrictions.getString("server_port");
                    if (!TextUtils.isEmpty(mdmPort)) etPort.setText(mdmPort);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePrinterStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (printerManager != null && printerManager.isConnected()) {
            printerManager.disconnect();
        }
    }

    private void attemptConnect() {
        String ip = etIp.getText() != null ? etIp.getText().toString().trim() : "";
        String portStr = etPort.getText() != null ? etPort.getText().toString().trim() : "";

        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, "Te rog introdu IP-ul serverului", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(portStr)) {
            Toast.makeText(this, "Te rog introdu portul serverului", Toast.LENGTH_LONG).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Port invalid.", Toast.LENGTH_LONG).show();
            return;
        }

        String baseUrl = "http://" + ip + ":" + portStr + "/api/";
        progressBar.setVisibility(View.VISIBLE);
        btnConnect.setEnabled(false);
        tvStatus.setText("Se verifică conexiunea...");

        Retrofit retrofit;
        try {
            retrofit = ApiClient.create(baseUrl);
        } catch (IllegalArgumentException e) {
            progressBar.setVisibility(View.GONE);
            btnConnect.setEnabled(true);
            tvStatus.setText("URL invalid.");
            return;
        }

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnConnect.setEnabled(true);

                if (response.isSuccessful()) {
                    PrefsManager.setServerIp(ConnectionActivity.this, ip);
                    PrefsManager.setServerPort(ConnectionActivity.this, portStr);
                    tvStatus.setText("Conectat la " + ip + ":" + portStr);
                    startActivity(new Intent(ConnectionActivity.this, LocationSelectorActivity.class));
                } else {
                    tvStatus.setText("Eroare server (cod " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConnect.setEnabled(true);
                tvStatus.setText("Eroare de rețea.");
            }
        });
    }

    private void connectPrinter() {
        if (!checkAndRequestBluetoothPermissions()) return;
        if (printerManager.isConnected()) {
            updatePrinterStatus();
            return;
        }
        executorService.execute(() -> {
            try {
                handler.post(() -> ((TextView) findViewById(R.id.printer_status_text_view)).setText(R.string.connecting));
                printerManager.connect(PRINTER_NAME);
                handler.post(this::updatePrinterStatus);
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                handler.post(this::updatePrinterStatus);
            }
        });
    }

    private void disconnectPrinter() {
        if (!printerManager.isConnected()) return;
        executorService.execute(() -> {
            printerManager.disconnect();
            handler.post(this::updatePrinterStatus);
        });
    }

    private void printTestLabel() {
        if (!printerManager.isConnected()) return;
        executorService.execute(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                printerManager.printLabel("Test MDM", "Label Print", "Bento MDM", "Admin", date);
                handler.post(() -> Toast.makeText(ConnectionActivity.this, "Print OK.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                handler.post(() -> Toast.makeText(ConnectionActivity.this, "Print Failed.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updatePrinterStatus() {
        TextView printerStatusTextView = findViewById(R.id.printer_status_text_view);
        Button connectButton = findViewById(R.id.connect_printer_button);
        Button disconnectButton = findViewById(R.id.disconnect_printer_button);
        Button printButton = findViewById(R.id.print_button);

        if (printerManager.isConnected()) {
            printerStatusTextView.setText(R.string.connected);
            printerStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            printButton.setEnabled(true);
        } else {
            printerStatusTextView.setText(R.string.disconnected);
            printerStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            printButton.setEnabled(false);
        }
    }

    private boolean checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, BLUETOOTH_PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectPrinter();
        }
    }
}

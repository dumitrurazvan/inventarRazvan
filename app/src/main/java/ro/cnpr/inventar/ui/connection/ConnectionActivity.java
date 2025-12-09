package ro.cnpr.inventar.ui.connection;

import android.Manifest;
import android.content.Intent;
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

        String savedIp = PrefsManager.getServerIp(this);
        String savedPort = PrefsManager.getServerPort(this);

        if (!TextUtils.isEmpty(savedIp)) {
            etIp.setText(savedIp);
        }
        if (!TextUtils.isEmpty(savedPort)) {
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

        connectPrinter(); // Automatically try to connect at startup
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
            Toast.makeText(this,
                    "Port invalid. Folosește doar cifre (ex: 8081).",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (port < 1 || port > 65535) {
            Toast.makeText(this,
                    "Port invalid. Interval permis: 1 - 65535.",
                    Toast.LENGTH_LONG).show();
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
            tvStatus.setText("URL invalid: " + e.getMessage());
            Toast.makeText(this,
                    "URL backend invalid. Verifică IP și port.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnConnect.setEnabled(true);

                if (!response.isSuccessful()) {
                    tvStatus.setText("Eroare la apelul /health (cod " + response.code() + ")");
                    Toast.makeText(ConnectionActivity.this,
                            "Eroare server: " + response.code(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                HealthResponse body = response.body();
                if (body == null || !"OK".equalsIgnoreCase(body.getStatus())) {
                    tvStatus.setText("Backend răspunde, dar status != OK");
                    Toast.makeText(ConnectionActivity.this,
                            "Conexiune parțială (status != OK)",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                PrefsManager.setServerIp(ConnectionActivity.this, ip);
                PrefsManager.setServerPort(ConnectionActivity.this, portStr);

                tvStatus.setText("Conectat la " + ip + ":" + portStr);

                Intent intent = new Intent(ConnectionActivity.this, LocationSelectorActivity.class);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConnect.setEnabled(true);
                tvStatus.setText("Eroare de rețea la /health");
                Toast.makeText(ConnectionActivity.this,
                        "Nu s-a putut contacta serverul: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Printer and Permissions --- //

    private void connectPrinter() {
        if (!checkAndRequestBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_LONG).show();
            return;
        }
        if (printerManager.isConnected()) {
            updatePrinterStatus();
            return;
        }

        executorService.execute(() -> {
            try {
                handler.post(() -> ((TextView) findViewById(R.id.printer_status_text_view)).setText(R.string.connecting));
                printerManager.connect(PRINTER_NAME);
                handler.post(() -> {
                    updatePrinterStatus();
                    Toast.makeText(ConnectionActivity.this, "Printer connected OK", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                handler.post(() -> {
                    Toast.makeText(ConnectionActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updatePrinterStatus();
                });
            }
        });
    }

    private void disconnectPrinter() {
        if (!printerManager.isConnected()) {
            updatePrinterStatus();
            return;
        }
        executorService.execute(() -> {
            printerManager.disconnect();
            handler.post(this::updatePrinterStatus);
        });
    }

    private void printTestLabel() {
        if (!printerManager.isConnected()) {
            Toast.makeText(this, "Printer is not connected.", Toast.LENGTH_SHORT).show();
            return;
        }
        executorService.execute(() -> {
            try {
                printerManager.printLabel("Test Print", null, null, null);
                handler.post(() -> Toast.makeText(ConnectionActivity.this, "Print command sent.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Print failed", e);
                handler.post(() -> Toast.makeText(ConnectionActivity.this, "Print failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
        // For older Android versions, permissions are granted at install time.
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Please press Connect.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission denied. Cannot connect to printer.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

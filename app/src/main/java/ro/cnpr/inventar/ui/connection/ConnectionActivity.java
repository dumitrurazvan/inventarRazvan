package ro.cnpr.inventar.ui.connection;

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
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.HealthResponse;
import ro.cnpr.inventar.network.ApiClient;
import ro.cnpr.inventar.network.ApiService;
import ro.cnpr.inventar.prefs.PrefsManager;
import ro.cnpr.inventar.ui.location.LocationSelectorActivity;

public class ConnectionActivity extends AppCompatActivity {

    private EditText etIp;
    private EditText etPort;
    private Button btnConnect;
    private TextView tvStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        etIp = findViewById(R.id.etIp);
        etPort = findViewById(R.id.etPort);
        btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);

        String savedIp = PrefsManager.getServerIp(this);
        String savedPort = PrefsManager.getServerPort(this);

        if (!TextUtils.isEmpty(savedIp)) {
            etIp.setText(savedIp);
        }
        if (!TextUtils.isEmpty(savedPort)) {
            etPort.setText(savedPort);
        }

        btnConnect.setOnClickListener(v -> attemptConnect());
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
}

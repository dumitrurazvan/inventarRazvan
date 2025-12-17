package ro.cnpr.inventar.ui.location;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.RoomDto;
import ro.cnpr.inventar.model.RoomsCache;
import ro.cnpr.inventar.network.ApiClient;
import ro.cnpr.inventar.network.ApiService;
import ro.cnpr.inventar.prefs.PrefsManager;
import ro.cnpr.inventar.ui.SearchAllItemsActivity;
import ro.cnpr.inventar.ui.room.RoomSelectorActivity;

public class LocationSelectorActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ProgressBar progressBar;
    private RecyclerView rvLocations;
    private Button btnTest;

    private final List<LocationWithRoomCount> locationsWithRoomCount = new ArrayList<>();
    private LocationAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selector);

        tvStatus = findViewById(R.id.tvLocationStatus);
        progressBar = findViewById(R.id.progressBarLocations);
        rvLocations = findViewById(R.id.rvLocations);
        btnTest = findViewById(R.id.btnTest);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter(locationsWithRoomCount, this::onLocationSelected);
        rvLocations.setAdapter(adapter);

        btnTest.setOnClickListener(v -> {
            Intent intent = new Intent(LocationSelectorActivity.this, SearchAllItemsActivity.class);
            startActivity(intent);
        });

        loadRoomsFromBackend();
    }

    private void loadRoomsFromBackend() {
        String ip = PrefsManager.getServerIp(this);
        String port = PrefsManager.getServerPort(this);

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            tvStatus.setText("Lipsesc IP/port. Revino la ecranul de conectare.");
            Toast.makeText(this,
                    "Configurare server lipsă. Te rog să revii la ecranul de conectare.",
                    Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        String baseUrl = "http://" + ip + ":" + port + "/api/";
        Retrofit retrofit = ApiClient.create(baseUrl);
        ApiService apiService = retrofit.create(ApiService.class);

        tvStatus.setText("Se încarcă locațiile...");
        progressBar.setVisibility(View.VISIBLE);

        apiService.getRooms().enqueue(new Callback<List<RoomDto>>() {
            @Override
            public void onResponse(Call<List<RoomDto>> call, Response<List<RoomDto>> response) {
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    tvStatus.setText("Eroare la încărcarea locațiilor (cod " + response.code() + ")");
                    Toast.makeText(LocationSelectorActivity.this,
                            "Eroare server la /rooms: " + response.code(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                List<RoomDto> roomList = response.body();
                if (roomList == null || roomList.isEmpty()) {
                    tvStatus.setText("Nu există locații disponibile.");
                    return;
                }

                RoomsCache.setRooms(roomList);

                Map<String, Integer> locationRoomCount = new HashMap<>();
                for (RoomDto room : roomList) {
                    String loc = room.getLocatie();
                    if (!TextUtils.isEmpty(loc)) {
                        locationRoomCount.put(loc, locationRoomCount.getOrDefault(loc, 0) + 1);
                    }
                }

                List<LocationWithRoomCount> sortedLocations = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : locationRoomCount.entrySet()) {
                    sortedLocations.add(new LocationWithRoomCount(entry.getKey(), entry.getValue()));
                }

                Collections.sort(sortedLocations, Comparator.comparing(location -> location.getLocationName().toLowerCase(Locale.ROOT)));

                locationsWithRoomCount.clear();
                locationsWithRoomCount.addAll(sortedLocations);
                adapter.notifyDataSetChanged();

                if (locationsWithRoomCount.isEmpty()) {
                    tvStatus.setText("Nu există locații distincte.");
                } else {
                    tvStatus.setText("Alege o locație din listă.");
                }
            }

            @Override
            public void onFailure(Call<List<RoomDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("Eroare de rețea la încărcarea locațiilor");
                Toast.makeText(LocationSelectorActivity.this,
                        "Nu s-au putut încărca locațiile: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onLocationSelected(LocationWithRoomCount location) {
        Intent intent = new Intent(this, RoomSelectorActivity.class);
        intent.putExtra(RoomSelectorActivity.EXTRA_LOCATIE, location.getLocationName());
        startActivity(intent);
    }
}

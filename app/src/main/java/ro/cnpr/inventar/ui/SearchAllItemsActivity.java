package ro.cnpr.inventar.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.AssetDto;
import ro.cnpr.inventar.network.ApiClient;
import ro.cnpr.inventar.network.ApiService;
import ro.cnpr.inventar.prefs.PrefsManager;

public class SearchAllItemsActivity extends AppCompatActivity {

    private EditText etSearchNrInv;
    private Button btnSearch;
    private ListView lvSearchResults;
    private ProgressBar progressBarSearch;
    private TextView tvSearchStatus;

    private ArrayAdapter<String> adapter;
    private final List<String> searchResultsStrings = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_all_items);

        etSearchNrInv = findViewById(R.id.etSearchNrInv);
        btnSearch = findViewById(R.id.btnSearch);
        lvSearchResults = findViewById(R.id.lvSearchResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchResultsStrings);
        lvSearchResults.setAdapter(adapter);

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
                    searchResultsStrings.clear();
                    searchResultsStrings.add(asset.getNrInventar() + " - " + asset.getDenumireObiect() + " - " + asset.getRoomDisplayName());
                    adapter.notifyDataSetChanged();
                    tvSearchStatus.setVisibility(View.GONE);
                } else {
                    searchResultsStrings.clear();
                    adapter.notifyDataSetChanged();
                    tvSearchStatus.setVisibility(View.VISIBLE);
                    Toast.makeText(SearchAllItemsActivity.this, "Eroare server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AssetDto> call, Throwable t) {
                progressBarSearch.setVisibility(View.GONE);
                searchResultsStrings.clear();
                adapter.notifyDataSetChanged();
                tvSearchStatus.setVisibility(View.VISIBLE);
                Toast.makeText(SearchAllItemsActivity.this, "Eroare rețea: " + t.getMessage(), Toast.LENGTH_SHORT ).show();
            }
        });
    }
}

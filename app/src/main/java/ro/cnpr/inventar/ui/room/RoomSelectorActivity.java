package ro.cnpr.inventar.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ro.cnpr.inventar.R;
import ro.cnpr.inventar.model.RoomDto;
import ro.cnpr.inventar.model.RoomsCache;
import ro.cnpr.inventar.ui.inventory.InventoryActivity;

public class RoomSelectorActivity extends AppCompatActivity {

    public static final String EXTRA_LOCATIE = "extra_locatie";

    private TextView tvLocation;
    private TextView tvStatus;
    private RecyclerView rvRooms;

    private final List<RoomDto> roomsForLocation = new ArrayList<>();
    private RoomAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_selector);

        tvLocation = findViewById(R.id.tvRoomLocation);
        tvStatus = findViewById(R.id.tvRoomsStatus);
        rvRooms = findViewById(R.id.rvRooms);

        String locatie = getIntent().getStringExtra(EXTRA_LOCATIE);
        if (locatie == null) {
            locatie = "(necunoscut)";
        }
        tvLocation.setText("Locație: " + locatie);

        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomAdapter(roomsForLocation, this::onRoomSelected);
        rvRooms.setAdapter(adapter);

        loadRoomsForLocation(locatie);
    }

    private void loadRoomsForLocation(String locatie) {
        if (!RoomsCache.hasRooms()) {
            tvStatus.setText("Nu există camere în cache. Revino la ecranul de locații.");
            Toast.makeText(this,
                    "Nu există date despre camere. Te rog să revii la ecranul de locații.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        roomsForLocation.clear();
        List<RoomDto> allRooms = RoomsCache.getRooms();
        if (allRooms != null) {
            for (RoomDto r : allRooms) {
                String roomLoc = r.getLocatie();
                if (!TextUtils.isEmpty(roomLoc)
                        && !TextUtils.isEmpty(locatie)
                        && roomLoc.equalsIgnoreCase(locatie)) {
                    roomsForLocation.add(r);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (roomsForLocation.isEmpty()) {
            tvStatus.setText("Nu au fost găsite camere pentru această locație.");
        } else {
            tvStatus.setText("Camere găsite: " + roomsForLocation.size());
        }
    }

    private void onRoomSelected(RoomDto room) {
        Intent intent = new Intent(this, InventoryActivity.class);
        intent.putExtra(InventoryActivity.EXTRA_ROOM_ID, room.getId());
        intent.putExtra(InventoryActivity.EXTRA_LOCATIE, room.getLocatie());
        intent.putExtra(InventoryActivity.EXTRA_CAMERA, room.getCamera());
        intent.putExtra(InventoryActivity.EXTRA_ROOM_DISPLAY_NAME, room.getDisplayName());
        startActivity(intent);
    }
}

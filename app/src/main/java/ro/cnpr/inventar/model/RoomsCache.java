package ro.cnpr.inventar.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoomsCache {

    private static List<RoomDto> rooms;

    public static void setRooms(List<RoomDto> list) {
        rooms = list;
    }

    public static List<RoomDto> getRooms() {
        return rooms;
    }

    public static boolean hasRooms() {
        return rooms != null && !rooms.isEmpty();
    }

    public static Set<String> getDistinctLocations() {
        Set<String> locations = new HashSet<>();
        if (hasRooms()) {
            for (RoomDto room : rooms) {
                if (room.getLocatie() != null && !room.getLocatie().trim().isEmpty()) {
                    locations.add(room.getLocatie().trim());
                }
            }
        }
        return locations;
    }
}

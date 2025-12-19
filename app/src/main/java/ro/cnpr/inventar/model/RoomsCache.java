package ro.cnpr.inventar.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    /**
     * Clears the cached room data and resets the state.
     * Call this during logout to prevent data leaking between sessions.
     */
    public static void clear() {
        if (rooms != null) {
            rooms.clear();
        }
        rooms = null;
    }




    /**
     * Filters the cache to return only rooms belonging to a specific location.
     */
    public static List<RoomDto> getRoomsByLocation(String locationName) {
        List<RoomDto> filteredRooms = new ArrayList<>();
        if (hasRooms() && locationName != null) {
            for (RoomDto room : rooms) {
                // Compare ignoring case to match the toUpperCase() in getDistinctLocations
                if (locationName.equalsIgnoreCase(room.getLocatie())) {
                    filteredRooms.add(room);
                }
            }
        }
        return filteredRooms;
    }

    /**
     * Finds a specific room by its ID.
     */
    public static RoomDto getRoomById(long roomId) {
        if (hasRooms()) {
            for (RoomDto room : rooms) {
                if (room.getId() == roomId) {
                    return room;
                }
            }
        }
        return null;
    }

    public static Set<String> getDistinctLocations() {
        Set<String> locations = new TreeSet<>(); // TreeSet keeps them sorted A-Z
        if (hasRooms()) {
            for (RoomDto room : rooms) {
                if (room.getLocatie() != null && !room.getLocatie().trim().isEmpty()) {
                    // toUpperCase() ensures "Warehouse" and "warehouse" are treated as the same
                    locations.add(room.getLocatie().trim().toUpperCase());
                }
            }
        }
        return locations;
    }
}

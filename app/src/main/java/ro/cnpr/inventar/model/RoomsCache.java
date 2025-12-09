package ro.cnpr.inventar.model;

import java.util.List;

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
}

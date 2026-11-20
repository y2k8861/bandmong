package com.bandmong.studio.dto;

import com.bandmong.studio.Room;
import com.bandmong.studio.Studio;

import java.util.List;

public record StudioView(Long id, String name, String area, String station, int openHour, int closeHour,
                         boolean lateDiscount, List<String> perks, List<RoomView> rooms) {

    public record RoomView(Long id, String name, int capacity, int pricePerHour, String gear) {
        static RoomView of(Room r) {
            return new RoomView(r.getId(), r.getName(), r.getCapacity(), r.getPricePerHour(), r.getGear());
        }
    }

    public static StudioView of(Studio s) {
        return new StudioView(s.getId(), s.getName(), s.getArea(), s.getStation(), s.getOpenHour(), s.getCloseHour(),
                s.isLateDiscount(), s.perkList(), s.getRooms().stream().map(RoomView::of).toList());
    }
}

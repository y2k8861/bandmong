package com.bandmong.studio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 합주실. 지금은 예시 데이터(StudioSeeder)이고, 실제 합주실과 연동되어 있지 않다. */
@Entity
@Table(name = "studios")
public class Studio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 20)
    private String area;

    @Column(length = 60)
    private String station;

    /** 여는 시각(시). */
    @Column(name = "open_hour", nullable = false)
    private int openHour;

    /** 닫는 시각(시). 24 이상이면 다음 날 새벽 (26 = 새벽 2시). */
    @Column(name = "close_hour", nullable = false)
    private int closeHour;

    /** 22시 이후 10% 할인 */
    @Column(name = "late_discount", nullable = false)
    private boolean lateDiscount;

    /** "|"로 구분 */
    @Column(length = 200)
    private String perks;

    @OneToMany(mappedBy = "studio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Room> rooms = new ArrayList<>();

    protected Studio() {
    }

    public Studio(String name, String area, String station, int openHour, int closeHour, boolean lateDiscount, String perks) {
        this.name = name;
        this.area = area;
        this.station = station;
        this.openHour = openHour;
        this.closeHour = closeHour;
        this.lateDiscount = lateDiscount;
        this.perks = perks;
    }

    public Studio room(String code, String name, int capacity, int pricePerHour, String gear) {
        rooms.add(new Room(this, code, name, capacity, pricePerHour, gear));
        return this;
    }

    /** 한 시간 값. 22시 이후 할인이면 10% 빼고 100원 단위로 */
    public int priceAt(Room room, int hour) {
        int p = room.getPricePerHour();
        return lateDiscount && hour >= 22 ? Math.round(p * 0.9f / 100) * 100 : p;
    }

    public List<String> perkList() {
        return perks == null || perks.isBlank() ? List.of() : Arrays.asList(perks.split("\\|"));
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getArea() { return area; }
    public String getStation() { return station; }
    public int getOpenHour() { return openHour; }
    public int getCloseHour() { return closeHour; }
    public boolean isLateDiscount() { return lateDiscount; }
    public List<Room> getRooms() { return rooms; }
}

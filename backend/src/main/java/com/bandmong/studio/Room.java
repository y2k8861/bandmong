package com.bandmong.studio;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "price_per_hour", nullable = false)
    private int pricePerHour;

    @Column(length = 120)
    private String gear;

    protected Room() {
    }

    Room(Studio studio, String code, String name, int capacity, int pricePerHour, String gear) {
        this.studio = studio;
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.pricePerHour = pricePerHour;
        this.gear = gear;
    }

    public Long getId() { return id; }
    public Studio getStudio() { return studio; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getPricePerHour() { return pricePerHour; }
    public String getGear() { return gear; }
}

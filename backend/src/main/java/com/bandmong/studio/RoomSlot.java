package com.bandmong.studio;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 방 하나의 한 시간 칸을 차지했다는 표시. (room_id, slot_start) 유니크 키가
 * "같은 방 같은 시간에 두 밴드" 를 DB 차원에서 막는다. 예약을 취소하면 지운다.
 */
@Entity
@Table(name = "room_slots",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_slot", columnNames = {"room_id", "slot_start"}))
public class RoomSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "slot_start", nullable = false)
    private Instant slotStart;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    protected RoomSlot() {
    }

    public RoomSlot(Long roomId, Instant slotStart, Long bookingId) {
        this.roomId = roomId;
        this.slotStart = slotStart;
        this.bookingId = bookingId;
    }

    public Instant getSlotStart() { return slotStart; }
}

package com.bandmong.jam;

import com.bandmong.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "jam_rsvps",
        uniqueConstraints = @UniqueConstraint(name = "uk_jam_rsvp", columnNames = {"jam_id", "user_id"}))
public class JamRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jam_id", nullable = false)
    private Jam jam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RsvpAnswer answer;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JamRsvp() {
    }

    public JamRsvp(Jam jam, User user, RsvpAnswer answer) {
        this.jam = jam;
        this.user = user;
        answer(answer);
    }

    public void answer(RsvpAnswer answer, Instant now) {
        this.answer = answer;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Jam getJam() { return jam; }
    public User getUser() { return user; }
    public RsvpAnswer getAnswer() { return answer; }
    public Instant getUpdatedAt() { return updatedAt; }
}

-- 밴드몽 첫 스키마 (MySQL 8).
-- 엔티티에서 Hibernate로 뽑은 CREATE 문을 정리한 것. 이후 변경은 V2__, V3__ … 새 파일로만 한다.
-- 이모지(채팅)를 위해 utf8mb4. 시간 컬럼은 모두 UTC로 저장한다 (hibernate.jdbc.time_zone=UTC).

create table availabilities (
    slot integer not null,
    week_start date not null,
    band_id bigint not null,
    id bigint not null auto_increment,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table band_applications (
    band_id bigint not null,
    created_at datetime(6) not null,
    decided_at datetime(6),
    id bigint not null auto_increment,
    user_id bigint not null,
    message varchar(200),
    instrument enum ('BASS','DRUM','ETC','GUITAR','KEYS','VOCAL') not null,
    status enum ('ACCEPTED','CANCELED','PENDING','REJECTED') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table band_members (
    band_id bigint not null,
    id bigint not null auto_increment,
    joined_at datetime(6) not null,
    last_read_message_id bigint,
    user_id bigint not null,
    instrument enum ('BASS','DRUM','ETC','GUITAR','KEYS','VOCAL') not null,
    role enum ('HOST','MEMBER') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table band_seats (
    seat_count integer not null,
    band_id bigint not null,
    instrument enum ('BASS','DRUM','ETC','GUITAR','KEYS','VOCAL') not null,
    primary key (band_id, instrument)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table bands (
    dues_amount integer,
    recruiting bit not null,
    created_at datetime(6) not null,
    host_id bigint not null,
    id bigint not null auto_increment,
    area varchar(20) not null,
    schedule varchar(20),
    title varchar(24) not null,
    dues_account varchar(60),
    intro varchar(300),
    genre enum ('BALLAD','CITYPOP','INDIE','JAZZ','ROCK') not null,
    level enum ('BEGINNER','HOBBY','STAGE'),
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table booking_settles (
    amount integer not null,
    booking_id bigint not null,
    id bigint not null auto_increment,
    paid_at datetime(6),
    user_id bigint not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table bookings (
    hours integer not null,
    per_person integer not null,
    price integer not null,
    band_id bigint not null,
    booked_by bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    jam_id bigint,
    room_id bigint not null,
    starts_at datetime(6) not null,
    status enum ('CANCELED','CONFIRMED') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table chat_messages (
    hidden bit not null,
    band_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    sender_id bigint,
    client_id varchar(36),
    body varchar(1000) not null,
    type enum ('SYSTEM','TEXT') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table dues_payments (
    amount integer not null,
    due_month varchar(7) not null,
    band_id bigint not null,
    id bigint not null auto_increment,
    marked_by bigint not null,
    paid_at datetime(6) not null,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table gig_checks (
    done bit not null,
    done_by bigint,
    gig_id bigint not null,
    id bigint not null auto_increment,
    text varchar(60) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table gig_cue_items (
    duration_sec integer not null,
    position integer not null,
    gig_id bigint not null,
    id bigint not null auto_increment,
    title varchar(60) not null,
    type enum ('MC','SONG') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table gigs (
    slot_minutes integer not null,
    band_id bigint not null,
    created_by bigint not null,
    id bigint not null auto_increment,
    starts_at datetime(6) not null,
    updated_at datetime(6) not null,
    version bigint not null,
    title varchar(30) not null,
    venue varchar(40),
    status enum ('CANCELED','SCHEDULED') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table jam_rsvps (
    id bigint not null auto_increment,
    jam_id bigint not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    answer enum ('LATE','NO','YES') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table jams (
    duration_minutes integer not null,
    band_id bigint not null,
    created_at datetime(6) not null,
    created_by bigint not null,
    id bigint not null auto_increment,
    starts_at datetime(6) not null,
    place varchar(40),
    memo varchar(200),
    status enum ('CANCELED','SCHEDULED') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table notifications (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    read_at datetime(6),
    user_id bigint not null,
    title varchar(60) not null,
    link varchar(100),
    body varchar(200),
    type enum ('APPLICATION_ACCEPTED','APPLICATION_RECEIVED','APPLICATION_REJECTED','BOOKING_CANCELED','BOOKING_CONFIRMED','GIG_SCHEDULED','HOST_CHANGED','JAM_CANCELED','JAM_EDITED','JAM_MOVED','JAM_SCHEDULED','MEMBER_LEFT','REPORT_RESOLVED','SANCTIONED') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table reports (
    accused_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    reporter_id bigint not null,
    resolved_at datetime(6),
    resolved_by bigint,
    target_id bigint not null,
    resolution varchar(20),
    admin_note varchar(300),
    detail varchar(500),
    reason enum ('ABUSE','FAKE','NO_SHOW','OTHER','SEXUAL','SPAM') not null,
    status enum ('DISMISSED','OPEN','RESOLVED') not null,
    target_type enum ('BAND','MESSAGE','USER') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table room_slots (
    booking_id bigint not null,
    id bigint not null auto_increment,
    room_id bigint not null,
    slot_start datetime(6) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table rooms (
    capacity integer not null,
    price_per_hour integer not null,
    id bigint not null auto_increment,
    studio_id bigint not null,
    code varchar(10) not null,
    name varchar(20) not null,
    gear varchar(120),
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table sanctions (
    created_by bigint not null,
    ends_at datetime(6) not null,
    id bigint not null auto_increment,
    lifted_at datetime(6),
    report_id bigint,
    starts_at datetime(6) not null,
    user_id bigint not null,
    reason varchar(200) not null,
    type enum ('SUSPENSION','WARNING') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table studios (
    close_hour integer not null,
    late_discount bit not null,
    open_hour integer not null,
    id bigint not null auto_increment,
    area varchar(20) not null,
    name varchar(40) not null,
    station varchar(60),
    perks varchar(200),
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table user_instruments (
    user_id bigint not null,
    instrument enum ('BASS','DRUM','ETC','GUITAR','KEYS','VOCAL') not null,
    primary key (user_id, instrument)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table users (
    created_at datetime(6) not null,
    deleted_at datetime(6),
    id bigint not null auto_increment,
    nickname varchar(10),
    provider varchar(20) not null,
    region varchar(30),
    provider_id varchar(100) not null,
    email varchar(120),
    level enum ('BEGINNER','HOBBY','STAGE'),
    role enum ('ADMIN','USER') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_availability_week on availabilities (band_id, week_start);
alter table availabilities add constraint uk_availability unique (band_id, user_id, week_start, slot);
create index idx_app_band_status on band_applications (band_id, status);
alter table band_members add constraint uk_band_member unique (band_id, user_id);
create index idx_bands_recruiting on bands (recruiting, created_at);
alter table booking_settles add constraint uk_settle unique (booking_id, user_id);
create index idx_booking_band on bookings (band_id, starts_at);
create index idx_chat_band_id on chat_messages (band_id, id);
alter table chat_messages add constraint uk_chat_client unique (band_id, sender_id, client_id);
alter table dues_payments add constraint uk_dues unique (band_id, user_id, due_month);
create index idx_gig_band on gigs (band_id, starts_at);
alter table jam_rsvps add constraint uk_jam_rsvp unique (jam_id, user_id);
create index idx_jams_band_starts on jams (band_id, starts_at);
create index idx_noti_user_id on notifications (user_id, id);
create index idx_report_status on reports (status, id);
create index idx_report_target on reports (target_type, target_id);
alter table room_slots add constraint uk_room_slot unique (room_id, slot_start);
create index idx_sanction_user on sanctions (user_id, ends_at);
alter table users add constraint uk_users_provider unique (provider, provider_id);
alter table availabilities add constraint fk_availabilities_band_id foreign key (band_id) references bands (id);
alter table availabilities add constraint fk_availabilities_user_id foreign key (user_id) references users (id);
alter table band_applications add constraint fk_band_applications_band_id foreign key (band_id) references bands (id);
alter table band_applications add constraint fk_band_applications_user_id foreign key (user_id) references users (id);
alter table band_members add constraint fk_band_members_band_id foreign key (band_id) references bands (id);
alter table band_members add constraint fk_band_members_user_id foreign key (user_id) references users (id);
alter table band_seats add constraint fk_band_seats_band_id foreign key (band_id) references bands (id);
alter table bands add constraint fk_bands_host_id foreign key (host_id) references users (id);
alter table booking_settles add constraint fk_booking_settles_booking_id foreign key (booking_id) references bookings (id);
alter table booking_settles add constraint fk_booking_settles_user_id foreign key (user_id) references users (id);
alter table bookings add constraint fk_bookings_band_id foreign key (band_id) references bands (id);
alter table bookings add constraint fk_bookings_booked_by foreign key (booked_by) references users (id);
alter table bookings add constraint fk_bookings_room_id foreign key (room_id) references rooms (id);
alter table chat_messages add constraint fk_chat_messages_band_id foreign key (band_id) references bands (id);
alter table chat_messages add constraint fk_chat_messages_sender_id foreign key (sender_id) references users (id);
alter table dues_payments add constraint fk_dues_payments_band_id foreign key (band_id) references bands (id);
alter table dues_payments add constraint fk_dues_payments_user_id foreign key (user_id) references users (id);
alter table gig_checks add constraint fk_gig_checks_gig_id foreign key (gig_id) references gigs (id);
alter table gig_cue_items add constraint fk_gig_cue_items_gig_id foreign key (gig_id) references gigs (id);
alter table gigs add constraint fk_gigs_band_id foreign key (band_id) references bands (id);
alter table gigs add constraint fk_gigs_created_by foreign key (created_by) references users (id);
alter table jam_rsvps add constraint fk_jam_rsvps_jam_id foreign key (jam_id) references jams (id);
alter table jam_rsvps add constraint fk_jam_rsvps_user_id foreign key (user_id) references users (id);
alter table jams add constraint fk_jams_band_id foreign key (band_id) references bands (id);
alter table jams add constraint fk_jams_created_by foreign key (created_by) references users (id);
alter table notifications add constraint fk_notifications_user_id foreign key (user_id) references users (id);
alter table reports add constraint fk_reports_reporter_id foreign key (reporter_id) references users (id);
alter table rooms add constraint fk_rooms_studio_id foreign key (studio_id) references studios (id);
alter table user_instruments add constraint fk_user_instruments_user_id foreign key (user_id) references users (id);

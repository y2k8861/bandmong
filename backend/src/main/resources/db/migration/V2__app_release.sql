-- 7단계: 앱 출시 준비 — 사용자 차단, 앱 로그인 토큰, 소셜 로그인 일회용 코드, 푸시 기기.
-- 이미 적용된 V1은 고치지 않는다.

create table user_blocks (
    id bigint not null auto_increment,
    blocked_id bigint not null,
    blocker_id bigint not null,
    created_at datetime(6) not null,
    primary key (id),
    constraint uk_user_block unique (blocker_id, blocked_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_block_blocked on user_blocks (blocked_id);

create table app_tokens (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    device_name varchar(60),
    expires_at datetime(6) not null,
    last_used_at datetime(6) not null,
    revoked_at datetime(6),
    token_hash varchar(64) not null,
    user_id bigint not null,
    primary key (id),
    constraint uk_app_token_hash unique (token_hash)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_app_token_user on app_tokens (user_id);
alter table app_tokens add constraint fk_app_tokens_user_id foreign key (user_id) references users (id);

create table app_login_codes (
    id bigint not null auto_increment,
    code_hash varchar(64) not null,
    expires_at datetime(6) not null,
    used_at datetime(6),
    user_id bigint not null,
    primary key (id),
    constraint uk_login_code_hash unique (code_hash)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table device_tokens (
    id bigint not null auto_increment,
    platform enum ('ANDROID','IOS') not null,
    token varchar(255) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    primary key (id),
    constraint uk_device_token unique (token)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_device_user on device_tokens (user_id);

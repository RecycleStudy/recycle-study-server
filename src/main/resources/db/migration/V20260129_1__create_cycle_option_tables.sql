-- cycle_option
create table cycle_option
(
    id          bigint auto_increment primary key,
    member_id   bigint       not null,
    title       varchar(255) not null,
    option_type varchar(255) not null,
    created_at  datetime(6)  not null,
    modified_at datetime(6)  null,
    constraint fk_cycle_option_member_id
        foreign key (member_id) references member (id)
);

-- cycle_option_duration
create table cycle_option_duration
(
    cycle_option_id bigint      not null,
    duration        bigint      not null,
    constraint pk_cycle_option_duration
        primary key (cycle_option_id, duration),
    constraint fk_cycle_option_duration_cycle_option_id
        foreign key (cycle_option_id) references cycle_option (id)
);

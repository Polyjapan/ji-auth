# --- !Ups

create table sessions
(
    session_key BINARY(16) null,
    user_id int null,
    created_at TIMESTAMP default CURRENT_TIMESTAMP null,
    last_used_at TIMESTAMP default CURRENT_TIMESTAMP null,
    expires_at TIMESTAMP default CURRENT_TIMESTAMP null,
    constraint sessions_pk
        primary key (session_key),
    constraint sessions_users_id_fk
        foreign key (user_id) references users (id)
            on update cascade on delete cascade
);

# --- !Downs

drop table sessions;
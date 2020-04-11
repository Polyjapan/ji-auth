# --- !Ups

drop table sessions;

create table sessions
(
    session_key binary(16)                                                 not null primary key,
    user_id     int                                                        null,
    created_at  timestamp default current_timestamp()                      null,
    expires_at  timestamp default TIMESTAMPADD(DAY, 30, CURRENT_TIMESTAMP) null,

    user_agent  varchar(250)                                               not null,
    ip_address  varchar(100)                                               not null,
    constraint sessions_users_id_fk
        foreign key (user_id) references users (id)
            on update cascade on delete cascade
);

rename table cas_proxy_tickets to cas_proxy_granting_tickets;

alter table cas_proxy_granting_tickets
    add session_key binary(16) null;

alter table cas_proxy_granting_tickets
    add constraint cas_proxy_granting_tickets_sessions_session_key_fk
        foreign key (session_key) references sessions (session_key)
            on update cascade on delete cascade;

alter table cas_tickets
    add session_key BINARY(16) null;

alter table cas_tickets
    add constraint cas_tickets_sessions_session_key_fk
        foreign key (session_key) references sessions (session_key)
            on update cascade on delete cascade;


# --- !Downs


rename table cas_proxy_granting_tickets to cas_proxy_tickets;

alter table cas_proxy_tickets
    drop constraint cas_proxy_granting_tickets_sessions_session_key_fk,
    drop column session_key;

alter table cas_tickets
    drop constraint cas_tickets_sessions_session_key_fk,
    drop column session_key;

drop table sessions;
create table sessions
(
    session_key  BINARY(16)                          null,
    user_id      int                                 null,
    created_at   TIMESTAMP default CURRENT_TIMESTAMP null,
    last_used_at TIMESTAMP default CURRENT_TIMESTAMP null,
    expires_at   TIMESTAMP default CURRENT_TIMESTAMP null,
    constraint sessions_pk
        primary key (session_key),
    constraint sessions_users_id_fk
        foreign key (user_id) references users (id)
            on update cascade on delete cascade
);


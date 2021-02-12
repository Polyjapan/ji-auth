# --- !Ups

create table webauthn_keys
(
    user_id int null,
    user_handle CHAR(128),
    key_name VARCHAR(255),
    key_uid VARCHAR(255),
    key_cose VARCHAR(255),

    constraint webauthn_keys_pk
        primary key (user_handle, key_uid)
);

create index webauthn_keys_user_handle_index
    on webauthn_keys (user_handle);

create index webauthn_keys_user_id_index
    on webauthn_keys (user_id);

alter table users
    add user_handle CHAR(128) default NULL null;

create unique index users_user_handle_uindex
    on users (user_handle);



# --- !Downs

drop table webauthn_keys;

alter table users
    drop index users_user_handle_uindex,
    drop column user_handle;

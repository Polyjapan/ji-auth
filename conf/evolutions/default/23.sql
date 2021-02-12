# --- !Ups

create table totp_keys
(
    id int primary key auto_increment,
    user_id int null,
    device_name VARCHAR(255) not null,
    shared_secret VARCHAR(100) not null,

    constraint totp_keys_users_id_fk
        foreign key (user_id) references users (id)
);

create index totp_keys_user_id_index
    on totp_keys (user_id);




# --- !Downs

drop table totp_keys;
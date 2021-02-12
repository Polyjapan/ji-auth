# --- !Ups

create table tfa_backup_codes
(
    user_id int null,
    code VARCHAR(100) not null,

    constraint tfa_backup_codes_users_id_fk
        foreign key (user_id) references users (id)
);

create index tfa_backup_codes_user_id_index
    on tfa_backup_codes (user_id);

# --- !Downs

drop table tfa_backup_codes;
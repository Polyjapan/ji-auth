# --- !Ups

alter table users
    add first_name varchar(100) default null null;

alter table users
    add last_name varchar(100) default null null;

alter table users
    add phone_number varchar(50) default null null;

create table users_addresses
(
    user_id int null,
    address varchar(200) null,
    address_complement varchar(200) default null null,
    post_code varchar(10) null,
    region varchar(25) null,
    country varchar(100) null,
    constraint users_addresses_pk
        primary key (user_id),
    constraint users_addresses_users_id_fk
        foreign key (user_id) references users (id)
            on delete cascade
);


# --- !Downs

alter table users
    drop column first_name;

alter table users
    drop column last_name;

alter table users
    drop column phone_number;

drop table users_addresses;

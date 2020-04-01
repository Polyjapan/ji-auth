# --- !Ups

create table cas_services
(
    service_id int auto_increment
        primary key,
    service_name varchar(200) null
);

create table cas_domains
(
    service_id int null,
    domain varchar(250) null,
    constraint cas_domains_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
);

create table cas_tickets
(
    service_id int null,
    user_id int null,
    expiration timestamp default (current_timestamp() + interval 5 minute) null,
    constraint cas_tickets_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id),
    constraint cas_tickets_users_id_fk
        foreign key (user_id) references users (id)
);

alter table cas_tickets
    add ticket VARCHAR(100) null;

create index cas_tickets_ticket_index
    on cas_tickets (ticket);


# --- !Downs

drop table cas_services;
drop table cas_domains;
drop table cas_tickets;
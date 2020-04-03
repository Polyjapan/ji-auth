# --- !Ups

create table cas_proxy_tickets
(
    ticket VARCHAR(100) null,
    service_id int not null,
    user_id int null,
    constraint cas_proxy_tickets_pk
        primary key (ticket),
    constraint cas_proxy_tickets_users_id_fk
        foreign key (user_id) references users (id),
    constraint cas_proxy_tickets_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
);

create index cas_proxy_tickets_user_id_index
    on cas_proxy_tickets (user_id);

create table cas_proxy_allow
(
    target_service int not null,
    allowed_service int not null,
    constraint cas_proxy_allow_pk
        primary key (allowed_service, target_service),
    constraint cas_proxy_allow_cas_services_service_id_fk
        foreign key (target_service) references cas_services (service_id),
    constraint cas_proxy_allow_cas_services_service_id_fk_2
        foreign key (allowed_service) references cas_services (service_id)
);


# --- !Downs

drop table cas_proxy_tickets;
drop table cas_proxy_allow;
# --- !Ups

create table cas_required_groups
(
    service_id int null,
    group_id int null,
    constraint cas_required_groups_pk
        primary key (service_id, group_id),
    constraint cas_required_groups_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id),
    constraint cas_required_groups_groups_id_fk
        foreign key (group_id) references `groups` (id)
)
    comment 'Each person accessing the app must be part of ALL required groups';

create table cas_allowed_groups
(
    service_id int null,
    group_id int null,
    constraint cas_allowed_groups_pk
        primary key (service_id, group_id),
    constraint cas_allowed_groups_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id),
    constraint cas_allowed_groups_groups_id_fk
        foreign key (group_id) references `groups` (id)
)
    comment 'Each person accessing the app must be part of AT LEAST ONE allowed group';



# --- !Downs

drop table cas_allowed_groups;
drop table cas_required_groups;
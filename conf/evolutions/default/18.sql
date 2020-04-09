# --- !Ups

alter table cas_allowed_groups drop foreign key cas_allowed_groups_groups_id_fk;

alter table cas_allowed_groups
    add constraint cas_allowed_groups_groups_id_fk
        foreign key (group_id) references `groups` (id)
            on update cascade on delete cascade;

alter table cas_required_groups drop foreign key cas_required_groups_groups_id_fk;

alter table cas_required_groups
    add constraint cas_required_groups_groups_id_fk
        foreign key (group_id) references `groups` (id)
            on update cascade on delete cascade;

alter table groups_allowed_scopes drop foreign key groups_allowed_scopes_groups_id_fk;

alter table groups_allowed_scopes
    add constraint groups_allowed_scopes_groups_id_fk
        foreign key (group_id) references `groups` (id)
            on update cascade on delete cascade;

alter table cas_allowed_groups drop foreign key cas_allowed_groups_cas_services_service_id_fk;

alter table cas_allowed_groups
    add constraint cas_allowed_groups_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
            on update cascade on delete cascade;

alter table cas_domains drop foreign key cas_domains_cas_services_service_id_fk;

alter table cas_domains
    add constraint cas_domains_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
            on update cascade on delete cascade;

alter table cas_proxy_allow drop foreign key cas_proxy_allow_cas_services_service_id_fk;

alter table cas_proxy_allow
    add constraint cas_proxy_allow_cas_services_service_id_fk
        foreign key (target_service) references cas_services (service_id)
            on update cascade on delete cascade;

alter table cas_proxy_allow drop foreign key cas_proxy_allow_cas_services_service_id_fk_2;

alter table cas_proxy_allow
    add constraint cas_proxy_allow_cas_services_service_id_fk_2
        foreign key (allowed_service) references cas_services (service_id)
            on update cascade on delete cascade;

alter table cas_proxy_tickets drop foreign key cas_proxy_tickets_cas_services_service_id_fk;

alter table cas_proxy_tickets
    add constraint cas_proxy_tickets_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
            on update cascade on delete cascade;

alter table cas_required_groups drop foreign key cas_required_groups_cas_services_service_id_fk;

alter table cas_required_groups
    add constraint cas_required_groups_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
            on update cascade on delete cascade;

alter table cas_tickets drop foreign key cas_tickets_cas_services_service_id_fk;

alter table cas_tickets
    add constraint cas_tickets_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id)
            on update cascade on delete cascade;

# --- !Downs

alter table cas_allowed_groups drop foreign key cas_allowed_groups_groups_id_fk;

alter table cas_allowed_groups
    add constraint cas_allowed_groups_groups_id_fk
        foreign key (group_id) references `groups` (id);

alter table cas_required_groups drop foreign key cas_required_groups_groups_id_fk;

alter table cas_required_groups
    add constraint cas_required_groups_groups_id_fk
        foreign key (group_id) references `groups` (id);

alter table groups_allowed_scopes drop foreign key groups_allowed_scopes_groups_id_fk;

alter table groups_allowed_scopes
    add constraint groups_allowed_scopes_groups_id_fk
        foreign key (group_id) references `groups` (id);


alter table cas_allowed_groups drop foreign key cas_allowed_groups_cas_services_service_id_fk;

alter table cas_allowed_groups
    add constraint cas_allowed_groups_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id);

alter table cas_domains drop foreign key cas_domains_cas_services_service_id_fk;

alter table cas_domains
    add constraint cas_domains_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id);

alter table cas_proxy_allow drop foreign key cas_proxy_allow_cas_services_service_id_fk;

alter table cas_proxy_allow
    add constraint cas_proxy_allow_cas_services_service_id_fk
        foreign key (target_service) references cas_services (service_id);

alter table cas_proxy_allow drop foreign key cas_proxy_allow_cas_services_service_id_fk_2;

alter table cas_proxy_allow
    add constraint cas_proxy_allow_cas_services_service_id_fk_2
        foreign key (allowed_service) references cas_services (service_id);

alter table cas_proxy_tickets drop foreign key cas_proxy_tickets_cas_services_service_id_fk;

alter table cas_proxy_tickets
    add constraint cas_proxy_tickets_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id);

alter table cas_required_groups drop foreign key cas_required_groups_cas_services_service_id_fk;

alter table cas_required_groups
    add constraint cas_required_groups_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id);

alter table cas_tickets drop foreign key cas_tickets_cas_services_service_id_fk;

alter table cas_tickets
    add constraint cas_tickets_cas_services_service_id_fk
        foreign key (service_id) references cas_services (service_id);


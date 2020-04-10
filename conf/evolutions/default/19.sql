# --- !Ups

alter table cas_services
    add service_requires_full_info boolean default false not null;

# --- !Downs

alter table cas_services
    drop column service_requires_full_info;


# --- !Ups

alter table cas_services
    add service_redirect_url VARCHAR(250) default NULL null;

insert into cas_services(service_name, service_redirect_url)
    select CONCAT(app_name, '_', app_id), redirect_url from apps;

insert into cas_domains(service_id, domain)
    select service_id, client_id from apps join cas_services on CONCAT(app_name, '_', app_id) = service_name;

alter table apps
    drop column client_id,
    drop column redirect_url;

# For internal apps that can use the token auth scheme
create table internal_domains
(
    domain_name VARCHAR(250) not null
);

create index internal_domains_domain_name_index
    on internal_domains (domain_name);



# --- !Downs

# THIS IS DESTRUCTIVE!!! APPLY WITH EXTREME CAUTION.

alter table apps
    add client_id varchar(150) null;

alter table apps
    add redirect_url varchar(250) null;


alter table cas_services drop column service_redirect_url;

drop table internal_domains;
# --- !Ups

alter table cas_services
    add service_portal_display boolean default false not null,
    add service_portal_title varchar(200) default null null,
    add service_portal_description mediumtext default null null,
    add service_portal_login_url mediumtext default null null,
    add service_portal_image_url mediumtext default null null;

# --- !Downs

alter table cas_services
    drop service_portal_display,
    drop service_portal_title,
    drop service_portal_description,
    drop service_portal_login_url,
    drop service_portal_image_url;

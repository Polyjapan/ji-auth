# --- !Ups

alter table cas_proxy_tickets
    add expiration TIMESTAMP default TIMESTAMPADD(DAY, 30, CURRENT_TIMESTAMP) null;


# --- !Downs

alter table cas_proxy_tickets drop column expiration;
# --- !Ups

alter table users
    add newsletter boolean default false not null;

# --- !Downs

alter table users
    drop column newsletter;


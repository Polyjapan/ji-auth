# --- !Ups

alter table users
	add admin_level int default 0 not null;

# --- !Downs

alter table users
    drop column admin_level;


# --- !Ups

alter table `groups`
    drop foreign key groups_users_fk,
    drop column owner;

alter table groups_members
    drop column can_manage_members;

alter table groups_members
    drop column can_read_members;

alter table groups_members
    drop column is_admin;

# --- !Downs

alter table `groups`
    add owner int null;

alter table groups_members
    add can_manage_members tinyint(1) default 0 null;

alter table groups_members
    add can_read_members tinyint(1) default 0 null;

alter table groups_members
    add is_admin tinyint(1) default 0 null;


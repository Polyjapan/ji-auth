# --- !Ups

create table `groups`
(
    `id`           INT AUTO_INCREMENT PRIMARY KEY,
    `owner`        INT          NOT NULL,
    `name`         VARCHAR(100) NOT NULL UNIQUE,
    `display_name` VARCHAR(100) NOT NULL
);

alter table `groups`
    add constraint `groups_users_fk` foreign key (`owner`) references `users` (`id`) on delete cascade on update cascade;

create table `groups_members`
(
    `group_id`           INT NOT NULL PRIMARY KEY,
    `user_id`            INT NOT NULL PRIMARY KEY,
    `can_manage_members` BOOLEAN DEFAULT FALSE,
    `can_read_members`   BOOLEAN DEFAULT FALSE,
    `is_admin`           BOOLEAN DEFAULT FALSE
);

alter table `groups_members`
    add constraint `groups_members_users_fk` foreign key (`user_id`) references `users` (`id`) on delete cascade on update cascade,
    add constraint `groups_members_groups_fk` foreign key (`group_id`) references `groups` (`id`) on delete cascade on update cascade;


# --- !Downs

drop table `groups_members`;
drop table `groups`;
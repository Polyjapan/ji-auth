# --- !Ups
create table `users`
(
    `id`                 INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `email`              VARCHAR(180) NOT NULL UNIQUE,
    `email_confirm_key`  VARCHAR(100) NULL,
    `password`           VARCHAR(250) NOT NULL,
    `password_algo`      VARCHAR(15)  NOT NULL,
    `password_reset`     VARCHAR(250),
    `password_reset_end` TIMESTAMP    NULL
);

create table `apps`
(
    `app_id`                INT AUTO_INCREMENT PRIMARY KEY,
    `app_created_by`        INT NOT NULL,
    `client_id`             VARCHAR(150) NOT NULL,
    `client_secret`         VARCHAR(150) NOT NULL,
    `app_name`              VARCHAR(150) NOT NULL,
    `redirect_url`          VARCHAR(250) NOT NULL,
    `email_callback_url`    VARCHAR(250) NOT NULL,
    `recaptcha_private_key` VARCHAR(250) NOT NULL
);

alter table `apps`
    add constraint `apps_users_fk` foreign key (`app_created_by`) references `users` (`id`) on delete cascade on update cascade;

create table `tickets`
(
    `token`    VARCHAR(100)                                                                              NOT NULL PRIMARY KEY,
    `user_id`  INTEGER                                                                                   NOT NULL,
    `app_id`   INTEGER                                                                                   NOT NULL,
    `valid_to` TIMESTAMP                                                                                 NOT NULL,
    `type`     SET ('T_LOGIN', 'T_REGISTER', 'T_DOUBLE_REGISTER', 'T_EMAIL_CONFIRM', 'T_PASSWORD_RESET') NOT NULL
);

alter table `tickets`
    add constraint `tickets_users_fk` foreign key (`user_id`) references `users` (`id`) on delete cascade on update cascade,
    add constraint `tickets_apps_fk` foreign key (`app_id`) references `apps` (`app_id`) on delete cascade on update cascade;

# --- !Downs

drop table `tickets`;
drop table `apps`;
drop table `users`;
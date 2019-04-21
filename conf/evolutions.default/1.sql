# --- !Ups
create table `clients`
(
    `client_id`                 INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `client_firstname`          VARCHAR(100) NOT NULL,
    `client_lastname`           VARCHAR(100) NOT NULL,
    `client_email`              VARCHAR(180) NOT NULL UNIQUE,
    `client_email_confirm_key`  VARCHAR(100) NULL,
    `client_password`           VARCHAR(250) NOT NULL,
    `client_password_algo`      VARCHAR(15)  NOT NULL,
    `client_password_reset`     VARCHAR(250),
    `client_password_reset_end` TIMESTAMP    NULL
);

create table `refresh_tokens`
(
    `token`      VARCHAR(100) NOT NULL PRIMARY KEY,
    `client_id`  INTEGER      NOT NULL,
    `valid_from` TIMESTAMP    NOT NULL,
    `valid_to`   TIMESTAMP    NOT NULL,
    `user_agent` VARCHAR(250) NOT NULL,
    `last_use`   TIMESTAMP    NOT NULL,
    `last_ip`    VARCHAR(32)  NOT NULL
);

alter table `refresh_tokens`
    add constraint `refresh_token_client_fk` foreign key (`client_id`) references `clients` (`client_id`);

create table `permissions`
(
    `client_id`  INTEGER      NOT NULL,
    `permission` VARCHAR(180) NOT NULL,

    PRIMARY KEY (`client_id`, `permission`)
);

alter table `permissions`
    add constraint `permissions_client_fk` foreign key (`client_id`) references `clients` (`client_id`) on update NO ACTION on delete CASCADE;

create table `apps`
(
    `client_id` VARCHAR(150) NOT NULL,
    `client_secret` VARCHAR(150) NOT NULL,
    `app_name` VARCHAR(150) NOT NULL,
    `redirect_url` VARCHAR(250) NOT NULL
);

# --- !Downs
drop table `permissions`;

drop table `refresh_tokens`;

drop table `clients`;

drop table `apps`;
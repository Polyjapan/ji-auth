# --- !Ups

drop table tickets;
rename table apps TO api_keys;

# --- !Downs

rename table api_keys TO apps;
create table tickets
(
    token varchar(100) not null
        primary key,
    user_id int not null,
    app_id int not null,
    valid_to timestamp default current_timestamp() not null on update current_timestamp(),
    type set('T_LOGIN', 'T_REGISTER', 'T_EXPLICIT_GRANT', 'T_DOUBLE_REGISTER', 'T_EMAIL_CONFIRM', 'T_PASSWORD_RESET', 'T_APP') not null,
    constraint tickets_apps_fk
        foreign key (app_id) references apps (app_id)
            on update cascade on delete cascade,
    constraint tickets_users_fk
        foreign key (user_id) references users (id)
            on update cascade on delete cascade
);


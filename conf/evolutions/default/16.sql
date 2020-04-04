# --- !Ups

create table groups_allowed_scopes
(
    group_id int not null,
    scope VARCHAR(200) not null,
    constraint groups_allowed_scopes_groups_id_fk
        foreign key (group_id) references `groups` (id)
);

create table users_allowed_scopes
(
    user_id int not null,
    scope VARCHAR(200) not null,
    constraint users_allowed_scopes_users_id_fk
        foreign key (user_id) references users (id)
);

create table api_keys_allowed_scopes
(
    api_key_id int not null,
    scope VARCHAR(200) not null,
    constraint api_keys_allowed_scopes_api_keys_app_id_fk
        foreign key (api_key_id) references api_keys (app_id)
);

create index api_keys_allowed_scopes_api_key_id_index
    on api_keys_allowed_scopes (api_key_id);


create index users_allowed_scopes_user_id_index
    on users_allowed_scopes (user_id);


create index groups_allowed_scopes_group_id_index
    on groups_allowed_scopes (group_id);



# --- !Downs

drop table users_allowed_scopes;
drop table api_keys_allowed_scopes;
drop table groups_allowed_scopes;
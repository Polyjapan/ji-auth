# --- !Ups

alter table users
    add email_confirm_last_sent timestamp default null null after email_confirm_key;

# --- !Downs

alter table users
    drop column email_confirm_last_sent;

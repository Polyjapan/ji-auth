# --- !Ups

alter table apps
    drop column recaptcha_private_key,
    drop column email_callback_url;

# --- !Downs

alter table apps
    add `email_callback_url`    VARCHAR(250) NOT NULL DEFAULT '',
    add `recaptcha_private_key` VARCHAR(250) NULL;
# --- !Ups

alter table tickets modify type set('T_LOGIN', 'T_REGISTER', 'T_EXPLICIT_GRANT', 'T_DOUBLE_REGISTER', 'T_EMAIL_CONFIRM', 'T_PASSWORD_RESET', 'T_APP') not null;

# --- !Downs

alter table tickets modify type set('T_LOGIN', 'T_REGISTER', 'T_EXPLICIT_GRANT', 'T_DOUBLE_REGISTER', 'T_EMAIL_CONFIRM', 'T_PASSWORD_RESET') not null;

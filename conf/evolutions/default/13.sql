# --- !Ups

alter table internal_domains
    add safe_redirection boolean default false null;

# --- !Downs

alter table internal_domains
    drop column safe_redirection;



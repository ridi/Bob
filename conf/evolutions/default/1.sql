# --- First database schema

# --- !Ups

set ignorecase true;

create table bob (
  id                        bigint not null auto_increment,
  name                      varchar(255) not null,
  constraint pk_bob primary key (id))
;

--create sequence bob_seq start with (select max(id) + 1 from bob);

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists bob;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists bob_seq;

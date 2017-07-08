# --- First database schema

# --- !Ups

# --- set ignorecase true;

create table bob (
  id                bigint not null auto_increment,
  name              varchar(255) not null,
  constraint pk_bob primary key (id))
;

create table poll (
  id                bigint not null auto_increment,
  channel_id        varchar(32),
  message_ts        varchar(32),
  is_open           boolean DEFAULT true,
  constraint pk_poll primary key (id)
);

create table candidates (
  poll_id           bigint not null,
  serial_no         int not null,
  bob_id            bigint
);

create table vote_history (
  poll_id           bigint not null,
  user_id           varchar(128),
  candidate_serial_no   int
);

create table poll_result (
  poll_id           bigint not null,
  bob_id            bigint not null
)

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists bob;
drop table if exists poll;
drop table if exists candidates;
drop table if exists vote_history;

SET REFERENTIAL_INTEGRITY TRUE;


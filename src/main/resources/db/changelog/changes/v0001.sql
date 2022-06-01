create table exec_data (
    id bigserial not null,
    session_id varchar(255) not null,
    date_coverage date not null,
    microservice_name varchar(255),
    microservice_version varchar(255),
    microservice_hash varchar(255),
    primary key (id)
);
create index idx_exec_data_date_coverage on exec_data(date_coverage);
create index idx_exec_data_microservice_name on exec_data(microservice_name);

create table report_data (
  id bigserial not null,
  exec_id bigint not null references exec_data (id),
  instructions_missed int,
  instructions_covered int,
  branches_missed int,
  branches_covered int,
  cxty_missed int,
  cxty_covered int,
  lines_missed int,
  lines_covered int,
  methods_missed int,
  methods_covered int,
  classes_missed int,
  classes_covered int,
  primary key (id)
);

create table server_status (
    id bigserial not null,
    status varchar(255) not null,
    time_stamp timestamp without time zone not null,
    primary key (id)
)
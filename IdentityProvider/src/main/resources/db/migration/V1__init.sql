
ALTER SCHEMA auth_service OWNER TO postgres;
create table auth_service.users (
  id                    bigserial,
  username              varchar(30) not null unique,
  password              varchar(80) not null,
  email                 varchar(50) unique,
  primary key (id)
);

create table auth_service.roles (
  id                    serial,
  name                  varchar(50) not null,
  primary key (id)
);

CREATE TABLE auth_service.users_roles (
  user_id               bigint not null,
  role_id               int not null,
  primary key (user_id, role_id),
  foreign key (user_id) references users (id),
  foreign key (role_id) references roles (id)
);

insert into auth_service.roles (name)
values
('ROLE_USER'), ('ROLE_ADMIN');

insert into auth_service.users (username, password, email)
values
('user', '$2a$04$Fx/SX9.BAvtPlMyIIqqFx.hLY2Xp8nnhpzvEEVINvVpwIPbA3v/.i', 'user@gmail.com'),
('admin', '$2a$04$Fx/SX9.BAvtPlMyIIqqFx.hLY2Xp8nnhpzvEEVINvVpwIPbA3v/.i', 'admin@gmail.com');

insert into auth_service.users_roles (user_id, role_id)
values
(1, 1),
(2, 2);
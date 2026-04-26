use reactor_netdisk;
-- auto-generated definition
create table user
(
    id          int auto_increment
        primary key,
    username    varchar(20)                        not null,
    password    varchar(20)                        null,
    email       varchar(50)                        null,
    create_time datetime default CURRENT_TIMESTAMP null,
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    delete_flag bit      default b'0'              null,
    constraint username
        unique (username)
);

create index email_index
    on user (email);

create index username_index
    on user (username);

-- auto-generated definition
create table user_token
(
    id          int auto_increment
        primary key,
    user_id     int                                not null,
    token       varchar(50)                        not null,
    expire_time datetime                           not null,
    create_time datetime default CURRENT_TIMESTAMP null,
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    delete_flag bit      default b'0'              null
);

create index user_id_index
    on user_token (user_id);
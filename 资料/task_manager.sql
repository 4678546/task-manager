create database if not exists `task-manager`;
use `task-manager`;
create table if not exists user(
                                   id int unsigned auto_increment comment 'id'primary key,
                                   username    varchar(16) not null comment '用户名',
                                   password    varchar(100) not null comment '密码',
                                   create_time datetime    not null comment '创建时间',
                                   update_time datetime not null comment '更新时间'
)comment '用户表';
INSERT INTO `task-manager`.user (id, username, password, create_time, update_time) VALUES (1, 'sakura', '$2a$10$Aei98PQRC3GcZWnpAYPBkeDXO50MqOT0DLX34xLxENpij158Y2Hny', '2026-06-15 22:25:34', '2026-06-15 22:25:34');

create table if not exists task(
                                   id int unsigned primary key auto_increment comment '任务主键',
                                   title varchar(20) comment '任务标题',
                                   content varchar(100)comment '任务内容',
                                   status varchar(5) default 'TODO' comment '状态,默认值为TODO',
                                   deadline datetime comment '截止时间',
                                   user_id int unsigned comment '用户id',
                                   create_time datetime not null comment '创建时间',
                                   update_time datetime not null comment '更新时间'
) comment '任务表';
alter table task add constraint fk_task_user foreign key (user_id) references user (id) on update cascade on delete cascade;
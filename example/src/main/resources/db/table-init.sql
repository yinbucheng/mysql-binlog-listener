create database if not exists ad_test;

create table if not exists ad_book(
 id bigint primary key auto_increment,
 name varchar(50) not null default "" comment "书名",
 title varchar(50) not null default "" comment "提纲",
 writer varchar(50) not null default "" comment "作者",
 create_time datetime not null default now() comment "创建时间",
 update_time datetime not null default now() comment "更新时间",
 content text not null  comment "内容"
);


create table if not exists ad_user(
id bigint primary key auto_increment,
name varchar(50) not null default "" comment "用户名称",
age int not null default 0 comment "用户年龄",
gender char(10) not null default "男" comment "用户性别",
create_time datetime not null default now() comment "创建时间",
update_time datetime not null default now() comment "更新时间"
);
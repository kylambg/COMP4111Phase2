use comp4111;
drop table if exists `Users`;


create table if not exists users
(UserID int,
 UserName varchar(255),
 Passwords varchar(255),
 PRIMARY KEY (UserID))
 engine = InnoDB;
 

show tables;
commit;
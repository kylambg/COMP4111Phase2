use comp4111;
drop table if exists `books`;

create table if not exists books
(BookID int not null auto_increment, 
Title varchar(255) not null,
Author varchar(255) not null,
Publisher varchar(255) not null,
Year int not null, 
Statuses boolean not null default 1,
PRIMARY KEY(BookID))
engine = InnoDB;
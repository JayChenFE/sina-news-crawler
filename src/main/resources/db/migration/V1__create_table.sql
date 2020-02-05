create table links_to_be_process
(
	link varchar(2000) not null
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci comment='待处理链接';

create table links_already_processed
(
	link varchar(2000) not null
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci  comment='已处理的链接';

create table news
(
	id bigint  primary key auto_increment,
	title text not null comment '标题' ,
	content mediumtext not null comment '内容',
	url varchar(2000) not null comment 'url',
	created_at timestamp default now(),
	modified_at timestamp default now()
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci  comment='新闻';


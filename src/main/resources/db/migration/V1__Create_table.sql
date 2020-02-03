create table LINKS_TO_BE_PROCESSED
(
	link varchar(2000) not null
);

comment on table LINKS_TO_BE_PROCESSED is '待处理链接';

create table LINKS_ALREADY_PROCESSED
(
	link varchar(2000) not null
);

comment on table LINKS_ALREADY_PROCESSED is '已处理的链接';

create table news
(
	id bigint auto_increment,
	title text not null,
	content text not null,
	url varchar(2000) not null,
	created_at timestamp,
	modified_at timestamp,
	constraint news_pk
		primary key (id)
);

comment on table news is '新闻';

comment on column news.title is '标题';

comment on column news.content is '内容';

comment on column news.url is 'url';

comment on column news.created_at is '创建时间';

comment on column news.modified_at is '修改时间';
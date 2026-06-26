create database if not exists pdd_flash_sale default charset utf8mb4 collate utf8mb4_unicode_ci;
use pdd_flash_sale;

drop table if exists pdd_order;
drop table if exists pdd_lottery_prize;
drop table if exists pdd_activity;
drop table if exists pdd_product;
drop table if exists pdd_user;

create table pdd_user
(
    id         bigint primary key,
    username   varchar(32)  not null,
    password   varchar(64)  not null,
    role       varchar(16)  not null default 'USER',
    created_at datetime     not null default current_timestamp,
    updated_at datetime     not null default current_timestamp on update current_timestamp,
    unique key uk_username (username)
) engine = InnoDB default charset = utf8mb4;

create table pdd_product
(
    id              bigint primary key,
    name            varchar(128)   not null,
    price           decimal(10, 2) not null,
    total_stock     int            not null,
    available_stock int            not null,
    category        varchar(32)    not null default 'DIGITAL' comment '商品类别: DIGITAL/DAILY_GOODS/FRUITS',
    description     varchar(512)   null comment '商品描述',
    image_url       varchar(256)   null comment '商品图片URL',
    status          tinyint        not null default 1 comment '1=在售 0=下架',
    created_at      datetime       not null default current_timestamp,
    updated_at      datetime       not null default current_timestamp on update current_timestamp,
    key idx_product_category (category)
) engine = InnoDB default charset = utf8mb4;

create table pdd_activity
(
    id              bigint primary key,
    product_id      bigint         not null,
    type            varchar(16)    not null,
    activity_price  decimal(10, 2) not null,
    total_stock     int            not null,
    available_stock int            not null,
    start_time      datetime       not null,
    end_time        datetime       not null,
    status          tinyint        not null default 1,
    created_at      datetime       not null default current_timestamp,
    updated_at      datetime       not null default current_timestamp on update current_timestamp,
    key idx_product_id (product_id),
    key idx_type_start_time (type, start_time),
    constraint fk_activity_product foreign key (product_id) references pdd_product (id)
) engine = InnoDB default charset = utf8mb4;

create table pdd_lottery_prize
(
    id          bigint primary key,
    activity_id bigint       not null,
    code        varchar(64)  not null,
    name        varchar(128) not null,
    level       varchar(32)  not null,
    prize_type  varchar(32)  not null default 'PHYSICAL',
    weight      int          not null,
    stock       int          null,
    winning     tinyint      not null default 1,
    sort_order  int          not null default 0,
    created_at  datetime     not null default current_timestamp,
    updated_at  datetime     not null default current_timestamp on update current_timestamp,
    unique key uk_activity_code (activity_id, code),
    key idx_activity_sort (activity_id, sort_order),
    constraint fk_prize_activity foreign key (activity_id) references pdd_activity (id)
) engine = InnoDB default charset = utf8mb4;

create table pdd_order
(
    id          bigint primary key,
    request_id  varchar(96)    not null,
    user_id     bigint         not null,
    activity_id bigint         not null,
    product_id  bigint         not null,
    quantity    int            not null,
    amount      decimal(10, 2) not null,
    source      varchar(16)    not null,
    status      varchar(16)    not null,
    created_at  datetime       not null default current_timestamp,
    updated_at  datetime       not null default current_timestamp on update current_timestamp,
    unique key uk_request_id (request_id),
    key idx_user_activity_source (user_id, activity_id, source),
    key idx_user_created_at (user_id, created_at),
    key idx_activity_id (activity_id)
) engine = InnoDB default charset = utf8mb4;

insert into pdd_user (id, username, password, role)
values (10001, 'demo', md5('123456'), 'USER'),
       (10002, 'admin', md5('123456'), 'ADMIN');

insert into pdd_product (id, name, price, total_stock, available_stock, category, description, status)
values (6180001, 'PDD 618 限量旗舰手机', 4999.00, 100000, 100000, 'DIGITAL', '618 大促旗舰手机，搭载最新处理器与 AI 影像系统', 1),
       (6180002, 'MacBook Pro M4 最新款', 12999.00, 50000, 50000, 'DIGITAL', 'Apple M4 芯片，16 英寸 Liquid Retina XDR 显示屏', 1),
       (6180003, 'PDD 精选洗发水 500ml', 39.90, 50000, 50000, 'DAILY_GOODS', '温和洁净，适合日常使用的氨基酸洗发水', 1),
       (6180004, 'PDD 抽纸 10 包装', 19.90, 100000, 100000, 'DAILY_GOODS', '三层加厚，柔软亲肤不掉屑', 1),
       (6180005, 'PDD 洗衣液 2kg', 29.90, 80000, 80000, 'DAILY_GOODS', '深层去渍，持久留香，母婴可用', 1),
       (6180006, '海南妃子笑荔枝 2.5kg', 49.90, 30000, 30000, 'FRUITS', '新鲜采摘，顺丰冷链直达，甜嫩多汁', 1),
       (6180007, '智利车厘子 JJ 级 1kg', 79.90, 20000, 20000, 'FRUITS', '进口车厘子，颗颗精选，脆甜爽口', 1),
       (6180008, '新疆哈密瓜 2 个装', 39.90, 40000, 40000, 'FRUITS', '产地直发，香甜多汁，正宗西州蜜', 1);

insert into pdd_activity (id, product_id, type, activity_price, total_stock, available_stock, start_time, end_time, status)
values (900001, 6180001, 'SECKILL', 2999.00, 90000, 90000, '2026-06-18 20:00:00', '2026-06-18 20:45:00', 1),
       (100001, 6180001, 'LOTTERY', 0.00, 10000, 10000, '2026-06-18 20:45:00', '2026-06-18 21:00:00', 1),
       (100002, 6180002, 'LOTTERY', 0.00, 50000, 50000, '2026-06-18 10:00:00', '2026-06-18 23:59:59', 1);

insert into pdd_lottery_prize (id, activity_id, code, name, level, prize_type, weight, stock, winning, sort_order)
values
       (700001, 100001, 'IPHONE_17_PLUS', 'iPhone 17 Plus', '一等奖', 'PHYSICAL', 50, 10, 1, 1),
       (700002, 100001, 'REDMI_K100_PRO', '红米 K100 Pro', '二等奖', 'PHYSICAL', 450, 100, 1, 2),
       (700003, 100001, 'COUPON_1000', '1000 元代金券', '三等奖', 'COUPON', 4500, 1000, 1, 3),
       (700004, 100001, 'PARTICIPATION', '参与奖', '参与奖', 'THANKS', 5000, null, 0, 4),
       (700101, 100002, 'MACBOOK_10_PERCENT', 'MacBook Pro M4 1 折购买资格', '超级大奖', 'DISCOUNT', 20, 20, 1, 1),
       (700102, 100002, 'MACBOOK_20_PERCENT', 'MacBook Pro M4 2 折购买资格', '一等奖', 'DISCOUNT', 40, 60, 1, 2),
       (700103, 100002, 'MACBOOK_30_PERCENT', 'MacBook Pro M4 3 折购买资格', '二等奖', 'DISCOUNT', 80, 120, 1, 3),
       (700104, 100002, 'MACBOOK_50_PERCENT', 'MacBook Pro M4 5 折购买资格', '三等奖', 'DISCOUNT', 160, 260, 1, 4),
       (700105, 100002, 'MACBOOK_70_PERCENT', 'MacBook Pro M4 7 折购买资格', '幸运奖', 'DISCOUNT', 260, 600, 1, 5),
       (700106, 100002, 'MACBOOK_90_PERCENT', 'MacBook Pro M4 9 折购买资格', '尝鲜奖', 'DISCOUNT', 420, 1200, 1, 6),
       (700107, 100002, 'COUPON_2000', '2000 元 Mac 代金券', '加码奖', 'COUPON', 500, 1000, 1, 7),
       (700108, 100002, 'COUPON_1000', '1000 元 Mac 代金券', '福利奖', 'COUPON', 800, 2000, 1, 8),
       (700109, 100002, 'IQIYI_YEAR', '爱奇艺黄金会员 1 年兑换码', '会员奖', 'MEMBERSHIP', 930, 2500, 1, 9),
       (700110, 100002, 'YOUKU_YEAR', '优酷黄金会员 1 年兑换码', '会员奖', 'MEMBERSHIP', 930, 2500, 1, 10),
       (700111, 100002, 'MIGU_YEAR', '咪咕黄金会员 1 年兑换码', '会员奖', 'MEMBERSHIP', 930, 2500, 1, 11),
       (700112, 100002, 'BILIBILI_YEAR', 'bilibili 大会员 1 年兑换码', '会员奖', 'MEMBERSHIP', 930, 2500, 1, 12),
       (700113, 100002, 'MAC_PARTICIPATION', '谢谢参与', '参与奖', 'THANKS', 4000, null, 0, 13);

use pdd_flash_sale;

set @has_unique_user_activity_source = (
    select count(*)
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'pdd_order'
      and index_name = 'uk_user_activity_source'
);

set @drop_sql = if(
    @has_unique_user_activity_source > 0,
    'alter table pdd_order drop index uk_user_activity_source',
    'select 1'
);
prepare drop_stmt from @drop_sql;
execute drop_stmt;
deallocate prepare drop_stmt;

set @has_normal_user_activity_source = (
    select count(*)
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'pdd_order'
      and index_name = 'idx_user_activity_source'
);

set @add_sql = if(
    @has_normal_user_activity_source = 0,
    'alter table pdd_order add index idx_user_activity_source (user_id, activity_id, source)',
    'select 1'
);
prepare add_stmt from @add_sql;
execute add_stmt;
deallocate prepare add_stmt;

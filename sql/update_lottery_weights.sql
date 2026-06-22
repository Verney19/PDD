use pdd_flash_sale;

update pdd_lottery_prize
set weight = case code
    when 'IPHONE_17_PLUS' then 50
    when 'REDMI_K100_PRO' then 450
    when 'COUPON_1000' then 4500
    when 'PARTICIPATION' then 5000
    else weight
end
where activity_id = 100001
  and code in ('IPHONE_17_PLUS', 'REDMI_K100_PRO', 'COUPON_1000', 'PARTICIPATION');

update pdd_lottery_prize
set weight = case code
    when 'MACBOOK_10_PERCENT' then 20
    when 'MACBOOK_20_PERCENT' then 40
    when 'MACBOOK_30_PERCENT' then 80
    when 'MACBOOK_50_PERCENT' then 160
    when 'MACBOOK_70_PERCENT' then 260
    when 'MACBOOK_90_PERCENT' then 420
    when 'COUPON_2000' then 500
    when 'COUPON_1000' then 800
    when 'IQIYI_YEAR' then 930
    when 'YOUKU_YEAR' then 930
    when 'MIGU_YEAR' then 930
    when 'BILIBILI_YEAR' then 930
    when 'MAC_PARTICIPATION' then 4000
    else weight
end
where activity_id = 100002
  and code in (
      'MACBOOK_10_PERCENT',
      'MACBOOK_20_PERCENT',
      'MACBOOK_30_PERCENT',
      'MACBOOK_50_PERCENT',
      'MACBOOK_70_PERCENT',
      'MACBOOK_90_PERCENT',
      'COUPON_2000',
      'COUPON_1000',
      'IQIYI_YEAR',
      'YOUKU_YEAR',
      'MIGU_YEAR',
      'BILIBILI_YEAR',
      'MAC_PARTICIPATION'
  );

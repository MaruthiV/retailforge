insert into stock_level (id, store_id, product_id, available, reserved) values ('store-001:prod-1', 'store-001', 'prod-1', 50, 0) on conflict (id) do nothing;
insert into stock_level (id, store_id, product_id, available, reserved) values ('store-001:prod-coffee', 'store-001', 'prod-coffee', 30, 0) on conflict (id) do nothing;
insert into stock_level (id, store_id, product_id, available, reserved) values ('store-001:prod-mug', 'store-001', 'prod-mug', 5, 0) on conflict (id) do nothing;

merge into product (id, name, base_price) key(id) values ('prod-1', 'Widget', 100.00);
merge into product (id, name, base_price) key(id) values ('prod-coffee', 'Coffee Bag', 12.50);
merge into product (id, name, base_price) key(id) values ('prod-mug', 'Ceramic Mug', 8.00);

merge into promotion (code, type, amount, product_id, starts_at, ends_at, active) key(code)
  values ('SAVE10', 'PERCENT', 10, null, timestamp '2020-01-01 00:00:00', timestamp '2030-01-01 00:00:00', true);
merge into promotion (code, type, amount, product_id, starts_at, ends_at, active) key(code)
  values ('BOGO-COFFEE', 'BOGO', 0, 'prod-coffee', timestamp '2020-01-01 00:00:00', timestamp '2030-01-01 00:00:00', true);
merge into promotion (code, type, amount, product_id, starts_at, ends_at, active) key(code)
  values ('SPRING20', 'PERCENT', 20, null, timestamp '2020-01-01 00:00:00', timestamp '2021-01-01 00:00:00', true);

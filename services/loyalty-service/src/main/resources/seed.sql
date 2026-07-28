insert into loyalty_customer (id, name, points_balance) values ('cust-001', 'Ada Lovelace', 1200) on conflict (id) do nothing;
insert into loyalty_customer (id, name, points_balance) values ('cust-002', 'Grace Hopper', 300) on conflict (id) do nothing;
insert into loyalty_customer (id, name, points_balance) values ('cust-003', 'Alan Turing', 6100) on conflict (id) do nothing;

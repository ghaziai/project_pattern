-- Jalankan di Supabase: SQL Editor -> New query -> Run.
-- Aplikasi memakai service_role key di sisi server saja (jangan ditaruh di frontend).

create table if not exists public.products (
  code  text primary key,
  name  text   not null,
  price bigint not null check (price >= 0),
  stock integer not null check (stock >= 0)
);

create table if not exists public.orders (
  id              text primary key,
  customer        text   not null,
  status          text   not null check (status in ('PENDING_PAYMENT','PAID','FAILED','CANCELLED')),
  method          text   not null,
  items           jsonb  not null default '[]'::jsonb,
  subtotal        bigint not null default 0,
  discount        bigint not null default 0,
  voucher         text,
  points_used     bigint not null default 0,
  fee             bigint not null default 0,
  total           bigint not null default 0,
  stock_reserved  boolean not null default false,
  points_reserved boolean not null default false,
  points_earned   bigint not null default 0,
  payment_ref     text,
  message         text,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);
create index if not exists orders_created_idx on public.orders (created_at desc);

create table if not exists public.loyalty (
  customer text primary key,
  points   bigint not null default 0 check (points >= 0)
);

create table if not exists public.transaction_log (
  id         bigserial primary key,
  created_at timestamptz not null default now(),
  order_id   text,
  event      text,
  method     text,
  amount     bigint,
  detail     text
);
create index if not exists txlog_created_idx on public.transaction_log (created_at desc);

-- RLS aktif tanpa policy: anon/authenticated tidak dapat mengakses; service_role melewati RLS.
alter table public.products        enable row level security;
alter table public.orders          enable row level security;
alter table public.loyalty         enable row level security;
alter table public.transaction_log enable row level security;

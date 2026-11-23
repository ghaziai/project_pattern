-- Jalankan di Supabase: SQL Editor -> New query -> Run.
-- Aplikasi memakai service_role key di sisi server saja (jangan ditaruh di frontend).

create table if not exists public.products (
  code  text primary key,
  name  text   not null,
  price bigint not null check (price >= 0),
  stock integer not null check (stock >= 0)
);

create or replace function public.add_product_stock(p_code text, p_quantity integer)
returns jsonb
language plpgsql
security invoker
as $$
declare
  updated_product public.products%rowtype;
begin
  if p_quantity is null or p_quantity <= 0 then
    return jsonb_build_object('status', 'invalid_quantity');
  end if;

  update public.products p
     set stock = p.stock + p_quantity
   where p.code = p_code
     and p.stock::bigint + p_quantity::bigint <= 2147483647
  returning p.* into updated_product;

  if found then
    return jsonb_build_object('status', 'ok', 'product', to_jsonb(updated_product));
  end if;
  if exists (select 1 from public.products p where p.code = p_code) then
    return jsonb_build_object('status', 'capacity_exceeded');
  end if;
  return jsonb_build_object('status', 'not_found');
end;
$$;
revoke all on function public.add_product_stock(text, integer) from public, anon, authenticated;
grant execute on function public.add_product_stock(text, integer) to service_role;

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

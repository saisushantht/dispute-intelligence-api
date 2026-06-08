create table transactions (
                              id               text primary key,
                              merchant_id      text not null,
                              product_category text not null,
                              customer_country text not null,
                              customer_email   text not null,
                              customer_ip      text not null,
                              amount           numeric(12,2) not null,
                              currency         text not null,
                              created_at       timestamptz not null
);

create table chargebacks (
                             id                 text primary key,
                             transaction_id     text not null references transactions(id),
                             merchant_id        text not null,
                             product_category   text not null,
                             customer_country   text not null,
                             customer_email     text not null,
                             customer_ip        text not null,
                             amount             numeric(12,2) not null,
                             currency           text not null,
                             reason_code        text not null,
                             reason_description text not null,
                             reason_category    text not null,
                             status             text not null check (status in ('open','won','lost','expired')),
                             opened_at          timestamptz not null,
                             deadline_at        timestamptz not null,
                             responded          boolean not null default false,
                             resolved_at        timestamptz
);

create index idx_cb_status   on chargebacks(status);
create index idx_cb_reason   on chargebacks(reason_code);
create index idx_cb_category on chargebacks(product_category);
create index idx_cb_country  on chargebacks(customer_country);
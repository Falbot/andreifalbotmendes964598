create extension if not exists "uuid-ossp";

create table if not exists app_user (
  id uuid primary key default uuid_generate_v4(),
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  created_at timestamptz not null default now()
);

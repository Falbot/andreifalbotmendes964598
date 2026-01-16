create table if not exists refresh_token (
  id uuid primary key default uuid_generate_v4(),
  usuario_id uuid not null references usuario(id),
  token_hash varchar(64) not null,
  expira_em timestamptz not null,
  revogado_em timestamptz null,
  criado_em timestamptz not null default now()
);

create unique index if not exists ux_refresh_token_hash on refresh_token(token_hash);
create index if not exists ix_refresh_token_usuario on refresh_token(usuario_id);
create index if not exists ix_refresh_token_expira on refresh_token(expira_em);

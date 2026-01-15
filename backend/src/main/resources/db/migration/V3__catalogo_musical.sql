create table if not exists artista (
  id uuid primary key default uuid_generate_v4(),
  nome varchar(200) not null,
  tipo varchar(20) not null, -- CANTOR | BANDA (validaremos na aplicação)
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

create table if not exists album (
  id uuid primary key default uuid_generate_v4(),
  titulo varchar(200) not null,
  ano_lancamento int null,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

create table if not exists artista_album (
  artista_id uuid not null references artista(id),
  album_id uuid not null references album(id),
  criado_em timestamptz not null default now(),
  primary key (artista_id, album_id)
);

create index if not exists idx_artista_nome on artista(nome);
create index if not exists idx_album_titulo on album(titulo);
create index if not exists idx_artista_album_album on artista_album(album_id);

--Renomear constraint
do $$
begin
  if exists (
    select 1
    from pg_constraint
    where conname = 'app_user_email_key'
  ) then
    alter table usuario rename constraint app_user_email_key to usuario_email_uk;
  end if;
exception when others then
  -- se não existir ou já estiver renomeada, ignora
end $$;
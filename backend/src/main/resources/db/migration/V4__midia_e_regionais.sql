create table if not exists capa_album (
  id uuid primary key default uuid_generate_v4(),
  album_id uuid not null unique references album(id),
  objeto_chave varchar(512) not null,   -- valor chave no MinIO
  content_type varchar(120) not null,
  tamanho_bytes bigint not null,
  criado_em timestamptz not null default now()
);

create table if not exists regional (
  id uuid primary key default uuid_generate_v4(),
  nome varchar(200) not null,
  ativo boolean not null default true,
  criado_em timestamptz not null default now()
);

create index if not exists idx_regional_nome on regional(nome);

-- V8: permitir múltiplas capas por álbum e manter uma "principal"

-- 1) remover UNIQUE antigo de album_id (pode ser constraint ou índice)
do $$
declare
  v_conname text;
begin
  -- drop UNIQUE constraint que contenha album_id
  for v_conname in
    select c.conname
    from pg_constraint c
    join pg_class t on t.oid = c.conrelid
    where t.relname = 'capa_album'
      and c.contype = 'u'
      and exists (
        select 1
        from unnest(c.conkey) as k(attnum)
        join pg_attribute a on a.attrelid = t.oid and a.attnum = k.attnum
        where a.attname = 'album_id'
      )
  loop
    execute format('alter table capa_album drop constraint %I', v_conname);
  end loop;

  -- se por algum motivo existir um UNIQUE index puro em album_id, remove também
  for v_conname in
    select i.relname
    from pg_class t
    join pg_index ix on ix.indrelid = t.oid
    join pg_class i on i.oid = ix.indexrelid
    where t.relname = 'capa_album'
      and ix.indisunique
      and not ix.indisprimary
      and array_length(ix.indkey, 1) = 1
      and exists (
        select 1
        from unnest(ix.indkey) as k(attnum)
        join pg_attribute a on a.attrelid = t.oid and a.attnum = k.attnum
        where a.attname = 'album_id'
      )
  loop
    execute format('drop index if exists %I', v_conname);
  end loop;
end $$;

-- 2) adicionar coluna principal
alter table capa_album
  add column if not exists principal boolean not null default false;

-- 3) como antes era 1:1, promover os registros existentes para principal
update capa_album
  set principal = true
where principal = false;

-- 4) garantir no máximo 1 principal por álbum (índice parcial)
create unique index if not exists ux_capa_album_album_principal
  on capa_album(album_id)
  where principal = true;

-- 5) índices úteis para listagem
create index if not exists ix_capa_album_album
  on capa_album(album_id);

create index if not exists ix_capa_album_album_criado
  on capa_album(album_id, criado_em desc);

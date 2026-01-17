--renomear o antigo "id" (uuid PK) para "id_regional" (PK interna)
--criar "id" integer para representar o ID vindo da API externa
--garantir histórico: permitir múltiplas linhas para o mesmo id (externo),
--mas no máximo 1 linha ativa por id

alter table if exists regional
  rename column id to id_regional;

alter table if exists regional
  add column if not exists id integer;

-- garante no maximo 1 registro ATIVO por id externo
create unique index if not exists ux_regional_id_ativo
  on regional(id)
  where ativo = true;

create index if not exists idx_regional_id
  on regional(id);

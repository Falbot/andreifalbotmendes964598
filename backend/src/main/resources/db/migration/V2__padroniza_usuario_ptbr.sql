-- Padronização PT-BR: tabela e campos de usuário

alter table if exists app_user rename to usuario;

alter table if exists usuario
  rename column password_hash to senha_hash;

alter table if exists usuario
  rename column created_at to criado_em;

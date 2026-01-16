alter table usuario
  add column if not exists criado_em timestamptz not null default now();

alter table usuario
  add column if not exists atualizado_em timestamptz not null default now();

create or replace function trg_set_atualizado_em()
returns trigger as $$
begin
  new.atualizado_em = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists usuario_set_atualizado_em on usuario;

create trigger usuario_set_atualizado_em
before update on usuario
for each row
execute function trg_set_atualizado_em();

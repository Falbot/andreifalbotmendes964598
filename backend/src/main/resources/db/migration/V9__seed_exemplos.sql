-- V9: carga inicial (exemplos do edital) - Artistas / Álbuns (N:N)

-- ===== Artistas =====
insert into artista (id, nome, tipo)
select '21293936-27cc-4a98-94c4-45f86c929711'::uuid, 'Serj Tankian', 'CANTOR'
where not exists (select 1 from artista where nome = 'Serj Tankian' and tipo = 'CANTOR');

insert into artista (id, nome, tipo)
select '84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid, 'Mike Shinoda', 'CANTOR'
where not exists (select 1 from artista where nome = 'Mike Shinoda' and tipo = 'CANTOR');

insert into artista (id, nome, tipo)
select '6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid, 'Michel Teló', 'CANTOR'
where not exists (select 1 from artista where nome = 'Michel Teló' and tipo = 'CANTOR');

insert into artista (id, nome, tipo)
select '8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid, 'Guns N'' Roses', 'BANDA'
where not exists (select 1 from artista where nome = 'Guns N'' Roses' and tipo = 'BANDA');

-- ===== Álbuns =====
insert into album (id, titulo, ano_lancamento)
select '0c8878a5-8483-4780-8cda-915180f8ac41'::uuid, 'Harakiri', null
where not exists (select 1 from album where titulo = 'Harakiri');

insert into album (id, titulo, ano_lancamento)
select 'a2f5bf42-bae7-4bad-b979-7c1b70d1aed2'::uuid, 'Black Blooms', null
where not exists (select 1 from album where titulo = 'Black Blooms');

insert into album (id, titulo, ano_lancamento)
select '55fab744-53f4-428e-8e6d-f679ba545fa7'::uuid, 'The Rough Dog', null
where not exists (select 1 from album where titulo = 'The Rough Dog');

insert into album (id, titulo, ano_lancamento)
select 'b0027bf9-e439-43dc-8c17-5843cda4f38e'::uuid, 'The Rising Tied', null
where not exists (select 1 from album where titulo = 'The Rising Tied');

insert into album (id, titulo, ano_lancamento)
select '7b73cfaa-4eb9-4dff-aef8-8ea145c22943'::uuid, 'Post Traumatic', null
where not exists (select 1 from album where titulo = 'Post Traumatic');

insert into album (id, titulo, ano_lancamento)
select '313d45b1-06f7-42d1-942e-78f05c8907b4'::uuid, 'Post Traumatic EP', null
where not exists (select 1 from album where titulo = 'Post Traumatic EP');

insert into album (id, titulo, ano_lancamento)
select '69e19808-c1bb-46a0-a2c8-e172b0e7f94b'::uuid, 'Where''d You Go', null
where not exists (select 1 from album where titulo = 'Where''d You Go');

insert into album (id, titulo, ano_lancamento)
select '1a7cc48d-9c61-45c0-bccd-5bff48133d65'::uuid, 'Bem Sertanejo', null
where not exists (select 1 from album where titulo = 'Bem Sertanejo');

insert into album (id, titulo, ano_lancamento)
select 'f5c9a7c5-677f-41d5-80b0-70ff9501fe4c'::uuid, 'Bem Sertanejo - O Show (Ao Vivo)', null
where not exists (select 1 from album where titulo = 'Bem Sertanejo - O Show (Ao Vivo)');

insert into album (id, titulo, ano_lancamento)
select '3e8d3d3a-0d5c-415c-9d60-9f4c4c39f2e4'::uuid, 'Bem Sertanejo - (1ª Temporada) - EP', null
where not exists (select 1 from album where titulo = 'Bem Sertanejo - (1ª Temporada) - EP');

insert into album (id, titulo, ano_lancamento)
select 'f72c2b6b-7c56-4f5c-bbe0-9132d7edb9f9'::uuid, 'Use Your Illusion I', null
where not exists (select 1 from album where titulo = 'Use Your Illusion I');

insert into album (id, titulo, ano_lancamento)
select '5b6d8f75-6e67-4a38-aaf1-0b2f0c88b6d1'::uuid, 'Use Your Illusion II', null
where not exists (select 1 from album where titulo = 'Use Your Illusion II');

insert into album (id, titulo, ano_lancamento)
select '8c2e7e45-73ea-4206-9f2f-7b8a2e6b1f57'::uuid, 'Greatest Hits', null
where not exists (select 1 from album where titulo = 'Greatest Hits');

-- ===== Vínculos N:N =====
-- Serj Tankian
insert into artista_album (artista_id, album_id)
select '21293936-27cc-4a98-94c4-45f86c929711'::uuid, '0c8878a5-8483-4780-8cda-915180f8ac41'::uuid
where not exists (select 1 from artista_album where artista_id='21293936-27cc-4a98-94c4-45f86c929711'::uuid and album_id='0c8878a5-8483-4780-8cda-915180f8ac41'::uuid);

insert into artista_album (artista_id, album_id)
select '21293936-27cc-4a98-94c4-45f86c929711'::uuid, 'a2f5bf42-bae7-4bad-b979-7c1b70d1aed2'::uuid
where not exists (select 1 from artista_album where artista_id='21293936-27cc-4a98-94c4-45f86c929711'::uuid and album_id='a2f5bf42-bae7-4bad-b979-7c1b70d1aed2'::uuid);

insert into artista_album (artista_id, album_id)
select '21293936-27cc-4a98-94c4-45f86c929711'::uuid, '55fab744-53f4-428e-8e6d-f679ba545fa7'::uuid
where not exists (select 1 from artista_album where artista_id='21293936-27cc-4a98-94c4-45f86c929711'::uuid and album_id='55fab744-53f4-428e-8e6d-f679ba545fa7'::uuid);

-- Mike Shinoda
insert into artista_album (artista_id, album_id)
select '84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid, 'b0027bf9-e439-43dc-8c17-5843cda4f38e'::uuid
where not exists (select 1 from artista_album where artista_id='84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid and album_id='b0027bf9-e439-43dc-8c17-5843cda4f38e'::uuid);

insert into artista_album (artista_id, album_id)
select '84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid, '7b73cfaa-4eb9-4dff-aef8-8ea145c22943'::uuid
where not exists (select 1 from artista_album where artista_id='84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid and album_id='7b73cfaa-4eb9-4dff-aef8-8ea145c22943'::uuid);

insert into artista_album (artista_id, album_id)
select '84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid, '313d45b1-06f7-42d1-942e-78f05c8907b4'::uuid
where not exists (select 1 from artista_album where artista_id='84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid and album_id='313d45b1-06f7-42d1-942e-78f05c8907b4'::uuid);

insert into artista_album (artista_id, album_id)
select '84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid, '69e19808-c1bb-46a0-a2c8-e172b0e7f94b'::uuid
where not exists (select 1 from artista_album where artista_id='84dd54b9-fda3-4e19-91d6-ce7bf8f0233c'::uuid and album_id='69e19808-c1bb-46a0-a2c8-e172b0e7f94b'::uuid);

-- Michel Teló
insert into artista_album (artista_id, album_id)
select '6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid, '1a7cc48d-9c61-45c0-bccd-5bff48133d65'::uuid
where not exists (select 1 from artista_album where artista_id='6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid and album_id='1a7cc48d-9c61-45c0-bccd-5bff48133d65'::uuid);

insert into artista_album (artista_id, album_id)
select '6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid, 'f5c9a7c5-677f-41d5-80b0-70ff9501fe4c'::uuid
where not exists (select 1 from artista_album where artista_id='6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid and album_id='f5c9a7c5-677f-41d5-80b0-70ff9501fe4c'::uuid);

insert into artista_album (artista_id, album_id)
select '6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid, '3e8d3d3a-0d5c-415c-9d60-9f4c4c39f2e4'::uuid
where not exists (select 1 from artista_album where artista_id='6c09ebf6-ee5c-4c5f-af01-bc7acea00bee'::uuid and album_id='3e8d3d3a-0d5c-415c-9d60-9f4c4c39f2e4'::uuid);

-- Guns N' Roses
insert into artista_album (artista_id, album_id)
select '8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid, 'f72c2b6b-7c56-4f5c-bbe0-9132d7edb9f9'::uuid
where not exists (select 1 from artista_album where artista_id='8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid and album_id='f72c2b6b-7c56-4f5c-bbe0-9132d7edb9f9'::uuid);

insert into artista_album (artista_id, album_id)
select '8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid, '5b6d8f75-6e67-4a38-aaf1-0b2f0c88b6d1'::uuid
where not exists (select 1 from artista_album where artista_id='8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid and album_id='5b6d8f75-6e67-4a38-aaf1-0b2f0c88b6d1'::uuid);

insert into artista_album (artista_id, album_id)
select '8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid, '8c2e7e45-73ea-4206-9f2f-7b8a2e6b1f57'::uuid
where not exists (select 1 from artista_album where artista_id='8ea4bebe-a071-4c52-893d-7c4c5e14b47b'::uuid and album_id='8c2e7e45-73ea-4206-9f2f-7b8a2e6b1f57'::uuid);

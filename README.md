###### **Seletivo SEPLAG - Edital 001/2026**

---

## Dados de Inscrição

- **Nome:** Andrei Falbot Mendes  
- **Email:** falbot@gmail.com  
- **Inscrição:** 16538 
- **Função/Perfil:** Analista de Tecnologia da Informação - Engenheiro da Computação (Sênior)
- **Projeto Escolhido:** ANEXO II - _A_ - Projeto **DESENVOLVEDOR BACK END**

## Sumário

- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Visão Geral e Arquitetura](#visão-geral-e-arquitetura)
- [Instruções de Execução](#instruções-de-execução)
- [Documentação da API e Endpoints](#documentação-da-api-e-endpoints)
- [Testes Unitários e de Integração](#testes-unitários-e-de-integração)
- [Justificativa Técnica](#justificativa-técnica)

---

## Tecnologias Utilizadas

- **Java 17**: versão LTS estável e amplamente adotada em ambientes corporativos; suporte a recursos modernos da linguagem e compatibilidade com o ecossistema Spring.
- **Spring Boot 4.0.1**: core da aplicação (API REST), com auto-configuração, padrão de projeto em camadas e integração madura com Security, Data JPA, Web e Observabilidade.
- **PostgreSQL 17**: SGBD relacional robusto e aderente a cenários transacionais; usado como banco principal da aplicação.
- **Flyway**: garante **versionamento de schema**, reprodutibilidade de ambiente e consistência entre dev/test/prod por meio de migrations (DDL/seed).
- **MinIO (S3 compatível)**: armazenamento de objetos para **capas de álbuns**, seguindo uma abordagem escalável (blob fora do banco) e compatível com padrões S3 (inclui geração de **presigned URLs**).
- **Docker & Docker Compose**: padroniza a execução local e reduz “na minha máquina funciona”, entregando PostgreSQL/MinIO/backend com configuração reproduzível.
- **Testcontainers**: utilizado na suíte de testes para subir infraestrutura real (ex.: PostgreSQL/MinIO) e aumentar a **fidelidade ao ambiente de produção** em testes de integração/fluxos.
- **Swagger UI / OpenAPI**: documentação navegável e contrato da API, facilitando validação do avaliador e consumo por clientes.
- **JWT (Authorization: Bearer)**: autenticação stateless, com expiração e renovação (refresh), mantendo a API desacoplada de sessão no servidor.

---

## Visão Geral e Arquitetura

Este projeto foi estruturado com **organização em camadas** e separação explícita de responsabilidades, visando:

- Manter o domínio e as regras de negócio isolados da borda HTTP.
- Facilitar testes (unitários e de integração).
- Sustentar evolução do contrato REST (versionamento de endpoints).
- Integrar serviços externos (MinIO e Regionais) sem acoplamento excessivo.

### Camadas (organização por responsabilidade)

A estrutura do código reflete a arquitetura em camadas exigida no edital:

- **Controller (camada HTTP / API v1)** (`br.com.falbot.seplag.backend.api`)
  Define endpoints REST em `/api/v1/**`.
  Realiza validação de entrada, paginação/filtros e traduz a requisição HTTP para um caso de uso (Service).
  Retorna DTOs de resposta, mantendo contrato claro e estável.
  Erros são padronizados por handler global (`api/erro`) para consistência de status codes e mensagens.

- **Service (regras de negócio / casos de uso)** (`br.com.falbot.seplag.backend.servico`)
  Implementa a lógica central do sistema: CRUD, paginação/filtros, upload/gestão de capas, emissão de eventos via WebSocket e sincronização de regionais.
  Orquestra integrações (MinIO, WebSocket e endpoint externo) e define o fluxo transacional do caso de uso.

- **Repository (persistência)** (`br.com.falbot.seplag.backend.repositorio`)
  Acesso a dados com Spring Data JPA e consultas (ex.: paginação e filtros/Specifications quando aplicável).
  Isola detalhes de persistência para não “vazar infra” para a regra de negócio.

- **Domínio (modelagem do problema)** (`br.com.falbot.seplag.backend.dominio`)
  Entidades JPA e enums (ex.: Artista, Álbum).
  Mantém o núcleo do modelo separado do contrato HTTP.

Camadas transversais:

- **Segurança** (`seguranca`): JWT, filtros e configuração de autorização; CORS/allowlist e rate limit como proteções de borda.
- **Configuração/Infra** (`config`): OpenAPI, WebSocket, S3/MinIO, Security e RestClient.

### Isolamento de Domínio (por que DTOs)

O projeto adota **DTOs (Data Transfer Objects)** (`api/dto`) como contratos de entrada/saída da API para reduzir acoplamento e controlar exposição de dados:

- **Redução de acoplamento**: entidades de domínio não são expostas diretamente na borda HTTP (evita dependência do cliente em detalhes internos).
- **Contrato explícito**: requests/responses deixam claro o que o cliente pode enviar/receber, facilitando validação e evolução.
- **Governança e segurança**: evita vazamento acidental de atributos e mantém a superfície pública controlada.

---

## Detalhamento da Estrutura

```
andreifalbotmendes964598-main/
├── README.md
├── infra/
│   └── docker-compose.yml              # Infra local (PostgreSQL/MinIO/backend) via Docker Compose
└── backend/
    ├── Dockerfile                      # Build/execução do backend em container
    ├── pom.xml                         # Dependências e build (Maven)
    └── src/
        ├── main/
        │   ├── java/br/com/falbot/seplag/backend/
        │   │   ├── api/                # Camada HTTP: Controllers, rotas, validação, paginação, retorno de respostas
        │   │   │   ├── dto/            # Contratos da API: Request/Response DTOs (evita expor entidades)
        │   │   │   └── erro/           # Tratamento de erros: handler global e respostas padronizadas
        │   │   ├── servico/            # Casos de uso/regras: orquestra transações e integrações (MinIO/WebSocket/externo)
        │   │   ├── repositorio/        # Persistência: Spring Data JPA + consultas/Specifications
        │   │   ├── dominio/            # Domínio: entidades JPA e enums (Artista, Álbum, etc.)
        │   │   ├── seguranca/          # Segurança: JWT, filtros (auth/origin/rate limit) e suporte à autorização
        │   │   └── config/             # Configuração: beans e infra (OpenAPI, WebSocket, RestClient, S3, Security)
        │   └── resources/
        │       ├── application.properties  # Configs da aplicação
        │       └── db/migration/           # Migrations Flyway (DDL/seed)
        └── test/
            └── java/br/com/falbot/seplag/backend/  # Testes automatizados (integração/fluxos)
```

---

## Instruções de Execução

### Pré-requisitos

- Docker instalado

- Docker Compose instalado (plugin `docker compose`)

### Como rodar a aplicação (API + PostgreSQL + MinIO)

1. Clone o repositório:
   
   ```bash
   git clone https://github.com/Falbot/andreifalbotmendes964598.git
   ```

2. Verifique se o Docker está em execução:
   
   ```bash
   **Windows (CMD):**
   docker info > nul 2>&1 || (echo "Docker não está em execução. Abra o Docker Desktop e tente novamente." & exit /b 1)
   docker compose version

   **Linux/macOS:**
   docker info >/dev/null 2>&1 || { echo "Docker não está em execução. Inicie o Docker e tente novamente."; exit 1; }
   docker compose version
   ```

3. Suba os containers:
   
   ```bash
   cd andreifalbotmendes964598\infra\
   docker compose up -d --build
   docker compose ps
   ```

4. Acompanhe os logs:
   
   ```bash
   docker compose logs -f backend
   ```

> O banco de dados é criado e populado automaticamente no primeiro start **(Flyway)**, com os exemplos do edital. Seed: `backend/src/main/resources/db/migration/V9__seed_exemplos.sql`

> Parar e limpar ambiente:
> 
> ```bash
> docker compose down
> ```

> Remover também os volumes (apaga dados do banco e do MinIO):
> 
> ```bash
> docker compose down -v
> ```

Endereços úteis

| Serviço            | URL                                                                                                |
| ------------------ | -------------------------------------------------------------------------------------------------- |
| API (backend)      | [http://localhost:8081](http://localhost:8081)                                                     |
| Swagger UI         | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)                     |
| OpenAPI            | [http://localhost:8081/api-docs](http://localhost:8081/api-docs)                                   |
| Health (readiness) | [http://localhost:8081/actuator/health/readiness](http://localhost:8081/actuator/health/readiness) |
| MinIO (API S3)     | [http://localhost:9000](http://localhost:9000)                                                     |
| MinIO Console      | [http://localhost:9001](http://localhost:9001) (user: minio \| password: minio12345)               |
| PostgreSQL         | localhost:5432 (database: app \| user: app \| password: app)                                       |

---

## Documentação da API e Endpoints

### Swagger / OpenAPI

Com os containers ativos, a documentação fica disponível em:

- **Swagger UI:** http://localhost:8081/swagger-ui.html  
- **OpenAPI (JSON):** http://localhost:8081/api-docs

> Observação: a configuração do springdoc está direcionada para documentar apenas os endpoints versionados em `/api/v1/**`.

### Endpoints principais (resumo)

- Auth: `/api/v1/autenticacao/*` (login/refresh/registro)
- Artistas: `/api/v1/artistas` (CRUD + paginação/filtros)
- Álbuns: `/api/v1/albuns` (CRUD + paginação/filtros)
- Capas: `/api/v1/albuns/{id}/capas` (upload + listagem + presigned URL)
- Regionais: `/api/v1/regionais/*` (sincronização + consulta)
- Health: `/actuator/health/*`

### Versionamento de Endpoints

O projeto adota versionamento por **prefixo de rota**.

- Base: `/api/v1/...`

Exemplos:

- `GET /api/v1/artistas`
- `GET /api/v1/albuns`

> Motivo: permite evolução da API sem quebrar consumidores (ex.: evolução/novas apis `/api/v2/...`).

### WebSocket (notificação de novo álbum)

O backend expõe um endpoint de WebSocket (SockJS + STOMP):

- **Handshake:** `/ws`
- **Tópico:** `/topic/albuns`

### Como testar (modo fácil)

Existe uma página de teste já incluída no projeto. Para verificar o comportamento em **sessões diferentes**, use duas janelas/abas:

1. Abra uma aba **normal** e acesse: http://localhost:8081/ws-teste.html  
2. Abra uma aba **anônima/privada** e acesse o mesmo endereço: http://localhost:8081/ws-teste.html  

Na aba **normal**:
3. Clique em **Conectar WS** (assina `/topic/albuns`)  
4. Clique em **Registrar** (cria usuário) e depois **Login**

Na aba **anônima/privada**:
5. Clique em **Login** (ou **Registrar** se preferir outro usuário)  
6. Clique em **Criar via REST** (cria um novo álbum)

Resultado esperado:

7. A aba **normal** deve receber imediatamente no log a notificação do tópico `/topic/albuns` (evento de “álbum criado”).

---

## Testes Unitários e de Integração

O edital exige **testes unitários e de integração** com cobertura mínima nos módulos principais (perfil sênior).  
A suíte atual foca em fluxos críticos: autenticação, paginação/consultas, upload/MinIO, WebSocket, CORS/origin allowlist, rate limit e sincronização de regionais.

### Pré-requisito para testes

Os testes foram escritos com **JUnit 5** e **Spring Boot Test**.A suíte contém **testes unitários** (serviços) e **testes de integração** (API via MockMvc).

- **Docker em execução** (alguns testes usam infraestrutura em container / Testcontainers).

> Importante: os testes de integração usam **Testcontainers** (PostgreSQL + MinIO), então é necessário ter **Docker rodando** (ex.: Docker Desktop).

### Como rodar os testes

**Windows (PowerShell / CMD):**

```bash
cd backend
.\mvnw test
```

**Linux/macOS:**

```bash
cd backend
./mvnw test
```

> Dica: para rodar uma classe específica:
> 
> ```bash
> .\mvnw test -Dtest=AutenticacaoFlowTests
> ```

### Principais testes automatizados

- `AutenticacaoFlowTests` — fluxo completo de **registro/login/refresh token**.
- `SecurityAuthorizationTests` — valida **rotas protegidas** e cenários de autorização.
- `CatalogoConsultasTests` — valida **paginação/filtros** em listagens (artistas/álbuns).
- `CapaAlbumFlowTests` — valida upload/listagem/remoção de **capas no MinIO** + links presignados.
- `AlbumServicoWsTests` — valida **evento WebSocket** ao criar álbum (`/topic/albuns`).
- `RegionaisSyncFlowTests` — valida **sincronização de regionais** (inserir/inativar/reinserir).
- `CorsAllowlistTests` — valida **allowlist de origins (CORS)**.
- `RateLimitFilterTests` — valida **rate limit** em endpoints protegidos.
- `ApiVersioningTests` — valida **versionamento por rota** (`/api/v1/...`).
- `RestStatusCodeTests` — valida **contratos de status code** e erros padronizados.

#### Como validar especificamente a Sincronização de Regionais

- Teste automatizado: `RegionaisSyncFlowTests`
- O que ele valida: execução idempotente e regras de negócio de **inserir / inativar / reinserir** (preservando histórico via inativação lógica e versionamento, sem delete físico).

Rodar apenas esse teste:

```bash
cd backend
.\mvnw test -Dtest=RegionaisSyncFlowTests
```

Validação manual (opcional):

- Com os containers ativos, acesse o **Swagger UI** e execute o fluxo de endpoints em `/api/v1/regionais/*` (consulta + sincronização).
- Reexecute a sincronização para confirmar **idempotência** (não deve gerar alterações quando não há divergências com a origem).

## Requisitos do Edital (checklist)

```md
- [x] Organização em camadas (controller/service/repository/model)
- [x] Migrations + carga inicial (Flyway)
- [x] Swagger/OpenAPI
- [x] CRUD Artista/Álbum + paginação e filtros
- [x] JWT com expiração e renovação
- [x] Upload de capas + MinIO + presigned URL
- [x] Segurança: CORS/origin allowlist + rate limit
- [x] WebSocket de notificação de novo álbum
- [x] Sincronização de Regionais (inserir/inativar/reinserir)
- [x] Health checks / liveness-readiness
- [x] Testes unitários e de integração (módulos principais)
```

---

## Justificativa Técnica

Esta seção registra **decisões de engenharia**, **trade-offs** e **priorização**, para dar transparência ao avaliador sobre o porquê das escolhas (inclusive quando um requisito eventualmente não é implementado).

### Priorização (como organizei a entrega)

A sequência de entrega foi pensada para maximizar **reprodutibilidade**, **aderência ao edital** e **segurança**, reduzindo risco de “funciona só na minha máquina”:

1) **Infra reproduzível** (Docker Compose) + **migrations** (Flyway) para garantir execução determinística do schema e seed.  
2) **Núcleo do domínio** (CRUD + paginação/filtros) com camadas bem definidas (Controller/Service/Repository/DTO).  
3) **Integrações obrigatórias**: MinIO (capas) + WebSocket + endpoint externo de Regionais.  
4) **Hardening da borda**: JWT, CORS allowlist e rate limit.  
5) **Testes automatizados** cobrindo fluxos críticos e integração com infraestrutura real (Testcontainers).

### Sincronização de Regionais (menor complexidade algorítmica + histórico)

A sincronização com o endpoint da Polícia Civil (`/v1/regionais`) foi projetada para ser:

- **Idempotente**: executar repetidamente não deve produzir efeitos colaterais indevidos;  
- **Rastreável**: preservar histórico (evitar “editar o passado”);  
- **Eficiente**: evitar algoritmos quadráticos e operações destrutivas.

### Estratégia de menor complexidade (O(n + m))

A rotina adota **comparação de estados** entre:

- `n` itens vindos da API externa (lista de regionais),
- `m` itens existentes na base local.

Para manter custo **linear O(n+m)**, a sincronização indexa os conjuntos por `id` (Map/Set) e toma decisões determinísticas:

- **Inserir** quando existe na origem e não existe localmente;
- **Inativar (soft-delete)** quando existe localmente e não existe mais na origem;
- **Versionar** quando existe em ambos, porém com mudanças relevantes (ex.: nome):  
  o registro antigo é marcado como `ativo = false` e um novo registro é inserido, preservando histórico, evitando operações destrutivas e favorecendo auditoria e rastreabilidade;
- **Reinserir / reativar** quando um `id` retorna na origem após ter sido inativado localmente (conforme regra de negócio do edital).

> Observação: a opção por **inativação lógica** (sem delete físico) e por **versionamento** reduz risco de quebra de integridade referencial e favorece auditoria.

### Onde fica a lógica e por quê

- A orquestração está na camada de **Service**, para concentrar regras de negócio e manter Controllers enxutos.
- DTOs são usados como **contrato de borda**, evitando acoplamento do domínio com o payload externo e controlando validação/sanitização.

### Upload de capa

- **Por que não usou Base64? **
    Aumenta payload, consumo de memória e tempo de processamento; para “produção”, multipart/presigned é mais adequado.

---

## O que foi / não foi feito

O checklist do edital está marcado como concluído nesta entrega.  
Se algum item ficar de fora por tempo/escopo, ele será registrado aqui no formato:

- **Item não implementado:** <descrição do requisito>  
  **Motivo:** <tempo/risco/escopo/decisão técnica>  
  **Como evoluiria:** <passos objetivos para implementação futura>  

---

## Evoluções possíveis

- Upload de capa via Base64 (ex.: /api/v2/...) para cenários sem multipart.
- Melhorias de documentação Swagger para parâmetros de consulta (descrição/exemplos).
- Expandir massa de dados de seed (caso desejável), sem alterar requisitos.

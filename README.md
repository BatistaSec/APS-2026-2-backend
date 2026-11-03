# Estação de Monitoramento Ambiental — APS

API REST em **Java 21 puro** (sem Spring), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap** para qualquer cidade do mundo. Inclui frontend web com mapa interativo.

---

## 🚀 Como Fazer Funcionar

### Pré-requisitos

| Ferramenta | Versão mínima | Download |
|---|---|---|
| Java (JDK) | 21 | [adoptium.net](https://adoptium.net/) |
| Maven | 3.8 | [maven.apache.org](https://maven.apache.org/download.cgi) |
| MySQL | 8.0 | [dev.mysql.com](https://dev.mysql.com/downloads/mysql/) |

Chave de API gratuita da OpenWeatherMap:
1. Crie conta em [openweathermap.org](https://home.openweathermap.org/users/sign_up)
2. Copie a chave em [openweathermap.org/api_keys](https://home.openweathermap.org/api_keys) (ativa em até 2h)

---

### Passo a passo

**1. Clone o repositório**
```bash
git clone https://github.com/BatistaSec/APS-2026-2-backend.git
cd APS-2026-2-backend
```

**2. Configure as credenciais**

Abra `src/main/resources/hibernate.cfg.xml` e ajuste:
```xml
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">sua_senha_do_mysql</property>
```

A chave da OpenWeatherMap fica em `OpenWeatherClientImpl.java`, método `obterApiKeyConfigurada()`.

**3. Inicie o servidor**
```bash
mvn compile exec:java
```

Na **primeira execução**, o `hbm2ddl.auto=create` recria as tabelas do zero. Nas execuções seguintes troque para `update` no `hibernate.cfg.xml` para preservar os dados.

**4. Acesse no browser**

| URL | O que abre |
|---|---|
| `http://localhost:8080` | Frontend com busca e mapa |
| `http://localhost:8080/health` | Status do sistema |
| `http://localhost:8080/api` | Documentação JSON dos endpoints |

---

### Problemas comuns

**`Access denied for user 'root'`** — senha errada no `hibernate.cfg.xml`. Teste: `mysql -u root -p`

**`Cidade não encontrada`** — use o nome em inglês (ex: `Sao Paulo`, `Tokyo`, `Paris`)

**`401` nas chamadas à OpenWeatherMap** — a chave demora até 2h para ativar após o cadastro

**Porta 8080 em uso** — defina a variável `PORT` antes de rodar: `$env:PORT=9090; mvn compile exec:java`

---

## 🗄️ Modelo de Dados

O sistema usa três tabelas relacionadas. Cada medição está vinculada a uma estação, que pertence a uma cidade.

```
cidades
  id (PK) | nome | pais

estacoes
  id (PK, texto) | cidade_id (FK → cidades) | tipo (OWM / MANUAL)

medicoes_ambientais
  id (PK) | estacao_id (FK → estacoes) | data_hora
  temperatura | umidade | precipitacao | velocidade_vento
  pressao_atmosferica | direcao_vento | indice_uv | sensacao_termica
  origem (COLETOR | PESQUISA | POST | SINCRONIZACAO | MANUAL)
```

### Campo `origem`

Rastreia qual dos 5 gatilhos gerou cada registro:

| Valor | Quando é usado |
|---|---|
| `COLETOR` | Coletor automático a cada 10 s |
| `PESQUISA` | Frontend pesquisou via `GET /atual?cidade=X` |
| `POST` | `POST /medicoes` com localização |
| `SINCRONIZACAO` | `POST /sincronizacoes` (lote) |
| `MANUAL` | `POST /medicoes` sem cidade |

### Exemplo de JOIN

```sql
SELECT m.*, e.id AS estacao, c.nome AS cidade, c.pais
FROM medicoes_ambientais m
JOIN estacoes e ON m.estacao_id = e.id
JOIN cidades c ON e.cidade_id = c.id
WHERE c.nome = 'Sao Paulo'
ORDER BY m.data_hora DESC;
```

---

## 🏛️ Estrutura de Pacotes

```text
src/main/java/com/app
├── Main.java
├── model
│   ├── Cidade.java                             # Entidade JPA — tabela cidades (id, nome, pais)
│   ├── Estacao.java                            # Entidade JPA — tabela estacoes (FK → cidades)
│   ├── MedicaoAmbiental.java                   # Entidade JPA — tabela medicoes_ambientais
│   └── dto
│       ├── request
│       │   ├── MedicaoRequest.java
│       │   └── SincronizacaoRequest.java
│       └── response
│           ├── MedicaoResponse.java
│           ├── EstatisticasClimaticas.java
│           ├── PaginatedResponse.java
│           └── ProblemDetails.java             # Erro padronizado RFC 7807
├── dao
│   ├── EstacaoRepository.java                  # Upsert thread-safe de Cidade + Estacao
│   ├── salvar/    SalvarMedicaoDao(Impl)
│   ├── consulta/  ConsultaMedicaoDao(Impl)      # Buscas com JOIN nas 3 tabelas
│   └── exclusao/  DeletarMedicaoDao(Impl)
├── service
│   ├── calculo/   CalculadoraSensacaoTermica    # Wind Chill e Heat Index
│   ├── registro/  RegistroMedicaoService(Impl)  # Valida, resolve Estacao/Cidade e salva
│   ├── estatistica/ EstatisticaService(Impl)
│   ├── openweather/ OpenWeatherClient(Impl)     # Consulta OWM com retry
│   ├── coleta/    ColetorAgendadoService(Impl)  # Coleta automática a cada 10 s
│   └── MonitoramentoService.java               # Fachada
├── handler
│   ├── MedicaoApiHandler.java                  # Dispatcher GET / POST / DELETE
│   ├── health/    HealthHandler
│   ├── http/request/  HttpRequest(Impl)
│   ├── http/response/ HttpResponseSender(Impl)  # CORS, GZIP, UTF-8
│   ├── operacoes/get/    MedicaoGetHandler(Impl)
│   ├── operacoes/post/   MedicaoPostHandler(Impl)
│   ├── operacoes/delete/ MedicaoDeleteHandler(Impl)
│   └── proxy/
│       ├── FrontendStaticHandler
│       └── WeatherProxyHandler
└── util
    ├── HibernateUtil.java
    ├── JsonUtil.java
    ├── database/  DatabaseConnectionManager
    └── logging/   AsyncLogger
```

---

## ⚙️ Os 4 Pilares do Sistema

**1. Coleta automática**
O `ColetorAgendadoService` coleta a cada 10 segundos, rotacionando entre cidades do estado de São Paulo: São Paulo, Campinas, Santos, São José dos Campos, Ribeirão Preto, Sorocaba, Osasco, Santo André, São Bernardo do Campo, Guarulhos. Registros com `origem = COLETOR`.

**2. Cálculo de sensação térmica**
- **Wind Chill** para temperatura ≤ 10 °C
- **Heat Index** (Steadman) para temperatura ≥ 26 °C com umidade ≥ 40 %
- Temperatura real preservada nos demais casos

**3. Estatísticas**
Uma única query HQL com `COUNT`, `MIN`, `MAX`, `AVG` — sem N+1.

**4. Registro e persistência**
O `RegistroMedicaoService` valida os dados, resolve `Cidade` e `Estacao` via `EstacaoRepository` (upsert thread-safe com tratamento de race condition), calcula a sensação térmica e persiste via Hibernate 6 com transações ACID.

---

## 🌤️ Dados por Medição

| Campo | Tipo | Descrição |
|---|---|---|
| `cidade` | String (via JOIN) | Nome da cidade |
| `estacaoId` | String (FK) | ID da estação |
| `dataHora` | LocalDateTime | Data e hora |
| `temperatura` | Double (°C) | Temperatura ambiente |
| `sensacaoTermica` | Double (°C) | Calculada pelo backend |
| `umidade` | Double (%) | Umidade relativa |
| `precipitacao` | Boolean | Chuva detectada |
| `velocidadeVento` | Double (km/h) | Velocidade do vento |
| `direcaoVento` | String | N, NE, L, SE, S, SO, O, NO |
| `pressaoAtmosferica` | Double (hPa) | Pressão barométrica |
| `indiceUv` | Double (0–12+) | Índice UV |
| `origem` | Enum | Gatilho que gerou o registro |

---

## 📮 Endpoints da API

> Base: `http://localhost:8080` — prefixo `/api/v1/`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/medicoes` | Insere medição (busca OWM se cidade informada) |
| `POST` | `/api/v1/sincronizacoes` | Insere lote de cidades |
| `POST` | `/api/v1/coletor/cidades` | Adiciona cidade ao coletor |
| `POST` | `/api/v1/coletor/execucoes` | Força ciclo imediato de coleta |
| `GET` | `/api/v1/medicoes` | Histórico paginado com filtros |
| `GET` | `/api/v1/medicoes/{id}` | Medição por ID |
| `GET` | `/api/v1/medicoes/atual?cidade=X` | Clima agora (salva e retorna) |
| `GET` | `/api/v1/medicoes/estatisticas` | Estatísticas agregadas |
| `GET` | `/api/v1/cidades` | Lista cidades com medições |
| `GET` | `/api/v1/cidades/{cidade}/medicoes` | Medições de uma cidade |
| `GET` | `/api/v1/cidades/{cidade}/estatisticas` | Estatísticas de uma cidade |
| `GET` | `/api/v1/estacoes/{id}/medicoes` | Medições de uma estação |
| `DELETE` | `/api/v1/medicoes/{id}` | Remove por ID |
| `DELETE` | `/api/v1/medicoes?cidade=X` | Remove por cidade |
| `GET` | `/health` | Status do sistema (banco, JVM) |

### Filtros do histórico paginado

```
GET /api/v1/medicoes?page=1&pageSize=20&cidade=Santos&sort=temperatura&order=DESC
```

Parâmetros: `cidade`, `tempMin`, `tempMax`, `precipitacao`, `sort` (datahora | temperatura | umidade | cidade), `order` (ASC | DESC), `page`, `pageSize`

### Inserir medição com localização

```json
{ "cidade": "Sao Paulo", "pais": "BR" }
```
```json
{ "cidade": "Tokyo", "pais": "JP" }
```

Campos opcionais para sobrescrever valores da OWM: `temperatura`, `umidade`, `precipitacao`, `velocidadeVento`, `pressaoAtmosferica`, `direcaoVento`, `indiceUv`, `dataHora`.

---

## ⚙️ Configuração

Credenciais em `src/main/resources/hibernate.cfg.xml`.

| `hbm2ddl.auto` | Quando usar |
|---|---|
| `create` | Primeira execução ou reset — recria tabelas do zero |
| `update` | Uso normal — preserva os dados |

---

## 🔧 Otimizações

| Otimização | Detalhe |
|---|---|
| **3 tabelas relacionadas** | `cidades → estacoes → medicoes_ambientais` com FK |
| **Upsert thread-safe** | `EstacaoRepository` trata race condition com retry em `ConstraintViolationException` |
| **Campo `origem`** | Rastreia qual dos 5 gatilhos gerou cada medição |
| **JOIN FETCH** | Queries carregam cidade e estação via JOIN — sem lazy-loading fora de sessão |
| **Sem N+1** | Estatísticas em uma única query HQL (COUNT, MIN, MAX, AVG) |
| **JDBC Batching** | Lotes de 25 inserts |
| **GZIP** | Compressão automática quando cliente suporta `Accept-Encoding: gzip` |
| **Virtual Threads** | `newVirtualThreadPerTaskExecutor()` do Java 21 |
| **AsyncLogger** | Fila de 4096 eventos — logs não bloqueiam threads HTTP |

---

## 🏛️ Princípios SOLID

- **S** — Cada classe tem uma única responsabilidade (`SalvarMedicaoDao`, `ConsultaMedicaoDao`, `DeletarMedicaoDao`, `MedicaoGetHandler`, etc.)
- **O** — Extensível via interfaces sem modificar contratos existentes
- **L** — Implementações substituíveis pelas interfaces sem quebrar o sistema
- **I** — Interfaces pequenas e específicas por operação
- **D** — Módulos de alto nível dependem de abstrações, não de implementações concretas

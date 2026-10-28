# Backend: Estação de Monitoramento Ambiental (APS)

API REST desenvolvida em **Java 21 Puro** (sem frameworks pesados), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap** para **qualquer cidade do mundo**.

O projeto adota rigorosamente os princípios **SOLID**:
- **S**ingle Responsibility Principle (SRP): Cada classe e interface tem uma única e bem definida razão para mudar.
- **O**pen/Closed Principle (OCP): Extensível por interfaces sem modificar os contratos existentes.
- **L**iskov Substitution Principle (LSP): As implementações podem ser substituídas por suas interfaces sem quebrar contratos.
- **I**nterface Segregation Principle (ISP): Interfaces pequenas e específicas (`SalvarMedicaoDao`, `ConsultaMedicaoDao`, `DeletarMedicaoDao`, `MedicaoGetHandler`, etc.).
- **D**ependency Inversion Principle (DIP): Módulos de alto nível dependem de abstrações (interfaces), não de implementações concretas.

---

## 🏛️ Estrutura de Pacotes

```text
src/main/java/com/app
├── Main.java                                    # Inicialização do servidor HTTP, EnvLoader e coletor
├── model
│   ├── MedicaoAmbiental.java                   # Entidade JPA mapeada para a tabela no MySQL
│   └── dto
│       ├── request
│       │   ├── MedicaoRequest.java             # DTO de entrada (cidade, estado, país, campos opcionais)
│       │   └── SincronizacaoRequest.java       # DTO de entrada para lote de cidades/estados
│       └── response
│           ├── MedicaoResponse.java            # DTO de saída de uma medição
│           ├── EstatisticasClimaticas.java     # DTO de saída das estatísticas
│           ├── PaginatedResponse.java          # Wrapper genérico de paginação
│           └── ProblemDetails.java             # Erro padronizado RFC 7807
├── dao
│   ├── salvar/                                 # SRP & ISP: Persistência de medições
│   │   ├── SalvarMedicaoDao.java
│   │   └── SalvarMedicaoDaoImpl.java
│   ├── consulta/                               # SRP & ISP: Buscas, filtros e listagens
│   │   ├── ConsultaMedicaoDao.java
│   │   └── ConsultaMedicaoDaoImpl.java
│   └── exclusao/                               # SRP & ISP: Remoção por ID e por cidade
│       ├── DeletarMedicaoDao.java
│       └── DeletarMedicaoDaoImpl.java
├── service
│   ├── calculo/                                # SRP: Cálculo de sensação térmica (Wind Chill & Heat Index)
│   │   ├── CalculadoraSensacaoTermica.java
│   │   └── CalculadoraSensacaoTermicaImpl.java
│   ├── registro/                               # SRP & DIP: Validação e registro com sensação calculada
│   │   ├── RegistroMedicaoService.java
│   │   └── RegistroMedicaoServiceImpl.java
│   ├── estatistica/                            # SRP & DIP: Agregação estatística de medições
│   │   ├── EstatisticaService.java
│   │   └── EstatisticaServiceImpl.java
│   ├── openweather/                            # SRP: Integração com a API OpenWeatherMap
│   │   ├── OpenWeatherClient.java
│   │   └── OpenWeatherClientImpl.java
│   ├── coleta/                                 # SRP & DIP: Coletor agendado em segundo plano
│   │   ├── ColetorAgendadoService.java
│   │   └── ColetorAgendadoServiceImpl.java
│   └── MonitoramentoService.java               # DIP & Fachada para os handlers REST
├── handler
│   ├── MedicaoApiHandler.java                  # Roteador / Dispatcher de requisições
│   ├── health/
│   │   └── HealthHandler.java                  # GET /health — métricas internas do sistema
│   ├── http
│   │   ├── request/                            # DIP: Abstração de requisição HTTP desacoplada
│   │   │   ├── HttpRequest.java
│   │   │   └── HttpRequestImpl.java
│   │   └── response/                           # DIP: Resposta HTTP com CORS, GZIP e UTF-8
│   │       ├── HttpResponseSender.java
│   │       └── HttpResponseSenderImpl.java
│   ├── operacoes
│   │   ├── get/                                # SRP: Handler exclusivo para GET
│   │   │   ├── MedicaoGetHandler.java
│   │   │   └── MedicaoGetHandlerImpl.java
│   │   ├── post/                               # SRP: Handler exclusivo para POST
│   │   │   ├── MedicaoPostHandler.java
│   │   │   └── MedicaoPostHandlerImpl.java
│   │   └── delete/                             # SRP: Handler exclusivo para DELETE
│   │       ├── MedicaoDeleteHandler.java
│   │       └── MedicaoDeleteHandlerImpl.java
│   └── proxy/
│       ├── FrontendStaticHandler.java          # Serve os arquivos estáticos do frontend
│       └── WeatherProxyHandler.java            # Proxy reverso para chamadas OpenWeatherMap
└── util
    ├── EnvLoader.java                          # Carrega o .env na inicialização
    ├── HibernateUtil.java                      # Inicialização e shutdown da SessionFactory
    ├── JsonUtil.java                           # Parser JSON leve e nativo (sem dependências)
    ├── cache/
    │   └── CacheManager.java                   # Cache em memória com TTL (ConcurrentHashMap)
    ├── database/
    │   └── DatabaseConnectionManager.java      # Gerenciador de sessões e transações Hibernate
    ├── i18n/
    │   └── I18n.java                           # Internacionalização pt-BR via ResourceBundle
    └── logging/
        └── AsyncLogger.java                    # Logging assíncrono via LinkedBlockingQueue
```

---

## ⚙️ Os 4 Pilares do Sistema

1. **Coleta (`ColetorAgendadoService` + `OpenWeatherClient`)**
   - Rotina em segundo plano a cada 10 segundos coletando dados reais da OpenWeatherMap.
   - Suporte a qualquer cidade do mundo; cidades adicionáveis dinamicamente via API.
   - Ciclo de coleta disparável sob demanda via `POST /api/v1/coletor/execucoes`.

2. **Cálculo (`CalculadoraSensacaoTermica`)**
   - Sensação Térmica calculada automaticamente com fórmulas internacionais:
     - **Wind Chill** para clima frio (≤ 10 °C).
     - **Heat Index** (Steadman) para calor com alta umidade (≥ 26 °C e umidade ≥ 40 %).
     - Temperatura real preservada em climas amenos.

3. **Estatística (`EstatisticaService`)**
   - Uma única query HQL de projeção (COUNT, MIN, MAX, AVG) — sem N+1.
   - Calcula média, mínima e máxima de temperatura, umidade e sensação térmica; pico de vento; contagem de chuvas.
   - Resultado em cache com TTL de 30 s.

4. **Registro (`RegistroMedicaoService` + `SalvarMedicaoDao`)**
   - Validação dos dados, preenchimento de metadados e persistência transacional no MySQL via Hibernate 6.
   - Batch de 25 inserts via JDBC batching.

---

## 🌤️ Variáveis Ambientais Monitoradas

| Variável | Tipo / Unidade | Campo JSON / Banco | Descrição |
| :--- | :--- | :--- | :--- |
| **Cidade** | `String` | `cidade` | Nome da cidade monitorada |
| **Estação ID** | `String` | `estacaoId` | Identificador da estação ou provedor |
| **Data e Hora** | `LocalDateTime` | `dataHora` | Registro cronológico da medição |
| **Temperatura Ambiente** | `Double` (°C) | `temperatura` | Medição térmica ambiente |
| **Sensação Térmica** | `Double` (°C) | `sensacaoTermica` | Calculada automaticamente pelo backend |
| **Umidade Relativa** | `Double` (%) | `umidade` | Umidade relativa do ar |
| **Precipitação** | `Boolean` | `precipitacao` | Detector de chuva |
| **Velocidade do Vento** | `Double` (km/h) | `velocidadeVento` | Convertida de m/s para km/h |
| **Direção do Vento** | `String` | `direcaoVento` | Rosa dos ventos (N, NE, L, SE, S, SO, O, NO) |
| **Pressão Atmosférica** | `Double` (hPa) | `pressaoAtmosferica` | Pressão barométrica |
| **Índice UV** | `Double` (0–12+) | `indiceUv` | Radiação solar |

---

## 📮 Endpoints da API REST

> Base URL: `http://localhost:8080` — todos os recursos da API usam o prefixo `/api/v1/`.

---

### 1. Inserir Medição — `POST /api/v1/medicoes`

O backend consulta a OpenWeatherMap, calcula a sensação térmica e persiste no MySQL.

**Opção A — apenas estado (resolve para a capital):**
```json
{ "estado": "PR" }
```
**Opção B — cidade + estado:**
```json
{ "cidade": "Curitiba", "estado": "PR" }
```
**Opção C — cidade + país:**
```json
{ "cidade": "Tokyo", "pais": "JP" }
```

*Também aceita query string:* `POST /api/v1/medicoes?cidade=Miami`

> Campos opcionais para sobrescrever valores da OWM: `temperatura`, `umidade`, `precipitacao`, `velocidadeVento`, `pressaoAtmosferica`, `direcaoVento`, `indiceUv`, `dataHora`.

---

### 2. Sincronizar Lote de Cidades — `POST /api/v1/sincronizacoes`

Busca e salva o clima de várias cidades de uma vez.

```json
{ "cidades": ["Tokyo", "Paris", "New York", "Curitiba"] }
```

---

### 3. Adicionar Cidade ao Coletor — `POST /api/v1/coletor/cidades`

Inclui uma cidade na rotina de coleta automática (ciclos a cada 10 s).

```json
{ "cidade": "Madrid" }
```

---

### 4. Forçar Ciclo de Coleta — `POST /api/v1/coletor/execucoes`

Dispara imediatamente um ciclo de coleta para todas as cidades cadastradas no coletor.

---

### 5. Clima em Tempo Real — `GET /api/v1/medicoes/atual?cidade=Tokyo`

Busca o clima atual na OWM, salva no banco e retorna os dados.

---

### 6. Listar Histórico Paginado — `GET /api/v1/medicoes`

```
GET /api/v1/medicoes?page=1&pageSize=20
```

**Filtros opcionais:** `cidade`, `tempMin`, `tempMax`, `precipitacao`, `sort` (datahora|temperatura|umidade|cidade), `order` (ASC|DESC)

**Exemplo:**
```
GET /api/v1/medicoes?cidade=Curitiba&tempMin=10&sort=temperatura&order=DESC&page=1&pageSize=10
```

**Resposta:**
```json
{
  "data": [{ "id": 15, "cidade": "Curitiba", "temperatura": 19.4, "dataHora": "2026-10-16T15:20:00" }],
  "pagination": { "page": 1, "pageSize": 10, "totalItems": 95, "totalPages": 10, "hasNext": true, "hasPrevious": false }
}
```

---

### 7. Estatísticas Climáticas — `GET /api/v1/medicoes/estatisticas`

Calcula sobre as N medições mais recentes (padrão 50, máx 500). Cache TTL 30 s.

```
GET /api/v1/medicoes/estatisticas?limite=50
GET /api/v1/medicoes/estatisticas?cidade=Curitiba&limite=20
GET /api/v1/cidades/Curitiba/estatisticas?limite=20
```

---

### 8. Listar Cidades — `GET /api/v1/cidades`

Retorna todas as cidades com medições no banco. Cache TTL 2 min.

---

### 9. Buscar Medição por ID — `GET /api/v1/medicoes/{id}`

---

### 10. Medições por Cidade — `GET /api/v1/cidades/{cidade}/medicoes`

---

### 11. Deletar por ID — `DELETE /api/v1/medicoes/{id}`

---

### 12. Deletar por Cidade — `DELETE /api/v1/medicoes?cidade=Miami`

---

### 13. Health Check — `GET /health`

Retorna métricas internas: status do banco, cache (hits/misses/TTL), transações e memória JVM.

---

## ⚙️ Configuração — Variáveis de Ambiente

As credenciais ficam **exclusivamente** no arquivo `.env` (ignorado pelo git — nunca vai ao GitHub).

| Variável | Descrição | Obrigatório |
| :--- | :--- | :---: |
| `DB_URL` | URL JDBC de conexão ao MySQL | ✓ |
| `DB_USER` | Usuário do banco | ✓ |
| `DB_PASSWORD` | Senha do banco | ✓ |
| `OPENWEATHER_API_KEY` | Chave da API OpenWeatherMap | ✓ |
| `PORT` | Porta do servidor (padrão: `8080`) | — |

> A aplicação lê o `.env` automaticamente via `EnvLoader` no início — não é necessário exportar variáveis manualmente.

---

## 🚀 Como Executar

### Pré-requisitos
- **Java 21+** — [download](https://adoptium.net/)
- **Maven 3.8+** — [download](https://maven.apache.org/download.cgi)
- **MySQL 8+** — [download](https://dev.mysql.com/downloads/mysql/)
- **Conta gratuita OpenWeatherMap** — [criar conta](https://home.openweathermap.org/users/sign_up)

### Passo a passo

**1. Clone o repositório**
```bash
git clone https://github.com/BatistaSec/APS-2026-2-backend.git
cd APS-2026-2-backend
```

**2. Configure o `.env`**
```bash
cp .env.example .env
```
Abra o `.env` e preencha com suas credenciais:
```env
DB_URL=jdbc:mysql://localhost:3306/estacao_ambiental_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8
DB_USER=root
DB_PASSWORD=sua_senha_do_mysql
OPENWEATHER_API_KEY=sua_chave_da_openweathermap
```

> O banco `estacao_ambiental_db` e a tabela `medicoes_ambientais` são criados automaticamente pelo Hibernate na primeira execução.

**3. Inicie o servidor**
```bash
mvn compile exec:java
```

**4. Abra no browser**

| URL | O que é |
|-----|---------|
| `http://localhost:8080` | Frontend principal |
| `http://localhost:8080/frontend/` | Frontend (rota alternativa) |
| `http://localhost:8080/health` | Health check do sistema |
| `http://localhost:8080/api` | Documentação JSON dos endpoints |

---

## 🌐 Internacionalização (i18n / pt-BR)

A aplicação está internacionalizada para **Português Brasileiro (pt-BR)** em todas as mensagens, validações e erros.

- Datas no formato `dd/MM/yyyy`, horários em 24h (`HH:mm:ss`)
- Erros no padrão **RFC 7807** (`type`, `title`, `status`, `detail`, `instance`)
- Contratos JSON internos preservam ponto decimal e ISO 8601 para compatibilidade REST
- Arquivos de recursos: `messages_pt_BR.properties` e `messages.properties` (fallback)

Para forçar o idioma na execução:
```bash
mvn compile exec:java -Duser.language=pt -Duser.country=BR
```

---

## 🔧 Otimizações Implementadas

| # | Otimização | Detalhe |
|---|---|---|
| 1 | **DatabaseConnectionManager** | Gerencia sessões e transações ACID com rollback automático e métricas expostas no `/health` |
| 2 | **CacheManager (TTL)** | `ConcurrentHashMap` com expiração: cidades (2 min), estatísticas (30 s). Invalidação proativa em toda escrita |
| 3 | **Sem N+1** | Estatísticas calculadas com uma única query HQL de projeção (COUNT, MIN, MAX, AVG) |
| 4 | **JDBC Batching** | Batch de 25 inserts configurado no Hibernate para operações em lote |
| 5 | **GZIP** | Compressão automática de respostas acima de 256 bytes quando o cliente envia `Accept-Encoding: gzip` |
| 6 | **AsyncLogger** | Fila `LinkedBlockingQueue` (4096 eventos) consumida por thread daemon — logs não bloqueiam as threads HTTP |
| 7 | **Virtual Threads** | Executor `newVirtualThreadPerTaskExecutor()` (Java 21) para alta concorrência sem overhead de pool |
| 8 | **EnvLoader** | Carrega o `.env` na inicialização — credenciais nunca hardcoded no código versionado |

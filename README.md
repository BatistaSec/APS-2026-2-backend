# Estação de Monitoramento Ambiental — APS

API REST em **Java 21 puro** (sem Spring), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap** para qualquer cidade do mundo. Inclui frontend web completo com mapa interativo.

---

## 🚀 Como Fazer Funcionar (do zero)

### O que você precisa instalar

| Ferramenta | Versão mínima | Download |
|---|---|---|
| Java (JDK) | 21 | [adoptium.net](https://adoptium.net/) |
| Maven | 3.8 | [maven.apache.org](https://maven.apache.org/download.cgi) |
| MySQL | 8.0 | [dev.mysql.com](https://dev.mysql.com/downloads/mysql/) |

Além disso, você precisa de uma **chave de API gratuita** da OpenWeatherMap:
1. Crie uma conta em [openweathermap.org](https://home.openweathermap.org/users/sign_up)
2. Acesse [openweathermap.org/api_keys](https://home.openweathermap.org/api_keys)
3. Copie a chave gerada (pode demorar até 2h pra ativar após o cadastro)

---

### Passo a passo

#### 1. Clone o repositório

```bash
git clone https://github.com/BatistaSec/APS-2026-2-backend.git
cd APS-2026-2-backend
```

#### 2. Configure o arquivo `.env`

Copie o arquivo de exemplo:

```bash
# Linux / macOS
cp .env.example .env

# Windows (PowerShell)
Copy-Item .env.example .env
```

Abra o `.env` num editor de texto e preencha com seus dados:

```env
DB_URL=jdbc:mysql://localhost:3306/estacao_ambiental_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8
DB_USER=root
DB_PASSWORD=sua_senha_do_mysql
OPENWEATHER_API_KEY=sua_chave_da_openweathermap
```

> **`DB_URL`** — se o MySQL estiver na porta padrão (3306) com usuário `root`, só troque a senha. O banco `estacao_ambiental_db` é criado automaticamente se não existir.
>
> **`OPENWEATHER_API_KEY`** — cole a chave que você copiou no passo anterior.
>
> O arquivo `.env` está no `.gitignore` — nunca vai para o GitHub.

#### 3. Confirme que o MySQL está rodando

```bash
# Linux / macOS
sudo service mysql start

# Windows — abra o "Gerenciador de Serviços" (services.msc) e inicie o MySQL,
# ou use o MySQL Notifier na bandeja do sistema
```

Você pode testar a conexão com:
```bash
mysql -u root -p
```
Se conectar, pode sair com `exit`.

#### 4. Inicie o servidor

```bash
mvn compile exec:java
```

Na primeira execução o Maven vai baixar as dependências (leva alguns minutos). Nas próximas será bem mais rápido.

Quando aparecer esta mensagem, o servidor está pronto:

```
[4/4] Servidor HTTP RESTful v1 iniciado com sucesso!
  Base URL:   http://localhost:8080/
  Frontend:   http://localhost:8080/frontend/
  Health:     http://localhost:8080/health
```

#### 5. Abra no browser

| URL | O que abre |
|---|---|
| `http://localhost:8080` | Frontend principal com busca e mapa |
| `http://localhost:8080/frontend/` | Mesmo frontend (rota alternativa) |
| `http://localhost:8080/health` | Status do sistema (banco, cache, JVM) |
| `http://localhost:8080/api` | Documentação JSON de todos os endpoints |

---

### Problemas comuns

**`Falha ao conectar ao banco`**
- Confirme que o MySQL está rodando
- Verifique se `DB_USER` e `DB_PASSWORD` no `.env` estão corretos
- Se a porta não for 3306, ajuste `DB_URL` para `localhost:SUA_PORTA`

**`401 Unauthorized` nas chamadas à OpenWeatherMap**
- A chave demora até 2h pra ativar após o cadastro — aguarde e tente novamente

**`Cidade não encontrada`**
- Use o nome em inglês ou com acento correto (ex.: `São Paulo`, `Paris`, `Tokyo`)

**Porta 8080 já em uso**
- Adicione `PORT=OUTRA_PORTA` no `.env` (ex.: `PORT=9090`)

---

## 🏛️ Arquitetura e Estrutura de Pacotes

O projeto segue os princípios **SOLID** — cada classe tem uma única responsabilidade e depende de interfaces, não de implementações.

```text
src/main/java/com/app
├── Main.java                                    # Inicialização: carrega .env, sobe HTTP server e coletor
├── model
│   ├── MedicaoAmbiental.java                   # Entidade JPA (tabela medicoes_ambientais)
│   └── dto
│       ├── request
│       │   ├── MedicaoRequest.java             # Entrada: cidade, estado, país e campos opcionais
│       │   └── SincronizacaoRequest.java       # Entrada: lote de cidades/estados
│       └── response
│           ├── MedicaoResponse.java            # Saída: dados de uma medição
│           ├── EstatisticasClimaticas.java     # Saída: estatísticas agregadas
│           ├── PaginatedResponse.java          # Wrapper genérico de paginação
│           └── ProblemDetails.java             # Erro padronizado RFC 7807
├── dao
│   ├── salvar/    SalvarMedicaoDao(Impl)        # Persiste medições no MySQL
│   ├── consulta/  ConsultaMedicaoDao(Impl)      # Buscas, filtros e listagens
│   └── exclusao/  DeletarMedicaoDao(Impl)       # Remove por ID ou por cidade
├── service
│   ├── calculo/   CalculadoraSensacaoTermica    # Wind Chill e Heat Index
│   ├── registro/  RegistroMedicaoService        # Valida e salva com sensação calculada
│   ├── estatistica/ EstatisticaService          # Agrega MIN, MAX, AVG em uma query
│   ├── openweather/ OpenWeatherClient           # Consulta a API OWM com retry e cache
│   ├── coleta/    ColetorAgendadoService        # Coleta automática a cada 10 s
│   └── MonitoramentoService.java               # Fachada para os handlers
├── handler
│   ├── MedicaoApiHandler.java                  # Dispatcher: roteia GET / POST / DELETE
│   ├── health/    HealthHandler                 # GET /health — métricas internas
│   ├── http/request/  HttpRequest(Impl)         # Abstração de leitura de requisição
│   ├── http/response/ HttpResponseSender(Impl)  # Resposta com CORS, GZIP e UTF-8
│   ├── operacoes/get/    MedicaoGetHandler      # Toda lógica de GET
│   ├── operacoes/post/   MedicaoPostHandler     # Toda lógica de POST
│   ├── operacoes/delete/ MedicaoDeleteHandler   # Toda lógica de DELETE
│   └── proxy/
│       ├── FrontendStaticHandler               # Serve HTML/CSS/JS do frontend
│       └── WeatherProxyHandler                 # Proxy reverso para a API OWM
└── util
    ├── EnvLoader.java                          # Lê o .env e injeta como System.setProperty
    ├── HibernateUtil.java                      # Inicializa a SessionFactory
    ├── JsonUtil.java                           # Parser JSON sem dependências externas
    ├── cache/     CacheManager                 # Cache em memória com TTL
    ├── database/  DatabaseConnectionManager    # Gerencia transações e métricas
    ├── i18n/      I18n                         # Mensagens em pt-BR via ResourceBundle
    └── logging/   AsyncLogger                  # Log assíncrono (não bloqueia threads HTTP)
```

---

## ⚙️ Os 4 Pilares do Sistema

**1. Coleta automática**
O `ColetorAgendadoService` busca dados reais da OpenWeatherMap a cada 10 segundos para todas as cidades cadastradas. Você pode adicionar cidades dinamicamente ou disparar um ciclo imediato via API.

**2. Cálculo de sensação térmica**
Calculado automaticamente pelo `CalculadoraSensacaoTermica`:
- **Wind Chill** para temperatura ≤ 10 °C
- **Heat Index** (Steadman) para temperatura ≥ 26 °C com umidade ≥ 40 %
- Temperatura real preservada em climas amenos

**3. Estatísticas**
O `EstatisticaService` calcula mínima, máxima e média de temperatura, umidade e sensação térmica, além do pico de vento e contagem de chuvas. Tudo em uma única query SQL sem N+1, com cache de 30 s.

**4. Registro e persistência**
Toda medição passa pelo `RegistroMedicaoService`, que valida os dados, calcula a sensação térmica e persiste no MySQL via Hibernate 6 com transações ACID.

---

## 🌤️ Dados Coletados por Medição

| Campo | Tipo | Descrição |
|---|---|---|
| `cidade` | String | Nome da cidade |
| `estacaoId` | String | Identificador da estação / provedor |
| `dataHora` | LocalDateTime | Data e hora da medição |
| `temperatura` | Double (°C) | Temperatura ambiente |
| `sensacaoTermica` | Double (°C) | Calculada pelo backend |
| `umidade` | Double (%) | Umidade relativa do ar |
| `precipitacao` | Boolean | Chuva detectada (true/false) |
| `velocidadeVento` | Double (km/h) | Velocidade do vento |
| `direcaoVento` | String | Rosa dos ventos (N, NE, L, SE, S, SO, O, NO) |
| `pressaoAtmosferica` | Double (hPa) | Pressão barométrica |
| `indiceUv` | Double (0–12+) | Índice de radiação UV |

---

## 📮 Endpoints da API

> Base: `http://localhost:8080` — todos os recursos da API usam prefixo `/api/v1/`

### POST — Inserir Medição
`POST /api/v1/medicoes`

O backend busca os dados na OWM, calcula sensação térmica e salva no banco.

```json
{ "cidade": "Curitiba", "estado": "PR" }
```
```json
{ "estado": "SP" }
```
```json
{ "cidade": "Tokyo", "pais": "JP" }
```

Campos opcionais para sobrescrever valores da OWM: `temperatura`, `umidade`, `precipitacao`, `velocidadeVento`, `pressaoAtmosferica`, `direcaoVento`, `indiceUv`, `dataHora`.

---

### POST — Sincronizar Lote
`POST /api/v1/sincronizacoes`

```json
{ "cidades": ["Tokyo", "Paris", "Curitiba", "New York"] }
```

---

### POST — Adicionar Cidade ao Coletor
`POST /api/v1/coletor/cidades`

```json
{ "cidade": "Madrid" }
```

---

### POST — Forçar Ciclo de Coleta
`POST /api/v1/coletor/execucoes`

Dispara imediatamente um ciclo de coleta para todas as cidades do coletor.

---

### GET — Clima em Tempo Real
`GET /api/v1/medicoes/atual?cidade=Tokyo`

Busca o clima agora, salva e retorna.

---

### GET — Histórico Paginado
`GET /api/v1/medicoes?page=1&pageSize=20`

Filtros opcionais: `cidade`, `tempMin`, `tempMax`, `precipitacao`, `sort` (datahora | temperatura | umidade | cidade), `order` (ASC | DESC)

```
GET /api/v1/medicoes?cidade=Curitiba&sort=temperatura&order=DESC&page=1&pageSize=10
```

Resposta:
```json
{
  "data": [{ "id": 15, "cidade": "Curitiba", "temperatura": 19.4, "dataHora": "2026-10-16T15:20:00" }],
  "pagination": { "page": 1, "pageSize": 10, "totalItems": 95, "totalPages": 10, "hasNext": true, "hasPrevious": false }
}
```

---

### GET — Estatísticas
```
GET /api/v1/medicoes/estatisticas?limite=50
GET /api/v1/medicoes/estatisticas?cidade=Curitiba&limite=20
GET /api/v1/cidades/Curitiba/estatisticas
```

---

### GET — Cidades cadastradas
`GET /api/v1/cidades`

---

### GET — Medição por ID
`GET /api/v1/medicoes/{id}`

---

### GET — Medições por cidade
`GET /api/v1/cidades/{cidade}/medicoes`

---

### DELETE — Deletar por ID
`DELETE /api/v1/medicoes/{id}`

---

### DELETE — Deletar por cidade
`DELETE /api/v1/medicoes?cidade=Miami`

---

### GET — Health Check
`GET /health`

Retorna status do banco, métricas de cache (hits/misses), contagem de transações e memória JVM.

---

## 🔧 Otimizações

| Otimização | Detalhe |
|---|---|
| **Cache TTL** | Cidades em cache por 2 min, estatísticas por 30 s. Invalidação automática a cada escrita |
| **Sem N+1** | Estatísticas em uma única query HQL de projeção (COUNT, MIN, MAX, AVG) |
| **JDBC Batching** | Lotes de 25 inserts configurados no Hibernate |
| **GZIP** | Respostas comprimidas automaticamente quando o cliente suporta `Accept-Encoding: gzip` |
| **Virtual Threads** | `newVirtualThreadPerTaskExecutor()` do Java 21 — alta concorrência sem overhead de pool |
| **AsyncLogger** | Fila de 4096 eventos consumida por thread daemon — logs não bloqueiam requisições |
| **EnvLoader** | Carrega o `.env` antes de qualquer inicialização — zero credencial no código versionado |

---

## 🌐 Internacionalização (i18n / pt-BR)

Todas as mensagens de erro e validação estão em Português Brasileiro via `ResourceBundle`.
Erros seguem o padrão **RFC 7807** com campos `type`, `title`, `status`, `detail` e `instance`.
Contratos JSON internos preservam ponto decimal e ISO 8601 para compatibilidade REST universal.

```bash
# Forçar idioma explicitamente
mvn compile exec:java -Duser.language=pt -Duser.country=BR
```

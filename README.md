# Estação de Monitoramento Ambiental — APS

API REST em **Java 21 puro** (sem Spring), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap** para qualquer cidade do mundo. Inclui frontend web com mapa interativo.

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
3. Copie a chave gerada (pode demorar até 2h para ativar após o cadastro)

---

### Passo a passo

#### 1. Clone o repositório
```bash
git clone https://github.com/BatistaSec/APS-2026-2-backend.git
cd APS-2026-2-backend
```

#### 2. Configure as credenciais

Abra `src/main/resources/hibernate.cfg.xml` e ajuste usuário e senha do MySQL:

```xml
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">sua_senha_do_mysql</property>
```

A chave da OpenWeatherMap fica em `src/main/java/com/app/service/openweather/OpenWeatherClientImpl.java`, no método `obterApiKeyConfigurada()`.

#### 3. Confirme que o MySQL está rodando

```bash
# Linux / macOS
sudo service mysql start

# Windows — abra o Gerenciador de Serviços (services.msc) e inicie o MySQL,
# ou use o MySQL Notifier na bandeja do sistema
```

#### 4. Inicie o servidor

```bash
mvn compile exec:java
```

Na primeira execução, o Hibernate recria as tabelas do zero (`hbm2ddl.auto=create`). Nas execuções seguintes, troque para `update` no `hibernate.cfg.xml` para preservar os dados.

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
| `http://localhost:8080/health` | Status do sistema (banco, JVM) |
| `http://localhost:8080/api` | Documentação JSON de todos os endpoints |

---

### Problemas comuns

**`Access denied for user 'root'@'localhost'`**
- A senha em `hibernate.cfg.xml` está incorreta. Teste: `mysql -u root -p`

**`Cidade não encontrada`**
- Use o nome em inglês (ex.: `Sao Paulo`, `Tokyo`, `Paris`)

**`401` nas chamadas à OpenWeatherMap**
- A chave demora até 2h para ativar após o cadastro

**Porta 8080 já em uso**
- Defina a variável `PORT` antes de rodar: `$env:PORT=9090; mvn compile exec:java`

---

## 🗄️ Modelo de Dados (3 tabelas)

O sistema usa um modelo relacional normalizado. Cada medição está vinculada a uma estação, que por sua vez pertence a uma cidade.

```
cidades
  id (PK) | nome | estado | pais
  (unique: nome + estado + pais)

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

| Origem | Gatilho |
|---|---|
| `COLETOR` | Coletor automático a cada 10 s |
| `PESQUISA` | Frontend buscou via `GET /atual?cidade=X` |
| `POST` | `POST /medicoes` com localização |
| `SINCRONIZACAO` | `POST /sincronizacoes` (lote de cidades) |
| `MANUAL` | `POST /medicoes` sem cidade (dados manuais) |

### Exemplo de JOIN

Para consultar medições com dados completos de cidade:

```sql
SELECT m.*, c.nome AS cidade, c.estado
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
├── Main.java                                    # Inicialização do servidor HTTP e coletor
├── model
│   ├── Cidade.java                             # Entidade JPA — tabela cidades
│   ├── Estacao.java                            # Entidade JPA — tabela estacoes (FK → cidades)
│   ├── MedicaoAmbiental.java                   # Entidade JPA — tabela medicoes_ambientais (FK → estacoes)
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
│   ├── EstacaoRepository.java                  # Busca ou cria Cidade + Estacao (upsert)
│   ├── salvar/    SalvarMedicaoDao(Impl)        # Persiste medições no MySQL
│   ├── consulta/  ConsultaMedicaoDao(Impl)      # Buscas e listagens com JOIN
│   └── exclusao/  DeletarMedicaoDao(Impl)       # Remove por ID ou por cidade
├── service
│   ├── calculo/   CalculadoraSensacaoTermica    # Wind Chill e Heat Index
│   ├── registro/  RegistroMedicaoService(Impl)  # Valida, resolve Estacao/Cidade e salva
│   ├── estatistica/ EstatisticaService(Impl)    # Agrega MIN, MAX, AVG em uma query
│   ├── openweather/ OpenWeatherClient(Impl)     # Consulta a API OWM com retry
│   ├── coleta/    ColetorAgendadoService(Impl)  # Coleta automática a cada 10 s
│   └── MonitoramentoService.java               # Fachada que conecta todos os serviços
├── handler
│   ├── MedicaoApiHandler.java                  # Dispatcher: roteia GET / POST / DELETE
│   ├── health/    HealthHandler                 # GET /health — métricas do sistema
│   ├── http/request/  HttpRequest(Impl)         # Abstração de leitura de requisição
│   ├── http/response/ HttpResponseSender(Impl)  # Resposta com CORS, GZIP e UTF-8
│   ├── operacoes/get/    MedicaoGetHandler(Impl)    # Toda lógica de GET
│   ├── operacoes/post/   MedicaoPostHandler(Impl)   # Toda lógica de POST
│   ├── operacoes/delete/ MedicaoDeleteHandler(Impl) # Toda lógica de DELETE
│   └── proxy/
│       ├── FrontendStaticHandler               # Serve HTML/CSS/JS do frontend
│       └── WeatherProxyHandler                 # Proxy reverso para a API OWM
└── util
    ├── HibernateUtil.java                      # Inicializa a SessionFactory
    ├── JsonUtil.java                           # Parser JSON sem dependências externas
    ├── database/  DatabaseConnectionManager    # Gerencia transações e métricas
    └── logging/   AsyncLogger                  # Log assíncrono (não bloqueia threads HTTP)
```

---

## ⚙️ Os 4 Pilares do Sistema

**1. Coleta automática**
O `ColetorAgendadoService` busca dados reais da OpenWeatherMap a cada 10 segundos para cidades do estado de São Paulo (São Paulo, Campinas, Santos, São José dos Campos, Ribeirão Preto, Sorocaba, Osasco, Santo André, São Bernardo do Campo, Guarulhos). Rotaciona as cidades em sequência e salva com `origem = COLETOR`.

**2. Cálculo de sensação térmica**
Calculado automaticamente pelo `CalculadoraSensacaoTermica`:
- **Wind Chill** para temperatura ≤ 10 °C
- **Heat Index** (Steadman) para temperatura ≥ 26 °C com umidade ≥ 40 %
- Temperatura real preservada em climas amenos

**3. Estatísticas**
O `EstatisticaService` calcula mínima, máxima e média de temperatura, umidade e sensação térmica, além do pico de vento e contagem de chuvas. Usa uma única query SQL com `COUNT`, `MIN`, `MAX`, `AVG` — sem N+1.

**4. Registro e persistência**
Toda medição passa pelo `RegistroMedicaoService`, que valida os dados, resolve a `Cidade` e `Estacao` (criando se necessário via `EstacaoRepository`), calcula a sensação térmica e persiste no MySQL via Hibernate 6 com transações ACID.

---

## 🌤️ Dados Coletados por Medição

| Campo | Tipo | Descrição |
|---|---|---|
| `cidade` | String (via JOIN) | Nome da cidade |
| `estacaoId` | String (FK) | ID da estação |
| `dataHora` | LocalDateTime | Data e hora da medição |
| `temperatura` | Double (°C) | Temperatura ambiente |
| `sensacaoTermica` | Double (°C) | Calculada pelo backend |
| `umidade` | Double (%) | Umidade relativa do ar |
| `precipitacao` | Boolean | Chuva detectada |
| `velocidadeVento` | Double (km/h) | Velocidade do vento |
| `direcaoVento` | String | Rosa dos ventos (N, NE, L, SE, S, SO, O, NO) |
| `pressaoAtmosferica` | Double (hPa) | Pressão barométrica |
| `indiceUv` | Double (0–12+) | Índice UV |
| `origem` | Enum | Gatilho que gerou o registro |

---

## 📮 Endpoints da API

> Base: `http://localhost:8080` — todos os recursos usam prefixo `/api/v1/`

### POST — Inserir Medição
`POST /api/v1/medicoes`

Busca dados na OWM, calcula sensação térmica, resolve cidade/estação e salva com `origem = POST`.

```json
{ "cidade": "Sao Paulo", "estado": "SP" }
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

Salva com `origem = SINCRONIZACAO`.

```json
{ "cidades": ["Campinas", "Santos", "Sorocaba"] }
```

---

### POST — Adicionar Cidade ao Coletor
`POST /api/v1/coletor/cidades`

```json
{ "cidade": "Guarulhos" }
```

---

### POST — Forçar Ciclo de Coleta
`POST /api/v1/coletor/execucoes`

---

### GET — Clima em Tempo Real
`GET /api/v1/medicoes/atual?cidade=Campinas`

Busca agora na OWM, salva com `origem = PESQUISA` e retorna.

---

### GET — Histórico Paginado
`GET /api/v1/medicoes?page=1&pageSize=20`

Filtros opcionais: `cidade`, `tempMin`, `tempMax`, `precipitacao`, `sort` (datahora | temperatura | umidade | cidade), `order` (ASC | DESC)

```
GET /api/v1/medicoes?cidade=Santos&sort=temperatura&order=DESC&page=1&pageSize=10
```

Resposta:
```json
{
  "data": [{ "id": 15, "cidade": "Santos", "temperatura": 27.4, "origem": "COLETOR", "dataHora": "2026-11-03T09:23:00" }],
  "pagination": { "page": 1, "pageSize": 10, "totalItems": 95, "totalPages": 10, "hasNext": true, "hasPrevious": false }
}
```

---

### GET — Estatísticas
```
GET /api/v1/medicoes/estatisticas?limite=50
GET /api/v1/medicoes/estatisticas?cidade=Campinas&limite=20
GET /api/v1/cidades/Campinas/estatisticas
```

---

### GET — Cidades cadastradas
`GET /api/v1/cidades`

Lista apenas cidades que têm medições no banco (via JOIN).

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
`DELETE /api/v1/medicoes?cidade=Santos`

---

### GET — Health Check
`GET /health`

Retorna status do banco, contagem de transações e memória JVM.

---

## ⚙️ Configuração

As credenciais ficam em `src/main/resources/hibernate.cfg.xml`:

```xml
<property name="hibernate.connection.url">jdbc:mysql://localhost:3306/estacao_ambiental_db?...</property>
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">sua_senha</property>
```

| Propriedade `hbm2ddl.auto` | Quando usar |
|---|---|
| `create` | Primeira execução — recria todas as tabelas do zero |
| `update` | Execuções seguintes — preserva os dados existentes |

---

## 🔧 Otimizações

| Otimização | Detalhe |
|---|---|
| **Modelo relacional normalizado** | 3 tabelas com FK — cidades e estações sem duplicação |
| **Campo `origem`** | Rastreia qual dos 5 gatilhos gerou cada medição |
| **JOIN FETCH** | Todas as queries carregam cidade via JOIN — sem lazy-loading fora de sessão |
| **Sem N+1** | Estatísticas em uma única query HQL de projeção (COUNT, MIN, MAX, AVG) |
| **JDBC Batching** | Lotes de 25 inserts configurados no Hibernate |
| **GZIP** | Respostas comprimidas automaticamente quando o cliente suporta `Accept-Encoding: gzip` |
| **Virtual Threads** | `newVirtualThreadPerTaskExecutor()` do Java 21 |
| **AsyncLogger** | Fila de 4096 eventos consumida por thread daemon |

---

## 🌐 Internacionalização (i18n / pt-BR)

Todas as mensagens de erro e validação estão em Português Brasileiro diretamente nas classes.
Erros seguem o padrão **RFC 7807** com campos `type`, `title`, `status`, `detail` e `instance`.

```bash
mvn compile exec:java -Duser.language=pt -Duser.country=BR
```

---

## 🏛️ Princípios SOLID

- **S** — Cada classe tem uma única responsabilidade (`SalvarMedicaoDao`, `ConsultaMedicaoDao`, `DeletarMedicaoDao`, `MedicaoGetHandler`, etc.)
- **O** — Extensível por interfaces sem modificar contratos existentes
- **L** — Implementações substituíveis pelas interfaces sem quebrar o sistema
- **I** — Interfaces pequenas e específicas por operação
- **D** — Módulos de alto nível dependem de abstrações, não de implementações concretas

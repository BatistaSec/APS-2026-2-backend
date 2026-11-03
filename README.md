# Estação de Monitoramento Ambiental — APS

API REST desenvolvida em **Java 21 puro** (sem frameworks pesados), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap**. Inclui frontend web completo com mapa interativo e coleta automática de dados climáticos.

---

## 🎓 Para a Apresentação

### O que o sistema faz

O sistema monitora automaticamente as condições climáticas de cidades do estado de São Paulo. A cada **10 segundos**, um coletor agendado busca dados reais da API OpenWeatherMap, calcula a sensação térmica e persiste no banco de dados MySQL. O usuário também pode pesquisar qualquer cidade do mundo pelo frontend — cada pesquisa é automaticamente salva no histórico.

### Tecnologias utilizadas

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (Virtual Threads) |
| ORM | Hibernate 6 (sem Spring) |
| Banco de dados | MySQL 8 |
| Servidor HTTP | `com.sun.net.httpserver` (nativo do JDK) |
| API externa | OpenWeatherMap (dados reais em tempo real) |
| Frontend | HTML, CSS, JavaScript puro + Leaflet.js (mapa) |

### Princípios SOLID aplicados

| Princípio | Como foi aplicado no projeto |
|---|---|
| **S** — Single Responsibility | Cada classe tem uma única responsabilidade: `SalvarMedicaoDao` só salva, `ConsultaMedicaoDao` só consulta, `DeletarMedicaoDao` só exclui, `MedicaoGetHandler` só trata GET, etc. |
| **O** — Open/Closed | Novas operações são adicionadas implementando interfaces existentes, sem modificar código já funcional |
| **L** — Liskov Substitution | Qualquer implementação pode ser substituída pela sua interface sem quebrar o sistema (`OpenWeatherClient`, `RegistroMedicaoService`, etc.) |
| **I** — Interface Segregation | Interfaces pequenas e específicas por operação em vez de uma interface genérica para tudo |
| **D** — Dependency Inversion | Os handlers e serviços dependem de abstrações (interfaces), não de implementações concretas — facilitando testes e substituições |

### Modelo de dados normalizado (3 tabelas)

Decisão de design: em vez de uma tabela única com campo `cidade` como texto livre (que causaria duplicatas e inconsistências), o sistema usa um modelo relacional normalizado:

```
cidades
  id (PK) | nome | pais
  ↓ 1:N
estacoes
  id (PK, texto) | cidade_id (FK) | tipo (OWM / MANUAL)
  ↓ 1:N
medicoes_ambientais
  id (PK) | estacao_id (FK) | data_hora | temperatura | umidade
  precipitacao | velocidade_vento | pressao_atmosferica
  direcao_vento | indice_uv | sensacao_termica
  origem (COLETOR | PESQUISA | POST | SINCRONIZACAO | MANUAL)
```

O campo **`origem`** registra qual dos 5 gatilhos gerou cada medição, permitindo rastrear a procedência de cada dado.

### Cálculo de sensação térmica

O sistema implementa as duas fórmulas meteorológicas internacionais:
- **Wind Chill** — resfriamento pelo vento para temperaturas ≤ 10 °C
- **Heat Index** (Steadman) — percepção de calor para temperaturas ≥ 26 °C com umidade ≥ 40 %
- Temperatura real preservada em climas amenos

### Cidades monitoradas automaticamente

São Paulo, Campinas, Santos, São José dos Campos, Ribeirão Preto, Sorocaba, Osasco, Santo André, São Bernardo do Campo, Guarulhos — todas do estado de São Paulo, coletadas em rotação a cada 10 segundos.

### Mapa interativo (Leaflet.js + OpenStreetMap)

O frontend inclui um mapa interativo completo que funciona **sem API key adicional**. Veja como ele funciona por dentro:

**Como abrir:** clique no botão **"🗺 Ver no mapa"** abaixo da barra de busca. O mapa é iniciado com preguiça (*lazy init*) — só carrega quando o usuário abre pela primeira vez, economizando recursos.

**Tiles via proxy do backend:** os azulejos do mapa (imagens OpenStreetMap) não são carregados diretamente pelo browser — passam pelo proxy Java (`/proxy/tiles/{z}/{x}/{y}.png`). Isso evita bloqueios de CORS e protege a origem das requisições.

**Clique no mapa → clima:** ao clicar em qualquer ponto do mundo, o frontend:
1. Coloca um marcador temporário com "⏳ Buscando local…"
2. Chama o endpoint `/proxy/reverse?lat=X&lon=Y` no backend Java, que repassa para a API Nominatim (OpenStreetMap) — o frontend nunca acessa o Nominatim diretamente
3. Recebe o nome da cidade, estado e país via reverse geocode
4. Exibe um popup com o nome e um botão **"Ver clima aqui"**
5. Ao clicar no botão, chama `loadWeather(place)` — o mesmo fluxo da busca por texto — que busca o clima na OWM, salva no banco e renderiza os dados

**Sincronização bidirecional:** quando o usuário pesquisa uma cidade pelo campo de texto, o marcador no mapa se move automaticamente para a cidade encontrada (via `window._onCityChosen`). Mapa e busca por texto estão sempre sincronizados.

**Salvamento automático:** toda seleção de cidade (pelo mapa ou pelo texto) dispara `salvarHistorico()`, que faz `POST /api/v1/medicoes` com os dados climáticos — salvando a medição no banco com `origem = POST`.

### Pontos de destaque técnico

- **Virtual Threads** (Java 21) — alta concorrência sem overhead de pool de threads
- **Upsert thread-safe** — `EstacaoRepository` trata race conditions entre threads paralelas com retry em caso de `ConstraintViolationException`
- **GZIP automático** — respostas comprimidas para clientes compatíveis
- **Logging assíncrono** — fila de 4096 eventos consumida por thread daemon, sem bloquear as threads HTTP
- **Erros padronizados** — respostas de erro seguem RFC 7807 (`type`, `title`, `status`, `detail`, `instance`)
- **JDBC Batching** — lotes de 25 inserts para operações em massa
- **JOIN FETCH** — todas as queries carregam cidade e estação numa única query, sem lazy-loading fora de sessão

---

## 🚀 Como Executar

### Pré-requisitos

| Ferramenta | Versão mínima | Download |
|---|---|---|
| Java (JDK) | 21 | [adoptium.net](https://adoptium.net/) |
| Maven | 3.8 | [maven.apache.org](https://maven.apache.org/download.cgi) |
| MySQL | 8.0 | [dev.mysql.com](https://dev.mysql.com/downloads/mysql/) |

Chave de API gratuita da OpenWeatherMap:
1. Crie conta em [openweathermap.org](https://home.openweathermap.org/users/sign_up)
2. Copie a chave em [openweathermap.org/api_keys](https://home.openweathermap.org/api_keys) (ativa em até 2h)

### Configuração

Abra `src/main/resources/hibernate.cfg.xml` e ajuste usuário e senha do MySQL:
```xml
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">sua_senha</property>
```

A chave da OpenWeatherMap fica em `OpenWeatherClientImpl.java`, método `obterApiKeyConfigurada()`.

### Execução

```bash
git clone https://github.com/BatistaSec/APS-2026-2-backend.git
cd APS-2026-2-backend
mvn compile exec:java
```

Na **primeira execução**, use `hbm2ddl.auto=create` no `hibernate.cfg.xml` para criar as tabelas. Depois troque para `update` para preservar os dados.

### URLs disponíveis

| URL | O que abre |
|---|---|
| `http://localhost:8080` | Frontend principal |
| `http://localhost:8080/health` | Status do sistema |
| `http://localhost:8080/api` | Documentação JSON |

---

## 🗄️ Banco de Dados

### Tabelas

```
cidades            — cidades monitoradas (nome + país)
estacoes           — fontes de dados vinculadas a cidades
medicoes_ambientais — registros climáticos com FK para estacoes
```

### JOIN completo

```sql
SELECT m.*, e.id AS estacao, c.nome AS cidade, c.pais
FROM medicoes_ambientais m
JOIN estacoes e ON m.estacao_id = e.id
JOIN cidades  c ON e.cidade_id  = c.id
WHERE c.nome = 'Sao Paulo'
ORDER BY m.data_hora DESC;
```

### Variáveis climáticas armazenadas

| Campo | Tipo | Fonte |
|---|---|---|
| `temperatura` | Double (°C) | OpenWeatherMap |
| `sensacao_termica` | Double (°C) | Calculada pelo backend |
| `umidade` | Double (%) | OpenWeatherMap |
| `precipitacao` | Boolean | OpenWeatherMap |
| `velocidade_vento` | Double (km/h) | OWM (convertido de m/s) |
| `direcao_vento` | String (rosa dos ventos) | OWM (convertido de graus) |
| `pressao_atmosferica` | Double (hPa) | OpenWeatherMap |
| `indice_uv` | Double (0–12+) | OWM ou estimado |
| `origem` | Enum | Sistema |

---

## 📮 Endpoints da API

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/medicoes` | Histórico paginado com filtros |
| `GET` | `/api/v1/medicoes/{id}` | Medição por ID |
| `GET` | `/api/v1/medicoes/atual?cidade=X` | Clima agora (salva e retorna) |
| `GET` | `/api/v1/medicoes/estatisticas` | Estatísticas agregadas |
| `GET` | `/api/v1/cidades` | Lista cidades com medições |
| `GET` | `/api/v1/cidades/{cidade}/medicoes` | Medições de uma cidade |
| `GET` | `/api/v1/cidades/{cidade}/estatisticas` | Estatísticas de uma cidade |
| `GET` | `/api/v1/estacoes/{id}/medicoes` | Medições de uma estação |
| `POST` | `/api/v1/medicoes` | Insere medição |
| `POST` | `/api/v1/sincronizacoes` | Insere lote de cidades |
| `POST` | `/api/v1/coletor/cidades` | Adiciona cidade ao coletor |
| `POST` | `/api/v1/coletor/execucoes` | Força ciclo imediato |
| `DELETE` | `/api/v1/medicoes/{id}` | Remove por ID |
| `DELETE` | `/api/v1/medicoes?cidade=X` | Remove por cidade |
| `GET` | `/health` | Status do sistema |

### Filtros do histórico

```
GET /api/v1/medicoes?page=1&pageSize=20&cidade=Santos&sort=temperatura&order=DESC
```

---

## 🏛️ Estrutura de Pacotes

```text
src/main/java/com/app
├── Main.java
├── model
│   ├── Cidade.java                   # Entidade JPA — tabela cidades
│   ├── Estacao.java                  # Entidade JPA — tabela estacoes
│   ├── MedicaoAmbiental.java         # Entidade JPA — tabela medicoes_ambientais
│   └── dto/request e response
├── dao
│   ├── EstacaoRepository.java        # Upsert thread-safe Cidade + Estacao
│   ├── salvar/    SalvarMedicaoDao
│   ├── consulta/  ConsultaMedicaoDao  # JOINs nas 3 tabelas
│   └── exclusao/  DeletarMedicaoDao
├── service
│   ├── calculo/     CalculadoraSensacaoTermica
│   ├── registro/    RegistroMedicaoService
│   ├── estatistica/ EstatisticaService
│   ├── openweather/ OpenWeatherClient
│   ├── coleta/      ColetorAgendadoService
│   └── MonitoramentoService.java     # Fachada (DIP)
├── handler
│   ├── MedicaoApiHandler.java        # Dispatcher
│   ├── health/    HealthHandler
│   ├── http/      HttpRequest + HttpResponseSender
│   ├── operacoes/ MedicaoGetHandler, PostHandler, DeleteHandler
│   └── proxy/     FrontendStaticHandler, WeatherProxyHandler
└── util
    ├── HibernateUtil.java
    ├── JsonUtil.java
    ├── database/  DatabaseConnectionManager
    └── logging/   AsyncLogger
```

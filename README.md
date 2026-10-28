# Backend: Estação de Monitoramento Ambiental (APS)

API REST desenvolvida em **Java 21 Puro** (sem frameworks pesados), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap** para **qualquer cidade do mundo**.

O projeto adota rigorosamente os princípios **SOLID**:
- **S**ingle Responsibility Principle (SRP): Cada classe e interface tem uma única e bem definida razão para mudar.
- **O**pen/Closed Principle (OCP): Extensível por interfaces sem modificar os contratos existentes.
- **L**iskov Substitution Principle (LSP): As implementações podem ser substituídas por suas interfaces sem quebrar contratos.
- **I**nterface Segregation Principle (ISP): Interfaces pequenas e específicas (`SalvarMedicaoDao`, `ConsultaMedicaoDao`, `DeletarMedicaoDao`, `MedicaoGetHandler`, etc.).
- **D**ependency Inversion Principle (DIP): Módulos de alto nível dependem de abstrações (interfaces), não de implementações concretas.

---

## 🆕 Últimas Mudanças

### 🗺 Mapa Interativo no Frontend
O frontend agora conta com um mapa interativo completo baseado em **Leaflet.js** + **OpenStreetMap**, sem necessidade de API key adicional.

- Botão **"🗺 Ver no mapa"** abaixo da barra de busca abre/fecha o mapa
- Clique em **qualquer ponto do mundo** — o sistema identifica a cidade via reverse geocode (Nominatim) e exibe um popup com o nome e um botão "Ver clima aqui"
- Selecionar uma cidade pelo mapa usa **o mesmo fluxo** da busca por texto
- O marcador no mapa acompanha a cidade selecionada, seja via mapa ou via texto
- Qualquer pesquisa (mapa ou texto) **salva automaticamente no histórico** do painel admin

### 🛠 Painel Administrativo Simplificado (`/admin`)
O painel admin foi reestruturado para ser mais limpo e focado. Agora acessível diretamente em **`http://localhost:8080/admin`**.

**3 abas funcionais:**

| Aba | Função |
|-----|--------|
| 📋 **Histórico de Pesquisas** | Lista todas as cidades pesquisadas no frontend com data/hora e dados climáticos capturados. Suporta filtros, paginação e exclusão de registros. |
| ⚙ **Coletor** | Sincronização em lote de cidades e execução imediata de ciclo de coleta. |
| ❤ **Health** | Métricas em tempo real: status do banco, pool HikariCP, cache, transações, memória e JVM. |

**Abas removidas por redundância:** Estatísticas (integrada ao Histórico) e Cidades.

### 📡 Histórico de Pesquisas Automático
Quando um usuário pesquisa uma cidade no frontend (via texto ou mapa), os dados climáticos retornados pela OpenWeatherMap são **automaticamente salvos no banco MySQL** e aparecem no painel admin em **📋 Histórico**.

---

## 🏛️ Estrutura de Pacotes SOLID (SRP / ISP / DIP)

```text
src/main/java/com/app
├── Main.java                                    # Inicialização do servidor HTTP e coletor
├── model
│   ├── MedicaoAmbiental.java                   # Entidade JPA mapeada para a tabela no MySQL
│   └── dto
│       ├── request                             # DTOs de entrada flexíveis (cidade, estado, lote)
│       │   ├── MedicaoRequest.java
│       │   └── SincronizacaoRequest.java
│       └── response                            # DTOs de saída formatados
│           ├── EstatisticasClimaticas.java
│           └── MedicaoResponse.java
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
│   ├── openweather/                            # SRP: Integração com a API OpenWeatherMap (qualquer cidade)
│   │   ├── OpenWeatherClient.java
│   │   └── OpenWeatherClientImpl.java
│   ├── coleta/                                 # SRP & DIP: Coletor em segundo plano com rotação de cidades
│   │   ├── ColetorAgendadoService.java
│   │   └── ColetorAgendadoServiceImpl.java
│   └── MonitoramentoService.java               # DIP & Fachada para os handlers REST
├── handler
│   ├── http
│   │   ├── request/                            # DIP: Abstração de requisição HTTP desacoplada
│   │   │   ├── HttpRequest.java
│   │   │   └── HttpRequestImpl.java
│   │   └── response/                           # DIP: Abstração de envio de resposta com CORS e UTF-8
│   │       ├── HttpResponseSender.java
│   │       └── HttpResponseSenderImpl.java
│   ├── operacoes
│   │   ├── get/                                # SRP: Handler exclusivo para requisições GET
│   │   │   ├── MedicaoGetHandler.java
│   │   │   └── MedicaoGetHandlerImpl.java
│   │   ├── post/                               # SRP: Handler exclusivo para requisições POST
│   │   │   ├── MedicaoPostHandler.java
│   │   │   └── MedicaoPostHandlerImpl.java
│   │   └── delete/                             # SRP: Handler exclusivo para requisições DELETE
│   │       ├── MedicaoDeleteHandler.java
│   │       └── MedicaoDeleteHandlerImpl.java
│   └── MedicaoApiHandler.java                  # Roteador / Dispatcher de requisições
└── util
    ├── HibernateUtil.java                      # Gerenciador da SessionFactory do Hibernate
    └── JsonUtil.java                           # Parser JSON leve e nativo
```

---

## ⚙️ Os 4 Pilares do Sistema

1. **Coleta (`ColetorAgendadoService` e `OpenWeatherClient`)**:
   - Rotina em segundo plano a cada 10 segundos coletando dados meteorológicos reais da OpenWeatherMap.
   - Suporte dinâmico a qualquer cidade do mundo (sem ficar restrito a uma só cidade).
   - Possibilidade de adicionar cidades dinamicamente ou disparar coletas sob demanda.

2. **Cálculo (`CalculadoraSensacaoTermica`)**:
   - Cálculo automático de Sensação Térmica utilizando fórmulas internacionais:
     - **Wind Chill** (resfriamento pelo vento) para clima frio ($\le 10^\circ\text{C}$).
     - **Heat Index** (Steadman) para calor e alta umidade ($\ge 26^\circ\text{C}$ e $\text{umidade} \ge 40\%$).
     - Preservação da temperatura real em climas amenos.

3. **Estatística (`EstatisticaService`)**:
   - Agregação matemática com cálculo de:
     - Média, mínima e máxima de temperatura e sensação térmica.
     - Média, mínima e máxima de umidade relativa.
     - Pico de vento (velocidade máxima).
     - Contagem de ocorrências de precipitação (chuva).

4. **Registro (`RegistroMedicaoService` e `SalvarMedicaoDao`)**:
   - Validação dos dados, preenchimento de metadados (`dataHora`, `estacaoId`, etc.) e persistência transacional no MySQL via Hibernate 6.

---

## 🌤️ Variáveis Ambientais Monitoradas

| Variável | Tipo / Unidade | Campo JSON / Banco | Descrição |
| :--- | :--- | :--- | :--- |
| **Cidade** | `String` | `cidade` | Nome da cidade monitorada |
| **Estação ID** | `String` | `estacaoId` | Identificador da estação ou provedor |
| **Data e Hora** | `LocalDateTime` | `dataHora` | Registro cronológico da medição |
| **Temperatura Ambiente** | `Double` (°C) | `temperatura` | Medição térmica ambiente |
| **Sensação Térmica** | `Double` (°C) | `sensacaoTermica` | Calculada automaticamente pelo backend |
| **Umidade Relativa do Ar**| `Double` (%) | `umidade` | Umidade relativa do ar |
| **Precipitação** | `Boolean` (true/false) | `precipitacao` | Detector de chuva |
| **Velocidade do Vento** | `Double` (km/h) | `velocidadeVento` | Medição de vento convertida de m/s para km/h |
| **Direção do Vento** | `String` | `direcaoVento` | Posição na rosa dos ventos (N, NE, L, SE, S, SO, O, NO) |
| **Pressão Atmosférica** | `Double` (hPa) | `pressaoAtmosferica`| Pressão barométrica |
| **Índice UV** | `Double` (0 a 12+) | `indiceUv` | Radiação solar |

---

## 📮 Endpoints da API REST (Postman / cURL)

### 1. Inserir Medição Simplificada (POST com `MedicaoRequest`)
Não é necessário preencher 10 campos manuais. O DTO `MedicaoRequest` permite informar **apenas a cidade**, **apenas o estado** (ex: `"PR"`, `"SP"`), ou **ambos**:

- **Método**: `POST`
- **URL**: `http://localhost:8080/api/medicoes`
- **Opção A (Apenas o Estado)**:
```json
{
  "estado": "PR"
}
```
*(O backend resolve inteligentemente o estado para sua capital ou região, consulta a OpenWeatherMap, calcula sensação térmica e salva no MySQL).*

- **Opção B (Cidade e Estado)**:
```json
{
  "cidade": "Curitiba",
  "estado": "PR"
}
```

- **Opção C (Apenas a Cidade)**:
```json
{
  "cidade": "Tokyo",
  "pais": "JP"
}
```
*Ou via URL:* `POST http://localhost:8080/api/medicoes?estado=SP` ou `POST http://localhost:8080/api/medicoes?cidade=Miami`

O backend consulta a OpenWeatherMap em tempo real, calcula a sensação térmica, define data/hora e salva no MySQL, retornando o `MedicaoResponse` com status `201 Created`.

> *Nota: Você também pode fornecer campos manuais se desejar sobrescrever valores (ex: `{"estado":"PR", "temperatura": 18.5, "dataHora":"2026-10-14T15:30:00"}`).*

---

### 2. Sincronizar Lote de Cidades (POST)
Sincroniza o clima de várias cidades do mundo simultaneamente:

- **Método**: `POST`
- **URL**: `http://localhost:8080/api/medicoes/sincronizar`
- **Corpo (JSON)**:
```json
{
  "cidades": ["Tokyo", "Paris", "New York", "Curitiba"]
}
```

---

### 3. Adicionar Cidade ao Coletor Automático de 10s (POST)
- **Método**: `POST`
- **URL**: `http://localhost:8080/api/coletor/cidades`
- **Corpo (JSON)**:
```json
{
  "cidade": "Madrid"
}
```

---

### 4. Forçar Ciclo Imediato de Coleta (POST)
- **Método**: `POST`
- **URL**: `http://localhost:8080/api/coletor/executar`

---

### 5. Clima em Tempo Real / Ao Vivo (GET)
- **Método**: `GET`
- **URL**: `http://localhost:8080/api/medicoes/atual?cidade=Tokyo`

---

### 6. Listar Histórico de Medições (GET)
- **Método**: `GET`
- **URL (geral)**: `http://localhost:8080/api/medicoes?limite=30`
- **URL (filtrado por cidade)**: `http://localhost:8080/api/medicoes?cidade=Curitiba&limite=10`

---

### 7. Obter Estatísticas Climáticas (GET)
- **Método**: `GET`
- **URL (geral)**: `http://localhost:8080/api/v1/medicoes/estatisticas?limite=50`
- **URL (por cidade)**: `http://localhost:8080/api/v1/medicoes/estatisticas?cidade=Curitiba&limite=20`
- **URL (cross-resource)**: `http://localhost:8080/api/v1/cidades/Curitiba/estatisticas?limite=20`
- **Semântica do Parâmetro `limite`**: As estatísticas (mínimas, máximas, médias, pico de vento e contagem de chuva) são calculadas diretamente sobre as **N medições mais recentes**, com ordenação determinística por `data_hora DESC, id DESC`. Caso o limite não seja informado ou seja menor/igual a zero, adota-se o padrão seguro de **50** medições (com teto máximo de 500). Resultados usam cache em memória com TTL de 30 segundos.

---

### 8. Listar Cidades com Dados no Banco (GET)
- **Método**: `GET`
- **URL**: `http://localhost:8080/api/medicoes/cidades`

---

### 9. Buscar Medição por ID (GET)
- **Método**: `GET`
- **URL**: `http://localhost:8080/api/medicoes/{id}`

---

### 10. Deletar Medições (DELETE)
- **Por ID**: `DELETE http://localhost:8080/api/medicoes/{id}`
- **Por Cidade**: `DELETE http://localhost:8080/api/medicoes?cidade=Miami`

---

## ⚙️ Configuração de Ambiente e Variáveis de Conexão

Por motivos de segurança, nenhuma credencial sensível é mantida hardcoded no código ou no `hibernate.cfg.xml`.
A aplicação requer a configuração das seguintes variáveis de ambiente (ou propriedades JVM via `-D`):

| Variável de Ambiente | Propriedade JVM (`-D`) | Descrição | Exemplo |
| :--- | :--- | :--- | :--- |
| `DB_URL` | `db.url` | URL de conexão JDBC ao banco MySQL | `jdbc:mysql://localhost:3306/estacao_ambiental_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8` |
| `DB_USER` | `db.user` | Usuário do banco de dados | `root` |
| `DB_PASSWORD` | `db.password` | Senha do banco de dados | `sua_senha_segura` |
| `OPENWEATHER_API_KEY` | `openweather.api.key` | Chave de API da OpenWeatherMap | `sua_chave_openweather` |
| `PORT` | `port` *(opcional)* | Porta do servidor HTTP (padrão: 8080) | `8080` |

### ⚠️ Aviso de Segurança e Rotação de Credenciais
> [!IMPORTANT]
> Caso credenciais ou chaves reais tenham sido commitadas no histórico Git anterior do projeto, **recomenda-se a imediata rotação** das mesmas (redefinir a senha do usuário MySQL e gerar uma nova API Key no painel da OpenWeatherMap). O arquivo `hibernate.cfg.xml` agora contém apenas definições de pooling e dialeto, isolando completamente segredos em variáveis de ambiente.

*(O banco `estacao_ambiental_db` e a tabela `medicoes_ambientais` são criados ou atualizados automaticamente pelo Hibernate com base na entidade mapeada).*

---

## 🚀 Como Executar

Execute pelo terminal:
```powershell
mvn compile exec:java
```
Ou execute os testes com:
```powershell
mvn clean test
```

Após iniciar, os endpoints disponíveis são:

| URL | Descrição |
|-----|-----------|
| `http://localhost:8080/` | Documentação da API em JSON |
| `http://localhost:8080/frontend/` | Frontend público (clima + mapa interativo) |
| `http://localhost:8080/admin` | Painel administrativo |
| `http://localhost:8080/health` | Health check (requer token Bearer) |

---

## 🌐 Internacionalização e Localização Regional (i18n / pt-BR)

A aplicação foi integralmente internacionalizada e localizada para **Português Brasileiro (pt-BR)** como idioma padrão de todas as interfaces voltadas ao usuário, mensagens do sistema, validações e documentação.

### 1. Padrões Regionais Adotados
* **Idioma Padrão:** `pt-BR` (Português do Brasil).
* **Cabeçalho de Resposta HTTP:** Todas as respostas da API incluem o cabeçalho `Content-Language: pt-BR`.
* **Apresentação de Datas:** Formato `dd/MM/yyyy` (ex.: `19/10/2026`).
* **Apresentação de Horários:** Padrão 24 horas `HH:mm:ss` (ex.: `15:30:00`).
* **Formatação Numérica:** Separador decimal com vírgula e milhar com ponto nos textos e relatórios (ex.: `21,5 °C`, `65,0%`, `1.013,3 hPa`, `14,2 km/h`).
* **Formatação Monetária:** Padrão Real Brasileiro `BRL` / `R$` (ex.: `R$ 150,00`).
* **Mensagens e Erros RFC 7807:** Títulos e descrições semânticas em português brasileiro culto e amigável (`"Requisição Inválida"`, `"Recurso Não Encontrado"`, `"Limite de Requisições Excedido"`, etc.).

### 2. Preservação de Padrões Técnicos Internos
* **Contratos JSON e APIs:** O formato interno de transporte JSON preserva números com ponto decimal (`.` no lugar de `,`) e datas em ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss`), garantindo estrita aderência às especificações REST e compatibilidade universal com frontends e bibliotecas clientes.
* **Persistência MySQL:** Estrutura de colunas e tipos técnicos permanecem inalterados.

### 3. Como Manter ou Alternar o Idioma
O sistema utiliza o padrão Enterprise Java `ResourceBundle` gerenciado pela classe `I18n.java` e arquivos de recursos:
* `src/main/resources/messages_pt_BR.properties` (Dicionário brasileiro).
* `src/main/resources/messages.properties` (Bundle padrão fallback).

Para manter ou forçar o idioma via linha de comando ou container Docker:
```powershell
mvn compile exec:java -Duser.language=pt -Duser.country=BR
```


---

## 🛠️ Diretrizes e Propostas de Melhorias Arquiteturais

Para elevar a qualidade, maturidade RESTful (Richardson Maturity Model Level 2/3), confiabilidade e segurança da API, foram mapeadas as seguintes diretrizes:

### 1. 🏷️ Nomenclatura Limpa e RESTful (Clean Naming)
* **Substantivos no Plural para Recursos:** Padronizar todos os endpoints em coleções no plural:
  * ✅ `/api/v1/medicoes` em vez de `/api/medicoes`
  * ✅ `/api/v1/cidades` e `/api/v1/estacoes`
* **Eliminar Verbos das URLs:** Em REST, o método HTTP define a ação:
  * ❌ `POST /api/medicoes/sincronizar` ➔ ✅ `POST /api/v1/sincronizacoes`
  * ❌ `POST /api/coletor/executar` ➔ ✅ `POST /api/v1/coletor/execucoes`
* **Padrões de Nomenclatura:** `kebab-case` para URIs e `camelCase` uniforme para campos de requisição e resposta JSON.

---

### 2. 🛡️ Confiabilidade e Resiliência (Ensure Reliability)
* **Tratamento de Erros Padronizado (RFC 7807 - Problem Details):** Unificar todos os erros e validações em um formato semântico único:
  ```json
  {
    "type": "https://api.aps-clima.com/erros/validacao",
    "title": "Erro de Validação",
    "status": 400,
    "detail": "A temperatura informada (-150°C) está fora dos limites físicos aceitáveis (-90°C a 60°C).",
    "instance": "/api/v1/medicoes",
    "timestamp": "2026-10-16T15:30:00Z"
  }
  ```
* **Resiliência na Integração Externa (OpenWeatherMap):**
  * **Timeout & Retry:** Configurar timeout explícito (ex: 3s) e até 3 tentativas com backoff exponencial.
  * **Fallback / Cache:** Se a API externa oscilar ou cair, responder com a última medição válida persistida em cache ou no banco para a cidade solicitada.
* **Health Check Endpoint:**
  * `GET /health` -> Verifica status da JVM, conectividade com MySQL e latência com a OpenWeatherMap.

---

### 3. 📌 Versionamento de API (Add Version)
* **Prefixagem por URI (`/api/v1/...`):**
  * Todas as rotas passam a responder sob `/api/v1/`.
  * Garante que futuras evoluções (ex: migração para v2 com novas métricas ou mudança estrutural de DTOs) não quebrem clientes existentes ou integrações frontend.

---

### 4. 📄 Paginação Estruturada (Add Pagination)
Substituir o parâmetro rudimentar `?limite=30` por paginação formal baseada em página e tamanho de lote:
* **Parâmetros:** `page` (padrão: 1) e `pageSize` (padrão: 20, máx: 100).
* **Exemplo:** `GET /api/v1/medicoes?page=2&pageSize=10`
* **Contrato de Resposta:**
  ```json
  {
    "data": [
      {
        "id": 15,
        "cidade": "Curitiba",
        "temperatura": 19.4,
        "sensacaoTermica": 19.4,
        "dataHora": "2026-10-16T15:20:00"
      }
    ],
    "pagination": {
      "page": 2,
      "pageSize": 10,
      "totalItems": 95,
      "totalPages": 10,
      "hasNext": true,
      "hasPrevious": true
    }
  }
  ```

---

### 5. 🔍 Filtros Dinâmicos via Query String (Use Query String)
Flexibilizar consultas sem criar múltiplos endpoints redundantes:
* **Filtros por Faixas e Critérios:**
  * `GET /api/v1/medicoes?cidade=Curitiba&tempMin=15&tempMax=25&precipitacao=true`
* **Filtro por Período Cronológico:**
  * `GET /api/v1/medicoes?dataInicio=2026-10-01T00:00:00&dataFim=2026-10-16T23:59:59`
* **Ordenação Configurável:**
  * `GET /api/v1/medicoes?sort=temperatura&order=desc`
  * `GET /api/v1/medicoes?sort=dataHora&order=asc`

---

### 6. 🔒 Segurança e Gestão de Segredos (Security)
* **Gestão Segura de Chaves (Secrets Management):**
  * Remover a chave da OpenWeatherMap e credenciais do MySQL do código-fonte e do README.
  * Carregar via variáveis de ambiente (`OPENWEATHER_API_KEY`, `DB_USER`, `DB_PASSWORD`).
* **Autenticação & Autorização:**
  * Endpoints públicos: Apenas leitura de dados consolidados (`GET /api/v1/medicoes`, `GET /api/v1/estatisticas`).
  * Endpoints protegidos: Inserção manual, sincronização em lote e deleção restritos via token **JWT** com role `ADMIN` ou `ESTACAO`.
* **CORS e Cabeçalhos:**
  * Restringir origens permitidas no `Access-Control-Allow-Origin` (evitar `*` indiscriminado em produção).
  * Adicionar cabeçalhos de segurança contra MIME-sniffing e clickjacking.

---

### 7. 🔗 Referências Cruzadas entre Recursos (Cross-Resource References)
Mapear relações de sub-recursos pai-filho de forma semântica:
* **Medições de uma Cidade Específica:**
  * `GET /api/v1/cidades/{cidade}/medicoes`
* **Estatísticas Climáticas de uma Cidade:**
  * `GET /api/v1/cidades/{cidade}/estatisticas`
* **Medições por Estação Meteorológica:**
  * `GET /api/v1/estacoes/{estacaoId}/medicoes`
* **Acesso Canônico Direto:**
  * `GET /api/v1/medicoes/{id}` (recurso independente pelo seu identificador primário).

---

### 8. ⏱️ Limitação de Taxa (Rate Limiting)
* **Proteção da Cota Externa e do Servidor:**
  * A conta da OpenWeatherMap possui limite de requisições por minuto.
  * O servidor HTTP nativo deve limitar o tráfego por IP para evitar esgotamento da cota e ataques de negação de serviço (DoS).
* **Política Sugerida:**
  * Máximo de 60 requisições por minuto por IP.
* **Cabeçalhos de Controle:**
  * `X-RateLimit-Limit: 60`
  * `X-RateLimit-Remaining: 54`
  * `X-RateLimit-Reset: 1718000060`
* **Status ao Exceder:** `HTTP 429 Too Many Requests` acompanhado do cabeçalho `Retry-After: 30`.


---

## ⚡ Otimizações de Alta Performance, Conexões, Cache e I/O

Para garantir máxima vazão de requisições, baixa latência e consumo eficiente de recursos no backend, foram implementadas as seguintes otimizações:

### 1. 🏊 Connection Pooling de Alta Performance (HikariCP)
* **Substituição do Pool Padrão do Hibernate:** Integração do **HikariCP** (`com.zaxxer:HikariCP`), considerado o connection pool JDBC mais rápido do ecossistema Java.
* **Configuração Otimizada no `hibernate.cfg.xml`:**
  * `minimumIdle`: 5 conexões mantidas sempre aquecidas.
  * `maximumPoolSize`: 20 conexões simultâneas máximas.
  * `idleTimeout`: 30.000 ms (elimina conexões ociosas).
  * `connectionTimeout`: 20.000 ms.
  * `maxLifetime`: 1.800.000 ms (reciclagem saudável de conexões).

---

### 2. 🎛️ Connection Manager Estruturado (`DatabaseConnectionManager`)
* **Gerenciamento Centralizado de Sessões:** Camada que orquestra o ciclo de vida de sessões e transações ACID.
* **Execução Transacional com Rollback Automático:**
  * Métodos `executeTransactional(...)` e `executeTransactionalResult(...)` garantem `commit` automático ou `rollback` imediato em caso de exceção.
* **Métricas de Integridade:** Contabilização de transações bem-sucedidas e falhas expostas diretamente no endpoint de `/health`.

---

### 3. 🧠 Cache em Memória com TTL (`CacheManager`)
* **Estrutura Thread-Safe:** Implementação baseada em `ConcurrentHashMap` com tempo de expiração temporal (**Time-To-Live - TTL**).
* **Entidades Cacheadas:**
  * `cidades:lista` ➔ Lista de cidades distintas cadastradas (TTL: 2 minutos).
  * `stats:{cidade}:{limite}` ➔ Estatísticas climáticas consolidadas (TTL: 30 segundos).
* **Invalidação Proativa de Cache:**
  * Toda operação de escrita (`SalvarMedicaoDaoImpl` ou `DeletarMedicaoDaoImpl`) dispara `invalidatePrefix("cidades")` e `invalidatePrefix("stats")`, garantindo que os clientes nunca leiam dados obsoletos.
* **Métricas:** Contadores atômicos de `hits`, `misses` e cálculo da taxa de acerto (`hitRate`).

---

### 4. 🚫 Eliminação de M Queries / N+1 (Evitar Múltiplas Queries)
* **Agregação Direta no Motor do Banco (SQL/HQL):**
  * O cálculo de estatísticas (`calcularEstatisticasAgregadas`) deixou de carregar centenas de entidades JPA para a memória da JVM.
  * Uma **única query HQL de projeção** executa `COUNT`, `MIN`, `MAX`, `AVG` e `SUM(CASE ...)` diretamente no MySQL em milissegundos.
* **JDBC Statement Batching:**
  * Configurado `hibernate.jdbc.batch_size=25`, `hibernate.order_inserts=true` e `hibernate.order_updates=true`.
  * Em sincronizações de lote (`salvarLote`), a sessão executa `flush()` e `clear()` a cada 25 registros, reduzindo drasticamente as viagens de rede (round-trips) ao banco.

---

### 5. ⚡ Serializador JSON Otimizado (`JsonSerializer`)
* **Zero Overhead de Regex:** Serialização com buffer `StringBuilder` pré-alocado e escape direto de caracteres especiais (`"`, `\`, quebras de linha e caracteres de controle).
* **Menor Pressão no Garbage Collector:** Redução expressiva na alocação efêmera de strings durante a serialização de grandes listas e mapas.

---

### 6. 🗜️ Compressão Dinâmica de Payload (GZIP)
* **Detecção Automática:** No [`HttpResponseSenderImpl`](file:///C:/Users/Canaljozo/OneDrive/Documentos/APS/src/main/java/com/app/handler/http/response/HttpResponseSenderImpl.java), verifica se o cliente enviou o cabeçalho `Accept-Encoding: gzip`.
* **Ativação Inteligente:** Para respostas com tamanho superior a 256 bytes, o payload é comprimido em tempo de execução via `GZIPOutputStream`.
* **Cabeçalhos Injetados:** `Content-Encoding: gzip` e `Vary: Accept-Encoding`.
* **Benefício:** Redução de até 75% na largura de banda consumida em listagens extensas de histórico climático.

---

### 7. 🧵 Logging Assíncrono Não-Bloqueante (`AsyncLogger`)
* **Desacoplamento de I/O de Disco:** Threads HTTP do servidor não esperam a escrita no console ou no disco.
* **Fila de Mensagens & Thread Daemon:**
  * Fila limitada (`LinkedBlockingQueue` de até 4096 eventos).
  * Thread consumidora dedicada em segundo plano (`AsyncLogger-Worker`).
  * Descarte seguro (*drop-safe*) em caso de saturação extrema de I/O para evitar travamento da aplicação.
* **Encerramento Gracioso:** Gancho de desligamento (*shutdown hook*) que esvazia todos os logs pendentes antes de fechar a JVM.

---

### 8. 📊 Monitoramento em Tempo Real (`GET /health`)
O endpoint de integridade agora expõe em tempo real o estado de todas essas otimizações:
```json
{
  "status": "UP",
  "versao": "1.0.0",
  "componentes": {
    "bancoDeDados": "UP",
    "connectionPooling": "HikariCP (Ativo)",
    "coletorSegundoPlano": "UP",
    "openWeather": "CONFIGURADO"
  },
  "metricasCache": {
    "itensEmCache": 2,
    "hits": 14,
    "misses": 2,
    "hitRatePorcentagem": 87.50
  },
  "metricasBanco": {
    "transacoesSucesso": 38,
    "transacoesFalha": 0
  },
  "sistema": {
    "jvm": "Java 21",
    "memoriaUsadaMb": 48,
    "memoriaTotalMb": 256
  },
  "timestamp": "2026-10-16T15:55:00"
}
```
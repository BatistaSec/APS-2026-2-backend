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
- Qualquer pesquisa (mapa ou texto) **salva automaticamente no histórico** de medições

### 📡 Histórico de Pesquisas Automático
Quando um usuário pesquisa uma cidade no frontend (via texto ou mapa), os dados climáticos retornados pela OpenWeatherMap são **automaticamente salvos no banco MySQL** e ficam disponíveis no histórico de medições.

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

> Todos os endpoints usam o prefixo `/api/v1/`.

---

### 1. Inserir Medição (POST)

O DTO aceita apenas a cidade, apenas o estado (sigla BR) ou ambos. O backend consulta a OpenWeatherMap em tempo real, calcula a sensação térmica e salva no MySQL.

- **Método**: `POST`
- **URL**: `http://localhost:8080/api/v1/medicoes`


**Opção A — Apenas o estado (resolve para a capital):**
```json
{ "estado": "PR" }
```

**Opção B — Cidade + estado:**
```json
{ "cidade": "Curitiba", "estado": "PR" }
```

**Opção C — Cidade + país:**
```json
{ "cidade": "Tokyo", "pais": "JP" }
```

*Também aceita via query string:* `POST /api/v1/medicoes?estado=SP` ou `?cidade=Miami`

> Campos opcionais para sobrescrever valores da OWM: `temperatura`, `umidade`, `precipitacao`, `velocidadeVento`, `pressaoAtmosferica`, `direcaoVento`, `indiceUv`, `dataHora`.

---

### 2. Sincronizar Lote de Cidades (POST)

Busca e salva o clima de várias cidades simultaneamente.

- **Método**: `POST`
- **URL**: `http://localhost:8080/api/v1/sincronizacoes`

- **Corpo**:
```json
{ "cidades": ["Tokyo", "Paris", "New York", "Curitiba"] }
```

---

### 3. Adicionar Cidade ao Coletor Automático (POST)

Inclui uma cidade na rotina de coleta que roda a cada 10 segundos.

- **Método**: `POST`
- **URL**: `http://localhost:8080/api/v1/coletor/cidades`

- **Corpo**:
```json
{ "cidade": "Madrid" }
```

---

### 4. Forçar Ciclo Imediato de Coleta (POST)

Dispara imediatamente um ciclo de coleta para todas as cidades do coletor.

- **Método**: `POST`
- **URL**: `http://localhost:8080/api/v1/coletor/execucoes`


---

### 5. Clima em Tempo Real (GET)

Busca o clima atual na OWM e salva no banco antes de retornar.

- **Método**: `GET`
- **URL**: `http://localhost:8080/api/v1/medicoes/atual?cidade=Tokyo`

---

### 6. Listar Histórico Paginado (GET)

- **Método**: `GET`
- **URL**: `http://localhost:8080/api/v1/medicoes?page=1&pageSize=20`
- **Filtros opcionais**: `cidade`, `tempMin`, `tempMax`, `precipitacao`, `sort` (datahora|temperatura|umidade|cidade), `order` (ASC|DESC)

**Exemplo com filtros:**
```
GET /api/v1/medicoes?cidade=Curitiba&tempMin=10&tempMax=25&sort=temperatura&order=desc&page=1&pageSize=10
```

**Resposta:**
```json
{
  "data": [ { "id": 15, "cidade": "Curitiba", "temperatura": 19.4, "dataHora": "2026-10-16T15:20:00" } ],
  "pagination": { "page": 1, "pageSize": 10, "totalItems": 95, "totalPages": 10, "hasNext": true, "hasPrevious": false }
}
```

---

### 7. Obter Estatísticas Climáticas (GET)

Calcula sobre as N medições mais recentes (padrão 50, máx 500). Resultado em cache com TTL de 30s.

- **Geral**: `GET /api/v1/medicoes/estatisticas?limite=50`
- **Por cidade**: `GET /api/v1/medicoes/estatisticas?cidade=Curitiba&limite=20`
- **Cross-resource**: `GET /api/v1/cidades/Curitiba/estatisticas?limite=20`

---

### 8. Listar Cidades com Dados no Banco (GET)

- **Método**: `GET`
- **URL**: `http://localhost:8080/api/v1/cidades`

---

### 9. Buscar Medição por ID (GET)

- **Método**: `GET`
- **URL**: `http://localhost:8080/api/v1/medicoes/{id}`

---

### 10. Listar Medições de uma Cidade (GET)

- **Método**: `GET`
- **URL**: `http://localhost:8080/api/v1/cidades/{cidade}/medicoes`

---

### 11. Deletar Medição por ID (DELETE)

- **Método**: `DELETE`
- **URL**: `http://localhost:8080/api/v1/medicoes/{id}`


---

### 12. Deletar Medições por Cidade (DELETE)

- **Método**: `DELETE`
- **URL**: `http://localhost:8080/api/v1/medicoes?cidade=Miami`


---

### 13. Health Check (GET)

- **Método**: `GET`
- **URL**: `http://localhost:8080/health`


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

**2. Configure as variáveis de ambiente**
```bash
# Copie o arquivo de exemplo
cp .env.example .env
```
Abra o `.env` e preencha:
```env
DB_USER=root
DB_PASSWORD=sua_senha_do_mysql
OPENWEATHER_API_KEY=sua_chave_da_openweathermap
```
> A aplicação carrega o `.env` automaticamente no início — não é necessário exportar as variáveis manualmente.
> O banco `estacao_ambiental_db` é criado automaticamente pelo Hibernate.

**3. Inicie o servidor**
```bash
mvn compile exec:java
```

**4. Abra no browser**

| URL | O que é |
|-----|---------|
| http://localhost:8080 | Frontend principal |
| http://localhost:8080/frontend/ | Frontend (rota alternativa) |
| http://localhost:8080/health | Health check do sistema |

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

## Melhorias Pendentes

### Confiabilidade e Resiliência
* **Tratamento de Erros Padronizado (RFC 7807):** Unificar todos os erros em um formato semântico único com 	ype, 	itle, status, detail, instance e 	imestamp.
* **Resiliência com a OpenWeatherMap:** Timeout explícito (3s) com até 3 tentativas e backoff exponencial. Fallback para a última medição válida em cache ou banco quando a OWM oscilar.

### Segurança
* **CORS:** Restringir Access-Control-Allow-Origin para origens conhecidas em produção (evitar * indiscriminado).
* **Cabeçalhos:** Adicionar proteção contra MIME-sniffing e clickjacking.

## Otimizações Implementadas

| # | Otimização | Detalhe |
|---|---|---|
| 1 | **HikariCP** | Pool de conexões de alta performance (min 5, max 20, reciclagem a cada 30 min) |
| 2 | **DatabaseConnectionManager** | Gerencia sessões e transações ACID com rollback automático e métricas expostas no /health |
| 3 | **CacheManager (TTL)** | ConcurrentHashMap com expiração: lista de cidades (2 min), estatísticas (30 s). Invalidação proativa em toda escrita |
| 4 | **Sem N+1** | Estatísticas calculadas com uma única query HQL de projeção (COUNT, MIN, MAX, AVG). Batch de 25 inserts via JDBC batching |
| 5 | **JsonSerializer** | Serialização com StringBuilder pré-alocado, sem regex, sem overhead de GC |
| 6 | **GZIP** | Compressão automática de respostas acima de 256 bytes quando o cliente envia Accept-Encoding: gzip |
| 7 | **AsyncLogger** | Fila LinkedBlockingQueue (4096 eventos) consumida por thread daemon — logs não bloqueiam as threads HTTP |


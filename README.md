# Backend: Estação de Monitoramento Ambiental (APS)

API REST desenvolvida em **Java 21 Puro** (sem frameworks pesados), **Hibernate 6**, **MySQL** e integração em tempo real com a **OpenWeatherMap** (Chave de API: `fd8737c649a4176f6c40c6411e959a59`) para **qualquer cidade do mundo**.

O projeto adota rigorosamente os princípios **SOLID**:
- **S**ingle Responsibility Principle (SRP): Cada classe e interface tem uma única e bem definida razão para mudar.
- **O**pen/Closed Principle (OCP): Extensível por interfaces sem modificar os contratos existentes.
- **L**iskov Substitution Principle (LSP): As implementações podem ser substituídas por suas interfaces sem quebrar contratos.
- **I**nterface Segregation Principle (ISP): Interfaces pequenas e específicas (`SalvarMedicaoDao`, `ConsultaMedicaoDao`, `DeletarMedicaoDao`, `MedicaoGetHandler`, etc.).
- **D**ependency Inversion Principle (DIP): Módulos de alto nível dependem de abstrações (interfaces), não de implementações concretas.

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
- **URL (por cidade)**: `http://localhost:8080/api/medicoes/estatisticas?cidade=Curitiba`
- **URL (geral)**: `http://localhost:8080/api/medicoes/estatisticas`

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

## ⚙️ Configuração do Banco de Dados (MySQL)

Edite as credenciais em `src/main/resources/hibernate.cfg.xml`:
```xml
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">SUA_SENHA_AQUI</property>
```
*(O banco `estacao_ambiental_db` e a tabela `medicoes_ambientais` são criados ou atualizados automaticamente pelo Hibernate).*

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
#   A P S - 2 0 2 6 - 2 - b a c k e n d  
 
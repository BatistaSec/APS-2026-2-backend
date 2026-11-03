# Trechos de Código — Estação de Monitoramento Ambiental (APS)

> Documento de apoio para a elaboração do trabalho acadêmico conforme
> **ABNT NBR 6023** (referências) e **NBR 10520** (citações).
> Cada trecho indica a fonte (caminho do arquivo) para referência bibliográfica.

---

## FOTOS DO SISTEMA EM EXECUÇÃO

> **Instruções para captura** — tire as screenshots com o sistema rodando
> (`mvn compile exec:java`) e cole neste documento ou no Word.
> Use `Win + Shift + S` para selecionar a área exata no Windows.

### Foto 1 — Frontend principal (tela de busca)
**O que capturar:** Abra `http://localhost:8080` no browser.
Mostre a tela com a barra de busca, resultado do clima de uma cidade (temperatura,
umidade, vento) e o ícone do mapa.
> 📸 *Inserir screenshot aqui*

---

### Foto 2 — Mapa interativo
**O que capturar:** Clique no botão "🗺 Ver no mapa".
Mostre o mapa com um marcador sobre uma cidade e o popup com nome e dados.
> 📸 *Inserir screenshot aqui*

---

### Foto 3 — Terminal com o coletor funcionando
**O que capturar:** O terminal onde o servidor está rodando,
mostrando as linhas `[COLETA 10s] [São Paulo] ID: #X | XX.X°C ...`
> 📸 *Inserir screenshot aqui*

---

### Foto 4 — Banco de dados: tabela `cidades`
**O que capturar:** No MySQL Workbench, execute:
```sql
SELECT * FROM cidades;
```
Mostre as cidades salvas automaticamente pelo coletor.
> 📸 *Inserir screenshot aqui*

---

### Foto 5 — Banco de dados: tabela `estacoes`
**O que capturar:** No MySQL Workbench, execute:
```sql
SELECT * FROM estacoes;
```
Mostre as estações com o `cidade_id` e o `tipo` (OWM / MANUAL).
> 📸 *Inserir screenshot aqui*

---

### Foto 6 — Banco de dados: tabela `medicoes_ambientais`
**O que capturar:** No MySQL Workbench, execute:
```sql
SELECT m.id, c.nome AS cidade, m.temperatura, m.umidade,
       m.precipitacao, m.origem, m.data_hora
FROM medicoes_ambientais m
JOIN estacoes e ON m.estacao_id = e.id
JOIN cidades c  ON e.cidade_id  = c.id
ORDER BY m.data_hora DESC
LIMIT 20;
```
Mostre a listagem com cidades, temperaturas e a coluna `origem`.
> 📸 *Inserir screenshot aqui*

---

### Foto 7 — Endpoint `/health` no browser
**O que capturar:** Abra `http://localhost:8080/health`.
Mostre o JSON com `status: UP`, métricas de banco e JVM.
> 📸 *Inserir screenshot aqui*

---

### Foto 8 — Endpoint `/api/v1/medicoes` no browser ou Postman
**O que capturar:** Abra `http://localhost:8080/api/v1/medicoes?page=1&pageSize=5`.
Mostre o JSON paginado com medições e o bloco `pagination`.
> 📸 *Inserir screenshot aqui*

---

## TRECHOS DE CÓDIGO

---

## 1. Modelo de Dados — Entidades JPA

### Código 1 — `Cidade.java` — Entidade com restrição de unicidade

Representa uma cidade monitorada. A anotação `@UniqueConstraint` garante
integridade referencial, impedindo duplicatas de nome e país no banco de dados.

```java
@Entity
@Table(name = "cidades", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cidade_nome_pais",
                      columnNames = { "nome", "pais" })
})
public class Cidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "pais", length = 10)
    private String pais;

    public Cidade(String nome, String pais) {
        this.nome = nome;
        this.pais = pais;
    }
}
```

> **Fonte:** `src/main/java/com/app/model/Cidade.java`

---

### Código 2 — `Estacao.java` — Relacionamento ManyToOne com Cidade

A entidade `Estacao` representa a fonte de coleta de dados. Utiliza `FetchType.EAGER`
para garantir que a cidade seja carregada imediatamente, evitando
`LazyInitializationException` fora do contexto de sessão Hibernate.

```java
@Entity
@Table(name = "estacoes")
public class Estacao {

    public enum Tipo { OWM, MANUAL }

    @Id
    @Column(name = "id", length = 60)
    private String id; // ex: "OPENWEATHER-SAO-PAULO"

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "cidade_id", nullable = true)
    private Cidade cidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private Tipo tipo;

    public Estacao(String id, Cidade cidade, Tipo tipo) {
        this.id     = id;
        this.cidade = cidade;
        this.tipo   = tipo;
    }
}
```

> **Fonte:** `src/main/java/com/app/model/Estacao.java`

---

### Código 3 — `MedicaoAmbiental.java` — Entidade principal com campos transientes

Entidade central do sistema. Demonstra: relacionamento `@ManyToOne` para `Estacao`,
`@Enumerated` para o campo `origem`, índices com `@Index`, e campos `@Transient`
para transporte temporário de dados entre camadas sem persistência no banco.

```java
@Entity
@Table(name = "medicoes_ambientais", indexes = {
    @Index(name = "idx_med_data_hora", columnList = "data_hora"),
    @Index(name = "idx_med_estacao",   columnList = "estacao_id"),
    @Index(name = "idx_med_origem",    columnList = "origem")
})
public class MedicaoAmbiental {

    public enum Origem {
        COLETOR, PESQUISA, POST, SINCRONIZACAO, MANUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estacao_id", nullable = false)
    private Estacao estacao;

    @Column(name = "data_hora",        nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "temperatura",      nullable = false)
    private Double temperatura;        // °C

    @Column(name = "umidade",          nullable = false)
    private Double umidade;            // %

    @Column(name = "precipitacao",     nullable = false)
    private Boolean precipitacao;

    @Column(name = "velocidade_vento", nullable = false)
    private Double velocidadeVento;    // km/h

    @Column(name = "sensacao_termica")
    private Double sensacaoTermica;    // °C — calculada pelo backend

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private Origem origem;

    // Campos transientes: transportam localização antes de persistir
    @Transient private String estacaoIdTemp;
    @Transient private String cidadeTemp;
    @Transient private String paisTemp;

    // Conveniência: acessa nome da cidade via relacionamento
    public String getCidade() {
        if (estacao != null && estacao.getCidade() != null)
            return estacao.getCidade().getNome();
        return null;
    }
}
```

> **Fonte:** `src/main/java/com/app/model/MedicaoAmbiental.java`

---

## 2. Repositório — Upsert Thread-Safe

### Código 4 — `EstacaoRepository.java` — Upsert com tratamento de race condition

Demonstra o padrão *upsert* implementado sem frameworks: tenta criar `Cidade` e
`Estacao` em uma única transação. Se duas threads tentarem criar o mesmo registro
simultaneamente (*race condition*), a que perder na restrição `UNIQUE KEY` captura
a `ConstraintViolationException` e busca o registro já criado pela outra thread.

```java
public Estacao resolverEstacao(String estacaoId, String nomeCidade,
                               String pais, Estacao.Tipo tipo) {
    String idLimpo   = estacaoId.trim();
    String nomeLimpo = (nomeCidade != null) ? nomeCidade.trim() : null;
    String paisLimpo = (pais != null)       ? pais.toUpperCase() : "BR";

    try {
        return dbManager.executeTransactionalResult(session -> {

            // 1. Verifica se a estação já existe
            Estacao estacao = session.find(Estacao.class, idLimpo);
            if (estacao != null) return estacao;

            // 2. Busca ou cria cidade na mesma transação
            Cidade cidade = resolverCidadeNaSessao(session, nomeLimpo, paisLimpo);

            // 3. Cria e persiste nova estação
            Estacao nova = new Estacao(idLimpo, cidade, tipo);
            session.persist(nova);
            session.flush();
            return nova;
        });

    } catch (RuntimeException e) {
        // Race condition: outra thread criou primeiro
        if (isCausaConstraintViolation(e)) {
            return buscarEstacaoExistente(idLimpo);
        }
        throw e;
    }
}

private Cidade resolverCidadeNaSessao(Session session,
                                      String nome, String pais) {
    if (nome == null) return null;

    // Busca cidade existente
    Cidade existente = session.createQuery(
            "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND pais = :pais",
            Cidade.class)
            .setParameter("nome", nome)
            .setParameter("pais", pais)
            .setMaxResults(1)
            .uniqueResultOptional()
            .orElse(null);

    if (existente != null) return existente;

    // Cria nova cidade
    Cidade nova = new Cidade(nome, pais);
    session.persist(nova);
    session.flush(); // garante ID antes de criar Estacao
    return nova;
}
```

> **Fonte:** `src/main/java/com/app/dao/EstacaoRepository.java`

---

## 3. Camada de Serviço — Lógica de Negócio

### Código 5 — `RegistroMedicaoServiceImpl.java` — Preparação e validação

Aplica o **SRP**: valida limites físicos, define origem, resolve estação/cidade
e calcula sensação térmica antes de delegar persistência ao DAO.

```java
public void prepararMedicao(MedicaoAmbiental medicao, Origem origem) {

    // Validação dos dados
    validarLimitesFisicos(medicao.getTemperatura(), medicao.getUmidade());

    // Define a origem do registro (coletor, pesquisa, post, etc.)
    medicao.setOrigem(origem != null ? origem : Origem.MANUAL);

    // Resolve estação e cidade se ainda não vinculadas
    if (medicao.getEstacao() == null) resolverEstacao(medicao);

    // Define valores padrão para campos obrigatórios
    if (medicao.getDataHora()        == null) medicao.setDataHora(LocalDateTime.now());
    if (medicao.getPrecipitacao()    == null) medicao.setPrecipitacao(false);
    if (medicao.getVelocidadeVento() == null) medicao.setVelocidadeVento(0.0);

    // Calcula sensação térmica automaticamente
    if (medicao.getSensacaoTermica() == null) {
        double calc = calculadoraSensacaoTermica.calcular(
            medicao.getTemperatura(),
            medicao.getUmidade()        != null ? medicao.getUmidade()        : 50.0,
            medicao.getVelocidadeVento()!= null ? medicao.getVelocidadeVento(): 0.0
        );
        medicao.setSensacaoTermica(Math.round(calc * 100.0) / 100.0);
    }
}

public void validarLimitesFisicos(Double temp, Double umi) {
    if (temp == null)
        throw new IllegalArgumentException("A temperatura é obrigatória.");
    if (temp < -90.0 || temp > 65.0)
        throw new IllegalArgumentException(
            String.format("Temperatura inválida (%.1f °C).", temp));
    if (umi == null)
        throw new IllegalArgumentException("A umidade é obrigatória.");
    if (umi < 0.0 || umi > 100.0)
        throw new IllegalArgumentException(
            String.format("Umidade inválida (%.1f%%).", umi));
}
```

> **Fonte:** `src/main/java/com/app/service/registro/RegistroMedicaoServiceImpl.java`

---

### Código 6 — `CalculadoraSensacaoTermicaImpl.java` — Fórmulas Wind Chill e Heat Index

Implementa as duas fórmulas meteorológicas internacionais:
**Wind Chill** (Environment Canada) e **Heat Index** (Steadman, 1979).

```java
@Override
public double calcular(double tempC, double umidade, double ventoKmH) {

    // Wind Chill — resfriamento pelo vento (temperatura ≤ 10 °C, vento > 4,8 km/h)
    if (tempC <= 10.0 && ventoKmH > 4.8) {
        double v016 = Math.pow(ventoKmH, 0.16);
        return 13.12
             + (0.6215  * tempC)
             - (11.37   * v016)
             + (0.3965  * tempC * v016);
    }

    // Heat Index (Steadman) — calor + umidade (temp ≥ 26 °C, umidade ≥ 40 %)
    if (tempC >= 26.0 && umidade >= 40.0) {
        double tF = (tempC * 9.0 / 5.0) + 32.0; // converte para °F
        double r  = umidade;

        double hiF = -42.379
            + (2.04901523   * tF)
            + (10.14333127  * r)
            - (0.22475541   * tF * r)
            - (6.83783e-3   * tF * tF)
            - (5.481717e-2  * r  * r)
            + (1.22874e-3   * tF * tF * r)
            + (8.5282e-4    * tF * r  * r)
            - (1.99e-6      * tF * tF * r * r);

        return (hiF - 32.0) * 5.0 / 9.0; // converte de volta para °C
    }

    return tempC; // clima ameno — temperatura real preservada
}
```

> **Fonte:** `src/main/java/com/app/service/calculo/CalculadoraSensacaoTermicaImpl.java`

---

### Código 7 — `ColetorAgendadoServiceImpl.java` — Coleta periódica com thread-safe

Demonstra o uso de `ScheduledExecutorService` para execução periódica,
`AtomicInteger` para rotação thread-safe entre cidades, e `CopyOnWriteArrayList`
permitindo adição dinâmica sem locks manuais.

```java
public ColetorAgendadoServiceImpl(...) {
    this.cidades = new CopyOnWriteArrayList<>(cidadesIniciais);
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Coletor-OpenWeather-10s");
        t.setDaemon(true); // não impede o encerramento da JVM
        return t;
    });
}

@Override
public void iniciar() {
    scheduler.scheduleAtFixedRate(
        this::executarCicloColeta, 0, 10, TimeUnit.SECONDS);
}

@Override
public void executarCicloColeta() {
    if (cidades.isEmpty()) return;

    // Rotação round-robin thread-safe
    int idx = Math.floorMod(indiceCidade.getAndIncrement(), cidades.size());
    String cidadeAlvo = cidades.get(idx);

    try {
        MedicaoAmbiental leitura = openWeatherClient.buscarClima(cidadeAlvo);
        registroMedicaoService.registrar(leitura, MedicaoAmbiental.Origem.COLETOR);
    } catch (Exception e) {
        AsyncLogger.error("Falha na coleta para '"
            + cidadeAlvo + "': " + e.getMessage(), e);
    }
}
```

> **Fonte:** `src/main/java/com/app/service/coleta/ColetorAgendadoServiceImpl.java`

---

## 4. Camada de Handler — API REST

### Código 8 — `MedicaoApiHandler.java` — Dispatcher por método HTTP

Aplica o **DIP**: o dispatcher depende de interfaces (`MedicaoGetHandler`,
`MedicaoPostHandler`, `MedicaoDeleteHandler`), não de implementações concretas.
O roteamento usa `switch` expression do Java 14+.

```java
public class MedicaoApiHandler implements HttpHandler {

    private final MedicaoGetHandler    getHandler;
    private final MedicaoPostHandler   postHandler;
    private final MedicaoDeleteHandler deleteHandler;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        HttpRequest        req  = new HttpRequestImpl(exchange);
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        // Trata preflight CORS sem processar
        if ("OPTIONS".equalsIgnoreCase(method)) {
            send.noContent();
            return;
        }

        try {
            switch (method) {
                case "GET"    -> getHandler.tratar(req, send);
                case "POST"   -> postHandler.tratar(req, send);
                case "DELETE" -> deleteHandler.tratar(req, send);
                default       -> send.metodoNaoPermitido();
            }
        } catch (IllegalArgumentException e) {
            send.badRequest(e.getMessage());
        } catch (Exception e) {
            send.erroInterno(e.getMessage());
        }
    }
}
```

> **Fonte:** `src/main/java/com/app/handler/MedicaoApiHandler.java`

---

### Código 9 — `MedicaoPostHandlerImpl.java` — Handler POST com múltiplos fluxos

Demonstra o roteamento interno de um único endpoint POST para 5 fluxos diferentes
(sincronização em lote, coletor, inserção com localização, inserção manual),
cada um com `origem` específica para rastreabilidade.

```java
@Override
public void tratar(HttpRequest req, HttpResponseSender send) throws IOException {
    String path  = req.getPath();
    String corpo = req.getBody();

    // Fluxo 1: sincronização em lote
    if (path.endsWith("/sincronizacoes")) {
        SincronizacaoRequest syncReq = SincronizacaoRequest.fromJson(corpo);
        List<MedicaoAmbiental> paraRegistrar = new ArrayList<>();
        for (String local : syncReq.locais()) {
            MedicaoAmbiental clima = openWeatherClient.buscarClima(local);
            if (clima != null) paraRegistrar.add(clima);
        }
        List<MedicaoAmbiental> salvas =
            registroService.registrarLote(paraRegistrar, Origem.SINCRONIZACAO);
        send.criado(MedicaoResponse.fromEntitiesToJson(salvas));
        return;
    }

    // Fluxo 2: inserção com localização (busca dados reais na OWM)
    MedicaoRequest dto = MedicaoRequest.fromJson(corpo);
    if (dto.temLocalizacao()) {
        MedicaoAmbiental medicao =
            openWeatherClient.buscarClima(dto.resolverLocalizacaoConsulta());
        MedicaoAmbiental salva = registroService.registrar(medicao, Origem.POST);
        send.criado(MedicaoResponse.fromEntity(salva).toJson());
        return;
    }

    // Fluxo 3: inserção manual (sem cidade, dados fornecidos pelo cliente)
    MedicaoAmbiental manual = new MedicaoAmbiental();
    manual.setEstacaoIdTemp("ESTACAO-MANUAL");
    manual.setTemperatura(dto.temperatura());
    manual.setUmidade(dto.umidade());
    registroService.registrar(manual, Origem.MANUAL);
}
```

> **Fonte:** `src/main/java/com/app/handler/operacoes/post/MedicaoPostHandlerImpl.java`

---

## 5. Consultas HQL com JOIN

### Código 10 — `ConsultaMedicaoDaoImpl.java` — Query paginada com JOIN e filtros dinâmicos

Demonstra construção de HQL dinâmico para filtros opcionais, paginação,
ordenação segura contra SQL Injection (via whitelist), e `JOIN FETCH`
para carregar relacionamentos numa única query sem N+1.

```java
@Override
public List<MedicaoAmbiental> listarComFiltros(
        String cidade, Double tempMin, Double tempMax,
        Boolean precipitacao, String sort, String order,
        int page, int pageSize) {

    int firstResult = (Math.max(1, page) - 1) * Math.min(pageSize, 100);
    String safeOrder = "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";

    // Construção dinâmica do HQL com filtros opcionais
    StringBuilder hql = new StringBuilder(
        "FROM MedicaoAmbiental m " +
        "JOIN FETCH m.estacao e LEFT JOIN FETCH e.cidade c WHERE 1=1 ");

    if (cidade != null)       hql.append("AND LOWER(c.nome) LIKE LOWER(:cidade) ");
    if (tempMin != null)      hql.append("AND m.temperatura >= :tempMin ");
    if (tempMax != null)      hql.append("AND m.temperatura <= :tempMax ");
    if (precipitacao != null) hql.append("AND m.precipitacao = :precipitacao ");

    // Mapeamento seguro do campo de ordenação (evita SQL Injection)
    String sortHql = switch (sort != null ? sort.toLowerCase() : "") {
        case "temperatura"  -> "m.temperatura";
        case "umidade"      -> "m.umidade";
        case "cidade"       -> "c.nome";
        default             -> "m.dataHora";
    };
    hql.append("ORDER BY ").append(sortHql).append(" ").append(safeOrder);

    try (Session s = HibernateUtil.getSessionFactory().openSession()) {
        Query<MedicaoAmbiental> q = s.createQuery(hql.toString(), MedicaoAmbiental.class);
        if (cidade != null)       q.setParameter("cidade",      "%" + cidade + "%");
        if (tempMin != null)      q.setParameter("tempMin",      tempMin);
        if (tempMax != null)      q.setParameter("tempMax",      tempMax);
        if (precipitacao != null) q.setParameter("precipitacao", precipitacao);
        return q.setFirstResult(firstResult)
                .setMaxResults(Math.min(pageSize, 100))
                .getResultList();
    }
}
```

> **Fonte:** `src/main/java/com/app/dao/consulta/ConsultaMedicaoDaoImpl.java`

---

### Código 11 — `ConsultaMedicaoDaoImpl.java` — Estatísticas com agregação SQL

Calcula estatísticas climáticas usando uma única query HQL de projeção com
`COUNT`, `MIN`, `MAX`, `AVG` e `SUM`. Evita o problema N+1 ao operar diretamente
no banco em vez de carregar todos os registros em memória.

```java
@Override
public EstatisticasClimaticas calcularEstatisticasAgregadas(
        String cidade, int limite) {

    // Passo 1: seleciona os IDs das N medições mais recentes
    StringBuilder idHql = new StringBuilder(
        "SELECT m.id FROM MedicaoAmbiental m " +
        "JOIN m.estacao e LEFT JOIN e.cidade c ");
    if (cidade != null) idHql.append("WHERE LOWER(c.nome) LIKE LOWER(:cidade) ");
    idHql.append("ORDER BY m.dataHora DESC, m.id DESC");

    List<Long> ids = session.createQuery(idHql.toString(), Long.class)
        .setParameter("cidade", "%" + cidade + "%")
        .setMaxResults(limite)
        .getResultList();

    // Passo 2: uma única query de agregação sobre esses IDs
    String aggHql =
        "SELECT count(m), " +
        "coalesce(min(m.temperatura),0), coalesce(max(m.temperatura),0), " +
        "coalesce(avg(m.temperatura),0), " +
        "coalesce(min(m.umidade),0),     coalesce(max(m.umidade),0), " +
        "coalesce(avg(m.umidade),0), " +
        "coalesce(max(m.velocidadeVento),0), " +
        "sum(case when m.precipitacao = true then 1L else 0L end) " +
        "FROM MedicaoAmbiental m WHERE m.id IN (:ids)";

    Object[] res = session.createQuery(aggHql, Object[].class)
        .setParameterList("ids", ids)
        .uniqueResult();

    return new EstatisticasClimaticas(
        ((Number) res[0]).intValue(),           // total
        ((Number) res[1]).doubleValue(),         // tempMin
        ((Number) res[2]).doubleValue(),         // tempMax
        ((Number) res[3]).doubleValue(),         // tempMedia
        ((Number) res[4]).doubleValue(),         // umidadeMin
        ((Number) res[5]).doubleValue(),         // umidadeMax
        ((Number) res[6]).doubleValue(),         // umidadeMedia
        ((Number) res[7]).doubleValue(),         // ventoMaximo
        ((Number) res[8]).longValue()            // ocorrenciasChuva
    );
}
```

> **Fonte:** `src/main/java/com/app/dao/consulta/ConsultaMedicaoDaoImpl.java`

---

## 6. Utilitários

### Código 12 — `AsyncLogger.java` — Logging assíncrono com fila de eventos

Implementa um sistema de log assíncrono usando `LinkedBlockingQueue` com capacidade
de 4096 eventos, consumida por uma thread daemon dedicada. As threads HTTP nunca
bloqueiam em operações de I/O de log.

```java
public class AsyncLogger {

    private static final int QUEUE_CAPACITY = 4096;
    private static final BlockingQueue<LogEntry> queue =
        new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final AtomicBoolean running = new AtomicBoolean(true);

    static {
        // Thread daemon consumidora da fila de logs
        Thread worker = new Thread(AsyncLogger::processLogs, "AsyncLogger-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private static void processLogs() {
        while (running.get() || !queue.isEmpty()) {
            try {
                LogEntry entry = queue.poll();
                if (entry != null) imprimir(entry);
                else Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void info(String message) {
        enqueue("INFO", message, null);
    }

    public static void error(String message, Throwable throwable) {
        enqueue("ERROR", message, throwable);
    }

    private static void enqueue(String level, String message, Throwable t) {
        LogEntry entry = new LogEntry(
            LocalDateTime.now().format(TIME_FORMAT),
            level,
            Thread.currentThread().getName(),
            message, t
        );
        // Se a fila estiver cheia, descarta o log (não bloqueia a thread HTTP)
        if (!queue.offer(entry)) {
            System.err.println("[LOG BUFFER FULL] Descartando: " + message);
        }
    }

    private record LogEntry(
        String timestamp, String level,
        String threadName, String message,
        Throwable throwable) {}
}
```

> **Fonte:** `src/main/java/com/app/util/logging/AsyncLogger.java`

---

## 7. Configuração

### Código 13 — `hibernate.cfg.xml` — Mapeamento ORM e configuração JDBC

Configura a conexão ao MySQL, estratégia DDL e mapeamento das três entidades
do modelo relacional. O `batch_size=25` agrupa inserts em lote para performance.

```xml
<hibernate-configuration>
  <session-factory>

    <property name="hibernate.connection.driver_class">
        com.mysql.cj.jdbc.Driver
    </property>
    <property name="hibernate.connection.url">
        jdbc:mysql://localhost:3306/estacao_ambiental_db?
        createDatabaseIfNotExist=true&amp;useSSL=false&amp;
        serverTimezone=America/Sao_Paulo
    </property>

    <!--
      update: ajusta schema preservando dados (uso normal)
      create: recria tabelas do zero (reset/primeira execução)
    -->
    <property name="hibernate.hbm2ddl.auto">update</property>

    <!-- Agrupa 25 inserts numa única operação JDBC -->
    <property name="hibernate.jdbc.batch_size">25</property>
    <property name="hibernate.order_inserts">true</property>

    <!-- Mapeamento das 3 entidades do modelo relacional -->
    <mapping class="com.app.model.Cidade"/>
    <mapping class="com.app.model.Estacao"/>
    <mapping class="com.app.model.MedicaoAmbiental"/>

  </session-factory>
</hibernate-configuration>
```

> **Fonte:** `src/main/resources/hibernate.cfg.xml`

---

## Referência do Repositório

**Repositório:** https://github.com/BatistaSec/APS-2026-2-backend
**Linguagem:** Java 21
**Dependências:** Hibernate ORM 6.5.2.Final · MySQL Connector/J 8.3.0
**Build:** Apache Maven 3.8+

# Trechos de Código — Estação de Monitoramento Ambiental (APS)

> Documento de apoio para a elaboração do trabalho acadêmico.
> Os trechos abaixo representam as partes mais relevantes do sistema,
> organizados por camada da arquitetura.
> Formato de referência: **ABNT NBR 6023** e **NBR 10520** (citação de código-fonte).

---

## 1. Camada de Modelo — Entidades JPA

### Código 1 — Entidade `Cidade.java`

Representa uma cidade monitorada. Utiliza a anotação `@UniqueConstraint` para garantir
integridade referencial, impedindo cadastro duplicado de cidades com mesmo nome e país.

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
    // getters e setters omitidos
}
```

> **Fonte:** `src/main/java/com/app/model/Cidade.java`

---

### Código 2 — Entidade `MedicaoAmbiental.java`

Entidade principal do sistema. Demonstra o relacionamento `@ManyToOne` com `Estacao`,
o uso de `@Enumerated` para o campo `origem` e campos `@Transient` para transporte
temporário de dados entre camadas sem persistência.

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
    private Double sensacaoTermica;    // calculada pelo backend

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private Origem origem;

    // Campos transientes: transportam localização antes de persistir
    @Transient private String estacaoIdTemp;
    @Transient private String cidadeTemp;
    @Transient private String paisTemp;
}
```

> **Fonte:** `src/main/java/com/app/model/MedicaoAmbiental.java`

---

## 2. Camada de Repositório — Upsert Thread-Safe

### Código 3 — `EstacaoRepository.java` — Upsert com tratamento de race condition

Demonstra o padrão *upsert* (insert or select) implementado manualmente em Java,
dentro de uma única transação Hibernate. O bloco `catch` trata a condição de corrida
(*race condition*) entre threads paralelas usando `ConstraintViolationException`.

```java
public Estacao resolverEstacao(String estacaoId, String nomeCidade,
                               String pais, Estacao.Tipo tipo) {
    String idLimpo   = estacaoId.trim();
    String nomeLimpo = (nomeCidade != null) ? nomeCidade.trim() : null;
    String paisLimpo = (pais != null)       ? pais.trim().toUpperCase() : "BR";

    try {
        return dbManager.executeTransactionalResult(session -> {

            // 1. Busca estação existente
            Estacao estacao = session.find(Estacao.class, idLimpo);
            if (estacao != null) return estacao;

            // 2. Busca ou cria cidade na mesma sessão
            Cidade cidade = resolverCidadeNaSessao(session, nomeLimpo, paisLimpo);

            // 3. Cria e persiste nova estação
            Estacao nova = new Estacao(idLimpo, cidade, tipo);
            session.persist(nova);
            session.flush();
            return nova;
        });

    } catch (RuntimeException e) {
        // Race condition: outra thread criou primeiro — busca o registro existente
        if (isCausaConstraintViolation(e)) {
            return buscarEstacaoExistente(idLimpo);
        }
        throw e;
    }
}

private Cidade resolverCidadeNaSessao(Session session, String nome, String pais) {
    if (nome == null) return null;

    Cidade existente = session.createQuery(
            "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND pais = :pais",
            Cidade.class)
            .setParameter("nome", nome)
            .setParameter("pais", pais)
            .setMaxResults(1)
            .uniqueResultOptional()
            .orElse(null);

    if (existente != null) return existente;

    Cidade nova = new Cidade(nome, pais);
    session.persist(nova);
    session.flush();
    return nova;
}
```

> **Fonte:** `src/main/java/com/app/dao/EstacaoRepository.java`

---

## 3. Camada de Serviço — Lógica de Negócio

### Código 4 — `RegistroMedicaoServiceImpl.java` — Preparação e validação da medição

Demonstra o princípio **SRP** (Single Responsibility Principle): a classe é responsável
apenas por validar dados, resolver a estação e calcular a sensação térmica antes
de delegar a persistência ao DAO.

```java
public void prepararMedicao(MedicaoAmbiental medicao, Origem origem) {

    // Validação dos limites físicos
    validarLimitesFisicos(medicao.getTemperatura(), medicao.getUmidade());

    // Define a origem do registro
    medicao.setOrigem(origem != null ? origem : Origem.MANUAL);

    // Resolve estação/cidade se ainda não vinculada
    if (medicao.getEstacao() == null) {
        resolverEstacao(medicao);
    }

    // Valores padrão para campos obrigatórios
    if (medicao.getDataHora()       == null) medicao.setDataHora(LocalDateTime.now());
    if (medicao.getPrecipitacao()   == null) medicao.setPrecipitacao(false);
    if (medicao.getVelocidadeVento()== null) medicao.setVelocidadeVento(0.0);

    // Calcula sensação térmica se ausente
    if (medicao.getSensacaoTermica() == null && medicao.getTemperatura() != null) {
        double calc = calculadoraSensacaoTermica.calcular(
            medicao.getTemperatura(),
            medicao.getUmidade()        != null ? medicao.getUmidade()        : 50.0,
            medicao.getVelocidadeVento()!= null ? medicao.getVelocidadeVento(): 0.0
        );
        medicao.setSensacaoTermica(Math.round(calc * 100.0) / 100.0);
    }
}
```

> **Fonte:** `src/main/java/com/app/service/registro/RegistroMedicaoServiceImpl.java`

---

### Código 5 — `CalculadoraSensacaoTermicaImpl.java` — Fórmulas meteorológicas

Implementa as duas fórmulas internacionais para cálculo de sensação térmica:
**Wind Chill** (resfriamento pelo vento) e **Heat Index** (Steadman, 1979).

```java
@Override
public double calcular(double tempC, double umidade, double ventoKmH) {

    // Wind Chill — clima frio (≤ 10 °C) com vento ativo (> 4,8 km/h)
    if (tempC <= 10.0 && ventoKmH > 4.8) {
        double v016 = Math.pow(ventoKmH, 0.16);
        return 13.12
             + (0.6215  * tempC)
             - (11.37   * v016)
             + (0.3965  * tempC * v016);
    }

    // Heat Index (Steadman) — clima quente (≥ 26 °C) e úmido (umidade ≥ 40 %)
    if (tempC >= 26.0 && umidade >= 40.0) {
        double tF = (tempC * 9.0 / 5.0) + 32.0; // converte para Fahrenheit
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

        return (hiF - 32.0) * 5.0 / 9.0; // converte de volta para Celsius
    }

    // Clima ameno — retorna temperatura real sem ajuste
    return tempC;
}
```

> **Fonte:** `src/main/java/com/app/service/calculo/CalculadoraSensacaoTermicaImpl.java`

---

### Código 6 — `ColetorAgendadoServiceImpl.java` — Coleta automática agendada

Demonstra o uso de `ScheduledExecutorService` para execução periódica a cada 10 segundos,
com `AtomicInteger` garantindo a rotação thread-safe entre cidades do pool,
e `CopyOnWriteArrayList` permitindo adição dinâmica de cidades sem locks manuais.

```java
public ColetorAgendadoServiceImpl(...) {
    this.cidades   = new CopyOnWriteArrayList<>(cidadesIniciais);
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Coletor-OpenWeather-10s");
        t.setDaemon(true);
        return t;
    });
}

@Override
public void iniciar() {
    scheduler.scheduleAtFixedRate(this::executarCicloColeta, 0, 10, TimeUnit.SECONDS);
}

@Override
public void executarCicloColeta() {
    if (cidades.isEmpty()) return;

    // Rotação round-robin thread-safe usando AtomicInteger
    int idx = Math.floorMod(indiceCidade.getAndIncrement(), cidades.size());
    String cidadeAlvo = cidades.get(idx);

    try {
        MedicaoAmbiental leitura = openWeatherClient.buscarClima(cidadeAlvo);
        registroMedicaoService.registrar(leitura, MedicaoAmbiental.Origem.COLETOR);
    } catch (Exception e) {
        AsyncLogger.error("Falha na coleta para '" + cidadeAlvo + "': " + e.getMessage(), e);
    }
}
```

> **Fonte:** `src/main/java/com/app/service/coleta/ColetorAgendadoServiceImpl.java`

---

## 4. Camada de Handler — Dispatcher HTTP

### Código 7 — `MedicaoApiHandler.java` — Padrão Dispatcher / Command

Demonstra o princípio **DIP** (Dependency Inversion Principle) e o padrão *Dispatcher*:
o handler depende de interfaces (`MedicaoGetHandler`, `MedicaoPostHandler`,
`MedicaoDeleteHandler`), não de implementações concretas. O roteamento por método HTTP
é feito com `switch` expression (Java 14+).

```java
public class MedicaoApiHandler implements HttpHandler {

    private final MedicaoGetHandler    getHandler;
    private final MedicaoPostHandler   postHandler;
    private final MedicaoDeleteHandler deleteHandler;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        HttpRequest req  = new HttpRequestImpl(exchange);
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            send.noContent();   // responde preflight CORS sem processar
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

## 5. Configuração — Mapeamento Hibernate

### Código 8 — `hibernate.cfg.xml` — Configuração do ORM

Arquivo de configuração do Hibernate responsável pela conexão ao banco de dados
MySQL e pelo mapeamento das três entidades do modelo relacional.

```xml
<hibernate-configuration>
  <session-factory>

    <property name="hibernate.connection.driver_class">
        com.mysql.cj.jdbc.Driver
    </property>
    <property name="hibernate.connection.url">
        jdbc:mysql://localhost:3306/estacao_ambiental_db?
        createDatabaseIfNotExist=true&amp;useSSL=false&amp;
        serverTimezone=America/Sao_Paulo&amp;characterEncoding=UTF-8
    </property>

    <!-- update: preserva dados; create: recria tabelas do zero -->
    <property name="hibernate.hbm2ddl.auto">update</property>

    <!-- Batching JDBC: agrupa 25 inserts numa única operação -->
    <property name="hibernate.jdbc.batch_size">25</property>
    <property name="hibernate.order_inserts">true</property>

    <!-- Mapeamento das entidades -->
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
**Dependências:** Hibernate ORM 6.5.2.Final, MySQL Connector/J 8.3.0
**Gerenciador de build:** Apache Maven 3.8+

package com.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Medição climática registrada pelo sistema.
 *
 * Relacionamentos:
 *   medicoes_ambientais.estacao_id → estacoes.id → cidades.id
 *
 * O campo 'origem' identifica qual dos 5 gatilhos gerou o registro:
 *   COLETOR        — coletor automático a cada 10 s
 *   PESQUISA       — GET /atual?cidade=X (frontend)
 *   POST           — POST /medicoes com localização
 *   SINCRONIZACAO  — POST /sincronizacoes (lote)
 *   MANUAL         — POST /medicoes sem cidade (dados manuais)
 */
@Entity
@Table(name = "medicoes_ambientais", indexes = {
    @Index(name = "idx_med_data_hora",  columnList = "data_hora"),
    @Index(name = "idx_med_estacao",    columnList = "estacao_id"),
    @Index(name = "idx_med_origem",     columnList = "origem")
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

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "temperatura", nullable = false)
    private Double temperatura;        // °C

    @Column(name = "umidade", nullable = false)
    private Double umidade;            // %

    @Column(name = "precipitacao", nullable = false)
    private Boolean precipitacao;      // true = chuva detectada

    @Column(name = "velocidade_vento", nullable = false)
    private Double velocidadeVento;    // km/h

    @Column(name = "pressao_atmosferica")
    private Double pressaoAtmosferica; // hPa

    @Column(name = "direcao_vento", length = 10)
    private String direcaoVento;       // N, NE, L, SE, S, SO, O, NO

    @Column(name = "indice_uv")
    private Double indiceUv;           // 0 a 12+

    @Column(name = "sensacao_termica")
    private Double sensacaoTermica;    // °C — calculada pelo backend

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private Origem origem;

    // ── campos transientes: usados apenas antes de persistir ──────────────
    // Transportam dados de localização vindos do OpenWeatherClient
    // para o RegistroMedicaoServiceImpl resolver a Estacao/Cidade.
    @Transient private String estacaoIdTemp;
    @Transient private String cidadeTemp;
    @Transient private String estadoTemp;
    @Transient private String paisTemp;

    public MedicaoAmbiental() {}

    // ── getters / setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Estacao getEstacao() { return estacao; }
    public void setEstacao(Estacao estacao) { this.estacao = estacao; }

    // Conveniência: retorna o nome da cidade via relacionamento
    public String getCidade() {
        if (estacao != null && estacao.getCidade() != null) {
            return estacao.getCidade().getNome();
        }
        return null;
    }

    // Conveniência: retorna o ID da estação via relacionamento
    public String getEstacaoId() {
        return estacao != null ? estacao.getId() : null;
    }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public Double getUmidade() { return umidade; }
    public void setUmidade(Double umidade) { this.umidade = umidade; }

    public Boolean getPrecipitacao() { return precipitacao; }
    public void setPrecipitacao(Boolean precipitacao) { this.precipitacao = precipitacao; }

    public Double getVelocidadeVento() { return velocidadeVento; }
    public void setVelocidadeVento(Double velocidadeVento) { this.velocidadeVento = velocidadeVento; }

    public Double getPressaoAtmosferica() { return pressaoAtmosferica; }
    public void setPressaoAtmosferica(Double pressaoAtmosferica) { this.pressaoAtmosferica = pressaoAtmosferica; }

    public String getDirecaoVento() { return direcaoVento; }
    public void setDirecaoVento(String direcaoVento) { this.direcaoVento = direcaoVento; }

    public Double getIndiceUv() { return indiceUv; }
    public void setIndiceUv(Double indiceUv) { this.indiceUv = indiceUv; }

    public Double getSensacaoTermica() { return sensacaoTermica; }
    public void setSensacaoTermica(Double sensacaoTermica) { this.sensacaoTermica = sensacaoTermica; }

    public Origem getOrigem() { return origem; }
    public void setOrigem(Origem origem) { this.origem = origem; }

    // getters/setters dos campos transientes
    public String getEstacaoIdTemp() { return estacaoIdTemp; }
    public void setEstacaoIdTemp(String estacaoIdTemp) { this.estacaoIdTemp = estacaoIdTemp; }

    public String getCidadeTemp() { return cidadeTemp; }
    public void setCidadeTemp(String cidadeTemp) { this.cidadeTemp = cidadeTemp; }

    public String getEstadoTemp() { return estadoTemp; }
    public void setEstadoTemp(String estadoTemp) { this.estadoTemp = estadoTemp; }

    public String getPaisTemp() { return paisTemp; }
    public void setPaisTemp(String paisTemp) { this.paisTemp = paisTemp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oClass = (o instanceof org.hibernate.proxy.HibernateProxy hp)
                ? hp.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisClass = (this instanceof org.hibernate.proxy.HibernateProxy hp)
                ? hp.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisClass != oClass) return false;
        MedicaoAmbiental that = (MedicaoAmbiental) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return (this instanceof org.hibernate.proxy.HibernateProxy hp)
                ? hp.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public String toString() {
        String dataF = dataHora != null
                ? dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : "não informada";
        return String.format(java.util.Locale.ROOT,
                "MedicaoAmbiental[id=%d, cidade='%s', origem=%s, hora=%s, temp=%.1f°C, umi=%.1f%%, chuva=%s]",
                id, getCidade(), origem, dataF, temperatura != null ? temperatura : 0.0,
                umidade != null ? umidade : 0.0,
                Boolean.TRUE.equals(precipitacao) ? "sim" : "não");
    }
}

package com.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;


@Entity
@Table(name = "medicoes_ambientais", indexes = {
    @Index(name = "idx_data_hora", columnList = "data_hora"),
    @Index(name = "idx_estacao_id", columnList = "estacao_id"),
    @Index(name = "idx_cidade", columnList = "cidade")
})
public class MedicaoAmbiental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estacao_id", nullable = false, length = 50)
    private String estacaoId;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "temperatura", nullable = false)
    private Double temperatura; // °C

    @Column(name = "umidade", nullable = false)
    private Double umidade; // %

    @Column(name = "precipitacao", nullable = false)
    private Boolean precipitacao; // true = chuva detectada, false = sem chuva

    @Column(name = "velocidade_vento", nullable = false)
    private Double velocidadeVento; // km/h

    @Column(name = "pressao_atmosferica")
    private Double pressaoAtmosferica; // hPa

    @Column(name = "direcao_vento", length = 10)
    private String direcaoVento; // ex: N, NE, L, SE, S, SO, O, NO

    @Column(name = "indice_uv")
    private Double indiceUv; // Índice UV (0 a 12+)

    @Column(name = "sensacao_termica")
    private Double sensacaoTermica; // °C


    public MedicaoAmbiental() {
    }

    public MedicaoAmbiental(String estacaoId, String cidade, LocalDateTime dataHora, Double temperatura, Double umidade,
                            Boolean precipitacao, Double velocidadeVento, Double pressaoAtmosferica,
                            String direcaoVento, Double indiceUv, Double sensacaoTermica) {
        this.estacaoId = estacaoId != null ? estacaoId : "ESTACAO-01";
        this.cidade = cidade;
        this.dataHora = dataHora != null ? dataHora : LocalDateTime.now();
        this.temperatura = temperatura;
        this.umidade = umidade;
        this.precipitacao = precipitacao;
        this.velocidadeVento = velocidadeVento;
        this.pressaoAtmosferica = pressaoAtmosferica;
        this.direcaoVento = direcaoVento;
        this.indiceUv = indiceUv;
        this.sensacaoTermica = sensacaoTermica;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEstacaoId() {
        return estacaoId;
    }

    public void setEstacaoId(String estacaoId) {
        this.estacaoId = estacaoId;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora)
    {
        this.dataHora = dataHora;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public Double getUmidade() {
        return umidade;
    }

    public void setUmidade(Double umidade) {
        this.umidade = umidade;
    }

    public Boolean getPrecipitacao() {
        return precipitacao;
    }

    public void setPrecipitacao(Boolean precipitacao) {
        this.precipitacao = precipitacao;
    }

    public Double getVelocidadeVento() {
        return velocidadeVento;
    }

    public void setVelocidadeVento(Double velocidadeVento) {
        this.velocidadeVento = velocidadeVento;
    }

    public Double getPressaoAtmosferica() {
        return pressaoAtmosferica;
    }

    public void setPressaoAtmosferica(Double pressaoAtmosferica) {
        this.pressaoAtmosferica = pressaoAtmosferica;
    }

    public String getDirecaoVento() {
        return direcaoVento;
    }

    public void setDirecaoVento(String direcaoVento) {
        this.direcaoVento = direcaoVento;
    }

    public Double getIndiceUv() {
        return indiceUv;
    }

    public void setIndiceUv(Double indiceUv) {
        this.indiceUv = indiceUv;
    }

    public Double getSensacaoTermica() {
        return sensacaoTermica;
    }

    public void setSensacaoTermica(Double sensacaoTermica) {
        this.sensacaoTermica = sensacaoTermica;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = (o instanceof org.hibernate.proxy.HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = (this instanceof org.hibernate.proxy.HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MedicaoAmbiental that = (MedicaoAmbiental) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return (this instanceof org.hibernate.proxy.HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }

    @Override
    public String toString() {
        String dataFormatada = (dataHora != null)
                ? com.app.util.i18n.I18n.formatarDataHora(dataHora)
                : "não informada";
        return String.format(com.app.util.i18n.I18n.LOCALE_PADRAO,
                "MedicaoAmbiental[id=%d, estacao='%s', cidade='%s', hora=%s, temp=%.1f °C, umi=%.1f%%, chuva=%s, vento=%.1f km/h, pressao=%.1f hPa, dir=%s, UV=%.1f, sensacao=%.1f °C]",
                id, estacaoId, cidade, dataFormatada,
                temperatura != null ? temperatura : 0.0,
                umidade != null ? umidade : 0.0,
                Boolean.TRUE.equals(precipitacao) ? "sim" : "não",
                velocidadeVento != null ? velocidadeVento : 0.0,
                pressaoAtmosferica != null ? pressaoAtmosferica : 0.0,
                direcaoVento != null ? direcaoVento : "N",
                indiceUv != null ? indiceUv : 0.0,
                sensacaoTermica != null ? sensacaoTermica : 0.0);
    }
}

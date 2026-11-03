package com.app.model;

import jakarta.persistence.*;

/**
 * Representa uma estação de coleta de dados.
 * Cada estação pertence a uma cidade e tem um tipo (origem dos dados).
 * Tabela: estacoes
 */
@Entity
@Table(name = "estacoes")
public class Estacao {

    public enum Tipo {
        OWM,      // OpenWeatherMap (coletor automático ou pesquisa)
        MANUAL    // Inserção manual via POST sem cidade
    }

    @Id
    @Column(name = "id", length = 60)
    private String id; // ex: "OPENWEATHER-SAO-PAULO", "ESTACAO-MANUAL"

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "cidade_id", nullable = true)
    private Cidade cidade; // null para estações manuais sem localização

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private Tipo tipo;

    public Estacao() {}

    public Estacao(String id, Cidade cidade, Tipo tipo) {
        this.id = id;
        this.cidade = cidade;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Cidade getCidade() { return cidade; }
    public void setCidade(Cidade cidade) { this.cidade = cidade; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return "Estacao[id=" + id + ", tipo=" + tipo + "]";
    }
}

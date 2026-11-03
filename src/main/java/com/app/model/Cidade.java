package com.app.model;

import jakarta.persistence.*;

/**
 * Representa uma cidade monitorada pelo sistema.
 * Tabela: cidades
 */
@Entity
@Table(name = "cidades", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cidade_estado_pais", columnNames = {"nome", "estado", "pais"})
})
public class Cidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "estado", length = 2)
    private String estado; // sigla BR: SP, RJ, etc.

    @Column(name = "pais", length = 2)
    private String pais; // ISO 2: BR, JP, US, etc.

    public Cidade() {}

    public Cidade(String nome, String estado, String pais) {
        this.nome = nome;
        this.estado = estado;
        this.pais = pais;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    @Override
    public String toString() {
        return "Cidade[id=" + id + ", nome=" + nome + ", estado=" + estado + ", pais=" + pais + "]";
    }
}

package com.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cidades", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cidade_nome_pais", columnNames = { "nome", "pais" })
})
public class Cidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "pais", length = 10)
    private String pais;

    public Cidade() {}

    public Cidade(String nome, String pais) {
        this.nome = nome;
        this.pais = pais;
    }

    public Long getId()          { return id; }
    public void setId(Long id)   { this.id = id; }

    public String getNome()            { return nome; }
    public void setNome(String nome)   { this.nome = nome; }

    public String getPais()            { return pais; }
    public void setPais(String pais)   { this.pais = pais; }

    @Override
    public String toString() {
        return "Cidade[id=" + id + ", nome=" + nome + ", pais=" + pais + "]";
    }
}

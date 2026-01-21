package br.com.falbot.seplag.backend.dominio;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "regional")
public class Regional {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id_regional", nullable = false)
    private UUID idRegional;

    //ID vindo do endpoint externo (coluna chama "id")
    @Column(name = "id")
    private Integer idExterno;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (this.criadoEm == null) this.criadoEm = OffsetDateTime.now();
    }

    // getters/setters
    public UUID getIdRegional() { return idRegional; }
    public Integer getIdExterno() { return idExterno; }
    public void setIdExterno(Integer idExterno) { this.idExterno = idExterno; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}

package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.evidence.EvidenceType;
import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.identity.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Mensagem recebida do campo e o que foi feito com ela. Serve de histórico e para ignorar reenvios. */
@Entity
@Table(name = "mensagem_campo")
public class FieldMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private MessageChannel channel;

    @Column(name = "id_externo", length = 120)
    private String externalId;

    @Column(name = "texto", length = 4000)
    private String text;

    @Column(name = "transcricao", length = 4000)
    private String transcript;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_midia", length = 30)
    private EvidenceType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 30)
    private FieldMessageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30)
    private MessageKind kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "talhao_id")
    private Plot plot;

    @Column(name = "evidencia_id")
    private UUID evidenceId;

    @Column(name = "atividade_id")
    private UUID activityId;

    @Column(name = "ocorrencia_id")
    private UUID occurrenceId;

    /** Resposta bruta da IA, guardada como veio para auditoria e reprocessamento. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interpretacao", columnDefinition = "jsonb")
    private String interpretation;

    @Column(name = "resposta", length = 1000)
    private String reply;

    @Column(name = "mensagem_erro", length = 500)
    private String errorMessage;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public MessageChannel getChannel() { return channel; }
    public void setChannel(MessageChannel channel) { this.channel = channel; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public EvidenceType getMediaType() { return mediaType; }
    public void setMediaType(EvidenceType mediaType) { this.mediaType = mediaType; }
    public FieldMessageStatus getStatus() { return status; }
    public void setStatus(FieldMessageStatus status) { this.status = status; }
    public MessageKind getKind() { return kind; }
    public void setKind(MessageKind kind) { this.kind = kind; }
    public Plot getPlot() { return plot; }
    public void setPlot(Plot plot) { this.plot = plot; }
    public UUID getEvidenceId() { return evidenceId; }
    public void setEvidenceId(UUID evidenceId) { this.evidenceId = evidenceId; }
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
    public UUID getOccurrenceId() { return occurrenceId; }
    public void setOccurrenceId(UUID occurrenceId) { this.occurrenceId = occurrenceId; }
    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

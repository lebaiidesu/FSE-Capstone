package com.bank.ledger.model.postgres;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECONCILIATION_LOG")
public class ReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recon_id")
    private Long reconId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "oracle_status", nullable = false, length = 30)
    private String oracleStatus; // 'SUCCESS', 'FAILED', 'COMMITTED'

    @Column(name = "postgres_status", nullable = false, length = 30)
    private String postgresStatus; // 'COMMITTED', 'PENDING', 'MISSING'

    @Column(name = "recon_status", nullable = false, length = 30)
    private String reconStatus; // 'MATCHED', 'DRIFT_DETECTED'

    @Column(name = "recon_date", nullable = false, updatable = false)
    private LocalDateTime reconDate = LocalDateTime.now();

    public ReconciliationLog() {}

    public ReconciliationLog(Long transactionId, String oracleStatus, String postgresStatus, String reconStatus) {
        this.transactionId = transactionId;
        this.oracleStatus = oracleStatus;
        this.postgresStatus = postgresStatus;
        this.reconStatus = reconStatus;
        this.reconDate = LocalDateTime.now();
    }

    public Long getReconId() { return reconId; }
    public void setReconId(Long reconId) { this.reconId = reconId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getOracleStatus() { return oracleStatus; }
    public void setOracleStatus(String oracleStatus) { this.oracleStatus = oracleStatus; }

    public String getPostgresStatus() { return postgresStatus; }
    public void setPostgresStatus(String postgresStatus) { this.postgresStatus = postgresStatus; }

    public String getReconStatus() { return reconStatus; }
    public void setReconStatus(String reconStatus) { this.reconStatus = reconStatus; }

    public LocalDateTime getReconDate() { return reconDate; }
    public void setReconDate(LocalDateTime reconDate) { this.reconDate = reconDate; }
}

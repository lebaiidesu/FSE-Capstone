package com.bank.reconciliation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECONCILIATION_LOG")
public class ReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RECON_ID")
    private Long reconId;

    @Column(name = "TRANSACTION_ID", nullable = false)
    private Long transactionId;

    @Column(name = "ORACLE_STATUS", nullable = false)
    private String oracleStatus;

    @Column(name = "POSTGRES_STATUS", nullable = false)
    private String postgresStatus;

    @Column(name = "RECON_STATUS", nullable = false)
    private String reconStatus;

    @Column(name = "RECON_DATE")
    private LocalDateTime reconDate = LocalDateTime.now();

    public ReconciliationLog() {}

    public ReconciliationLog(Long transactionId, String oracleStatus, String postgresStatus, String reconStatus) {
        this.transactionId = transactionId;
        this.oracleStatus = oracleStatus;
        this.postgresStatus = postgresStatus;
        this.reconStatus = reconStatus;
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

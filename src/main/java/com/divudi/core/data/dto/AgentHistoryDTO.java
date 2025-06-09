package com.divudi.core.data.dto;

import com.divudi.core.entity.AgentHistory;
import java.io.Serializable;
import java.util.Date;

public class AgentHistoryDTO implements Serializable {

    private Long id;
    private double balanceBeforeTransaction;
    private double balanceAfterTransaction;
    private double transactionValue;
    private String historyType;
    private String creatorName;
    private String institutionName;
    private Date createdAt;

    public AgentHistoryDTO() {
    }



    // Constructor
    public AgentHistoryDTO(Long id, double balanceBeforeTransaction, double balanceAfterTransaction,
                           double transactionValue, String historyType, String creatorName,
                           String institutionName, Date createdAt) {
        this.id = id;
        this.balanceBeforeTransaction = balanceBeforeTransaction;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.transactionValue = transactionValue;
        this.historyType = historyType;
        this.creatorName = creatorName;
        this.institutionName = institutionName;
        this.createdAt = createdAt;
    }

    public AgentHistoryDTO(AgentHistory agentHistory) {
        if (agentHistory != null) {
            this.id = agentHistory.getId();
            this.balanceBeforeTransaction = agentHistory.getBalanceBeforeTransaction();
            this.balanceAfterTransaction = agentHistory.getBalanceAfterTransaction();
            this.transactionValue = agentHistory.getTransactionValue();
            this.historyType = agentHistory.getHistoryType() != null
                    ? agentHistory.getHistoryType().name() : null;
            this.creatorName = agentHistory.getCreater() != null
                    ? agentHistory.getCreater().getName() : null;
            this.institutionName = agentHistory.getInstitution() != null
                    ? agentHistory.getInstitution().getName() : null;
            this.createdAt = agentHistory.getCreatedAt();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getBalanceBeforeTransaction() {
        return balanceBeforeTransaction;
    }

    public void setBalanceBeforeTransaction(double balanceBeforeTransaction) {
        this.balanceBeforeTransaction = balanceBeforeTransaction;
    }

    public double getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(double balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public double getTransactionValue() {
        return transactionValue;
    }

    public void setTransactionValue(double transactionValue) {
        this.transactionValue = transactionValue;
    }

    public String getHistoryType() {
        return historyType;
    }

    public void setHistoryType(String historyType) {
        this.historyType = historyType;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

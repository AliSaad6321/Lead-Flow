package com.leadflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records a financial payment transaction when a lead converts to CLOSED_WON.
 */
@Entity
@Table(name = "payments")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private int paymentId;

    @Column(name = "lead_id", nullable = false, unique = true, length = 36)
    private String leadId;

    @Column(name = "recorded_by_employee_id", nullable = false)
    private int recordedByEmployeeId;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /** Default constructor required by Hibernate. */
    public PaymentTransaction() {}

    /**
     * Constructs a payment record linked to a converted lead.
     *
     * @param leadId               converted lead ID
     * @param recordedByEmployeeId ID of employee logging payment
     * @param amount               monetary payment amount
     * @param paymentMethod        payment channel (e.g., Card, Bank Transfer, Cash)
     */
    public PaymentTransaction(String leadId, int recordedByEmployeeId, double amount, String paymentMethod) {
        this.leadId = leadId;
        this.recordedByEmployeeId = recordedByEmployeeId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionDate = LocalDateTime.now();
    }

    public int getPaymentId() { return paymentId; }
    public String getLeadId() { return leadId; }
    public int getRecordedByEmployeeId() { return recordedByEmployeeId; }
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
}
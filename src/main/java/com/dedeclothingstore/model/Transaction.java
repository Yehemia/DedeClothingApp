package com.dedeclothingstore.model;

import java.util.Date;

public class Transaction {
    private int transactionId;
    private int userId;
    private Date transactionDate;
    private double totalPrice;
    private String paymentMethod;

    public Transaction(int transactionId,int userId, Date transactionDate, double totalPrice, String paymentMethod) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.transactionDate = transactionDate;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
    }
    public Transaction(int userId, Date transactionDate, double totalPrice, String paymentMethod) {
        this.userId = userId;
        this.transactionDate = transactionDate;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }


    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

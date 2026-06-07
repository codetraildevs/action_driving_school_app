package com.drivingschoolrwandaapp.models.response;

import com.google.gson.annotations.SerializedName;

public class IremboPaymentResponse {
    @SerializedName("amount")
    private double amount;
    
    @SerializedName("currency")
    private String currency;
    
    @SerializedName("itemName")
    private String itemName;
    
    @SerializedName("recipient")
    private String recipient;
    
    @SerializedName("transactionFee")
    private double transactionFee;
    
    @SerializedName("phoneNumber")
    private String phoneNumber;
    
    @SerializedName("reference")
    private String reference;

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getItemName() {
        return itemName;
    }

    public String getRecipient() {
        return recipient;
    }

    public double getTransactionFee() {
        return transactionFee;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public String getReference() {
        return reference;
    }
}

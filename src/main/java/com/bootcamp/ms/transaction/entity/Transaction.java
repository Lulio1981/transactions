package com.bootcamp.ms.transaction.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
public class Transaction {
    @Id
    private String id;
    private String idMovement;
    private BigDecimal amount;
    private String idOriginTransaction;
    private Date transactionDate;
    private String isoCurrencyCode;
    private String originMovement;
    private String descriptionMovement;
    private Short operationType;
    private Date insertionDate;
    private Boolean isComission;
    private Boolean isPassive;
    private String fk_insertionUser;
    private String insertionTerminal;
    private short registrationStatus;
}

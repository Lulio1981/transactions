package com.bootcamp.ms.transaction.service;

import com.bootcamp.ms.transaction.entity.Movement;
import com.bootcamp.ms.transaction.entity.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;


public interface TransactionService {

    public Flux<Transaction> getAll();

    public Mono<Transaction> getById(String id);

    public Mono<Transaction> save(Transaction transaction);

    public Mono<Transaction> update(Transaction transaction);

    public Mono<Transaction> delete(String id);

    public Flux<Transaction> getByIdOriginTransaction(String idOriginTransaction);

    public Mono<Transaction> generateTransactions(Movement movement);

    public Mono<BigDecimal> getProductBalance(String idOriginTransaction);

    public Mono<Transaction> generateComission(Transaction transaction, Long transactionsAllowed);

    public Mono<Boolean> checkComission(String idOriginTransaction, Long transactionsAllowed);

    Flux<Transaction> getByIdOriginTransactionAndInsertionDateBetweenAndIsComission(String idOriginTransaction, Date startDate, Date finishDate, Boolean isComission);

    Flux<Transaction> getByIdOriginTransactionAndInsertionDateBetween(String idOriginTransaction, Date startDate, Date finishDate);

    Flux<Transaction> getByIdOriginTransactionAndOperationTypeAndInsertionDateBetween(String idOriginTransaction, Short operationType, Date startDate, Date finishDate);

    Mono<Boolean> anyDebtExpired(String idOriginTransaction);

    Flux<Transaction> getTop10ByIdOriginTransactionOrderByInsertionDateDesc(String idOriginTransaction);

}

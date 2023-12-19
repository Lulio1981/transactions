package com.bootcamp.ms.transaction.repository;

import com.bootcamp.ms.transaction.entity.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

    Flux<Transaction> findByIdOriginTransaction(String idOriginTransaction);

    Flux<Transaction> findByIdOriginTransactionAndInsertionDateBetweenAndIsComission(String idOriginTransaction, Date startDate, Date finishDate, Boolean isComission);

    Flux<Transaction> findByIdOriginTransactionAndInsertionDateBetween(String idOriginTransaction, Date startDate, Date finishDate);

    Flux<Transaction> findByIdOriginTransactionAndOperationTypeAndInsertionDateBetween(String idOriginTransaction, Short operationType, Date startDate, Date finishDate);

    Flux<Transaction> findTop10ByIdOriginTransactionOrderByInsertionDateDesc(String idOriginTransaction);
}
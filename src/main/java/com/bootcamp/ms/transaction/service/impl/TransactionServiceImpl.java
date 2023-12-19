package com.bootcamp.ms.transaction.service.impl;

import com.bootcamp.ms.transaction.entity.CreditCard;
import com.bootcamp.ms.transaction.entity.Movement;
import com.bootcamp.ms.transaction.entity.Transaction;
import com.bootcamp.ms.transaction.repository.TransactionRepository;
import com.bootcamp.ms.transaction.service.TransactionService;
import com.bootcamp.ms.transaction.service.WebClientService;
import com.bootcamp.ms.transaction.util.Constant;
import com.bootcamp.ms.transaction.util.DateProcess;
import com.bootcamp.ms.transaction.util.handler.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    public final TransactionRepository repository;

    public final WebClientService webClient;

    @Override
    public Flux<Transaction> getAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Transaction> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return repository.findById(transaction.getId())
                .map(sa -> {
                    throw new BadRequestException(
                            "ID",
                            "Client have one ore more accounts",
                            sa.getId(),
                            TransactionServiceImpl.class,
                            "save.onErrorResume"
                    );
                })
                .switchIfEmpty(Mono.defer(() -> {
                            transaction.setId(null);
                            transaction.setInsertionDate(new Date());
                            return repository.save(transaction);
                        }
                ))
                .onErrorResume(e -> Mono.error(e)).cast(Transaction.class);
    }

    @Override
    public Mono<Transaction> update(Transaction transaction) {

        return repository.findById(transaction.getId())
                .switchIfEmpty(Mono.error(new Exception("An item with the id " + transaction.getId() + " was not found. >> switchIfEmpty")))
                .flatMap(p -> repository.save(transaction))
                .onErrorResume(e -> Mono.error(new BadRequestException(
                        "ID",
                        "An error occurred while trying to update an item.",
                        e.getMessage(),
                        TransactionServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    @Override
    public Mono<Transaction> delete(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new Exception("An item with the id " + id + " was not found. >> switchIfEmpty")))
                .flatMap(p -> {
                    p.setRegistrationStatus(Constant.STATUS_INACTIVE);
                    return repository.save(p);
                })
                .onErrorResume(e -> Mono.error(new BadRequestException(
                        "ID",
                        "An error occurred while trying to delete an item.",
                        e.getMessage(),
                        TransactionServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    @Override
    public Flux<Transaction> getByIdOriginTransaction(String idOriginTransaction) {
        return repository.findByIdOriginTransaction(idOriginTransaction);
    }

    @Override
    public Flux<Transaction> getByIdOriginTransactionAndInsertionDateBetweenAndIsComission(String idOriginTransaction, Date startDate, Date finishDate, Boolean isComission) {
        return repository.findByIdOriginTransactionAndInsertionDateBetweenAndIsComission(idOriginTransaction, startDate, finishDate, isComission);
    }

    @Override
    public Flux<Transaction> getByIdOriginTransactionAndInsertionDateBetween(String idOriginTransaction, Date startDate, Date finishDate) {
        return repository.findByIdOriginTransactionAndInsertionDateBetween(idOriginTransaction, startDate, finishDate);
    }

    @Override
    public Flux<Transaction> getByIdOriginTransactionAndOperationTypeAndInsertionDateBetween(String idOriginTransaction, Short operationType, Date startDate, Date finishDate) {
        return repository.findByIdOriginTransactionAndOperationTypeAndInsertionDateBetween(idOriginTransaction, operationType, startDate, finishDate);
    }

    @Override
    public Mono<Transaction> generateTransactions(Movement movement) {
        Transaction objTransaction = new Transaction(null, movement.getId(), movement.getAmount(),
                movement.getIdDepartureAccount(), new Date(), movement.getIsoCurrencyCode(),
                movement.getOriginMovement(), movement.getDescriptionMovement(), Constant.EXIT, new Date(),
                false, movement.getIsPassive(), "PLHERRERAM", "192.168.1.2", Constant.STATUS_ACTIVE);

        Long transactionsAllowed = 3L;

        return repository.save(objTransaction).flatMap(tr -> {
            String idSaved = tr.getId();
            tr.setId(null);
            tr.setIdOriginTransaction(movement.getIdIncomeAccount());
            tr.setOperationType(Constant.ENTRY);
            return repository.save(tr).map(tre -> {
                tr.setId(idSaved);
                if (movement.getIsPassive()) {
                    generateComission(tre, transactionsAllowed);
                    generateComission(tr, transactionsAllowed);
                }
                return tr;
            });
        });
    }

    @Override
    public Mono<Transaction> generateComission(Transaction transaction, Long transactionsAllowed) {
        BigDecimal comission = new BigDecimal(1);
        return checkComission(transaction.getIdOriginTransaction(), transactionsAllowed).flatMap(re -> {
            if (re) {
                transaction.setIsComission(true);
                transaction.setAmount(comission);
                transaction.setDescriptionMovement(Constant.COMISSION);
                return repository.save(transaction);
            } else {
                return Mono.just(transaction);
            }
        });
    }

    @Override
    public Mono<BigDecimal> getProductBalance(String idOriginTransaction) {
        return getByIdOriginTransaction(idOriginTransaction)
                .map(tr -> tr.getOperationType().equals(Constant.ENTRY) ? tr.getAmount() : tr.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Mono<Boolean> checkComission(String idOriginTransaction, Long transactionsAllowed) {
        if (idOriginTransaction.length() < 24) {
            return Mono.just(false);
        } else {
            return getByIdOriginTransaction(idOriginTransaction).count()
                    .map(c -> c.compareTo(transactionsAllowed) < 0);
        }
    }

    public Mono<BigDecimal> expiredDebtCreditCardV1(String idOriginTransaction) {
        return webClient
                .getWebClient()
                .post()
                .uri("personal/active/credit_card")
                .bodyValue(idOriginTransaction)
                .retrieve()
                .bodyToMono(CreditCard.class).map(cc -> {
                    Calendar today = Calendar.getInstance();
                    Calendar cutDateStart = Calendar.getInstance();
                    Calendar cutDateFinish = Calendar.getInstance();
                    Calendar paymentDate = Calendar.getInstance();

                    cutDateStart.setTime(DateProcess.updateDate(cc.getCutDate(), 0));
                    paymentDate.setTime(DateProcess.updateDate(cc.getPaymentDate(), 1));
                    cutDateFinish.setTime(DateProcess.updateDate(cc.getPaymentDate(), 1));

                    BigDecimal exitAcumulator = new BigDecimal(0);
                    BigDecimal entryAcumulator = new BigDecimal(0);


                    repository.findByIdOriginTransactionAndInsertionDateBetween(idOriginTransaction,
                                    DateProcess.reduceOneMonth(cutDateStart.getTime(), 2),
                                    DateProcess.reduceOneMonth(paymentDate.getTime(), 1))
                            .map(tr -> {
                                if (Short.compare(tr.getOperationType(), Constant.EXIT) == 0 &&
                                        (tr.getTransactionDate().before(cutDateFinish.getTime()))) {
                                    exitAcumulator.add(tr.getAmount()).negate();
                                } else if (Short.compare(tr.getOperationType(), Constant.ENTRY) == 0) {
                                    entryAcumulator.add(tr.getAmount());
                                }
                                return entryAcumulator.add(exitAcumulator);
                            });
                    return entryAcumulator;
                });
    }


    public Mono<BigDecimal> expiredDebtCreditCardV2(String idOriginTransaction) {
        return webClient
                .getWebClient()
                .post()
                .uri("personal/active/credit_card")
                .bodyValue(idOriginTransaction)
                .retrieve()
                .bodyToMono(CreditCard.class).flatMap(cc -> {
                    Calendar today = Calendar.getInstance();
                    Calendar cutDateStart = Calendar.getInstance();
                    Calendar cutDateFinish = Calendar.getInstance();
                    Calendar paymentDate = Calendar.getInstance();

                    cutDateStart.setTime(DateProcess.updateDate(cc.getCutDate(), 0));
                    paymentDate.setTime(DateProcess.updateDate(cc.getPaymentDate(), 1));
                    cutDateFinish.setTime(DateProcess.updateDate(cc.getPaymentDate(), 1));


                    Mono<BigDecimal> entries = repository.findByIdOriginTransactionAndInsertionDateBetween(idOriginTransaction,
                                    DateProcess.reduceOneMonth(cutDateStart.getTime(), 2),
                                    DateProcess.reduceOneMonth(paymentDate.getTime(), 1))
                            .map(tm -> tm.getOperationType().compareTo(Constant.ENTRY) == 0 ? tm.getAmount() :
                                    new BigDecimal(0)).reduce(BigDecimal.ZERO, BigDecimal::add);

                    Mono<BigDecimal> exits = repository.findByIdOriginTransactionAndInsertionDateBetween(idOriginTransaction,
                                    DateProcess.reduceOneMonth(cutDateStart.getTime(), 2),
                                    DateProcess.reduceOneMonth(cutDateFinish.getTime(), 1))
                            .map(tm -> tm.getOperationType().compareTo(Constant.EXIT) == 0 ? tm.getAmount().negate() :
                                    new BigDecimal(0)).reduce(BigDecimal.ZERO, BigDecimal::add);

                    return entries.map(c -> {
                        exits.map(c::add);
                        return c;
                    });
                });
    }

    public Mono<BigDecimal> expiredDebtCredit(String idOriginTransaction) {
        return webClient
                .getWebClient()
                .post()
                .uri("personal/active/credit_card")
                .bodyValue(idOriginTransaction)
                .retrieve()
                .bodyToMono(CreditCard.class).flatMap(cc -> {
                    Integer settingDay = 0;
                    Calendar today = Calendar.getInstance();
                    Calendar paymentDate = Calendar.getInstance();
                    Calendar paymentDatePeriod = Calendar.getInstance();

                    paymentDate.setTime(DateProcess.updateDate(cc.getPaymentDate(), 1));
                    paymentDatePeriod.setTime(DateProcess.updateDate(cc.getPaymentDate(), 0));

                    if (today.before(paymentDate)) {
                        settingDay = 2;
                    } else {
                        settingDay = 1;
                    }

                    return repository.findByIdOriginTransactionAndInsertionDateBetween(idOriginTransaction,
                                    DateProcess.reduceOneMonth(paymentDatePeriod.getTime(), settingDay),
                                    DateProcess.reduceOneMonth(paymentDate.getTime(), settingDay - 1))
                            .map(tm -> tm.getOperationType().compareTo(Constant.ENTRY) == 0 ? tm.getAmount() :
                                    tm.getAmount().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);


                });
    }

    @Override
    public Mono<Boolean> anyDebtExpired(String idOriginTransaction) {
        return expiredDebtCreditCardV2(idOriginTransaction)
                .flatMap(v1 -> {
                    return v1.compareTo(new BigDecimal(0)) > -1 ? Mono.just(false)
                            : expiredDebtCredit(idOriginTransaction)
                            .map(v2 -> v2.compareTo(new BigDecimal(0)) < 0);
                });
    }

    @Override
    public Flux<Transaction> getTop10ByIdOriginTransactionOrderByInsertionDateDesc (String idOriginTransaction) {
        return repository.findTop10ByIdOriginTransactionOrderByInsertionDateDesc(idOriginTransaction)
                .switchIfEmpty(Mono.error(new Exception("Don´t have movements")));
    }
}
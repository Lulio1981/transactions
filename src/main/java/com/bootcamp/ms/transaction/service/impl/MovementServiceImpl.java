package com.bootcamp.ms.transaction.service.impl;

import com.bootcamp.ms.transaction.entity.Movement;
import com.bootcamp.ms.transaction.repository.MovementRepository;
import com.bootcamp.ms.transaction.service.MovementService;
import com.bootcamp.ms.transaction.service.TransactionService;
import com.bootcamp.ms.transaction.util.Constant;
import com.bootcamp.ms.transaction.util.handler.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    public final MovementRepository repository;

    public final TransactionService service;

    private WebClient webClientAccounts;

    @Override
    public Flux<Movement> getAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Movement> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Mono<Movement> save(Movement movement) {
        return repository.findById(movement.getId())
                .map(sa -> {
                    throw new BadRequestException(
                            "ID",
                            "The movement exist",
                            sa.getId(),
                            MovementServiceImpl.class,
                            "save.onErrorResume"
                    );
                })
                .switchIfEmpty(Mono.defer(() -> {
                            movement.setId(null);
                            movement.setInsertionDate(new Date());
                            movement.setRegistrationStatus((short) 1);
                            return admissionMovement(movement);
                        }
                ))
                .onErrorResume(e -> Mono.error(e)).cast(Movement.class);
    }

    @Override
    public Mono<Movement> update(Movement movement) {

        return repository.findById(movement.getId())
                .switchIfEmpty(Mono.error(new Exception("An item with the id " + movement.getId() + " was not found. >> switchIfEmpty")))
                .flatMap(p -> repository.save(movement))
                .onErrorResume(e -> Mono.error(new BadRequestException(
                        "ID",
                        "An error occurred while trying to update an item.",
                        e.getMessage(),
                        MovementServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    @Override
    public Mono<Movement> delete(String id) {
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
                        MovementServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    public Mono<Movement> admissionMovement(Movement movement) {
        return service.getProductBalance(movement.getId()).flatMap(am -> {
            return service.checkComission(movement.getIdDepartureAccount(), 3L).flatMap(re -> {
                if (re == true) {
                    if (am.compareTo(movement.getAmount().add(Constant.AMOUNT_COMISSION)) > -1 &&
                            movement.getIdDepartureAccount().length() == 24) {
                        return repository.save(movement).flatMap(m -> {
                            return service.generateTransactions(m).map(tr -> {
                                return m;
                            });
                        });
                    } else {
                        return Mono.error(new Exception("Insufficient funds"));
                    }
                } else {
                    if (am.compareTo(movement.getAmount()) > 0 && movement.getIdDepartureAccount().length() == 24) {
                        return repository.save(movement).flatMap(m -> {
                            return service.generateTransactions(m).map(tr -> {
                                return m;
                            });
                        });
                    } else if (!(movement.getIdDepartureAccount().length() == 24)) {
                        return repository.save(movement).flatMap(m -> {
                            return service.generateTransactions(m).map(tr -> {
                                return m;
                            });
                        });

                    } else {
                        return Mono.error(new Exception("Insufficient funds"));
                    }
                }
            });
        }).onErrorResume(e -> Mono.error(e)).cast(Movement.class);
    }

}
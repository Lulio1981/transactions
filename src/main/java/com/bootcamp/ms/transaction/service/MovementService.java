package com.bootcamp.ms.transaction.service;

import com.bootcamp.ms.transaction.entity.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface MovementService {

    public Flux<Movement> getAll();

    public Mono<Movement> getById(String id);

    public Mono<Movement> save(Movement movement);

    public Mono<Movement> update(Movement movement);

    public Mono<Movement> delete(String id);
}
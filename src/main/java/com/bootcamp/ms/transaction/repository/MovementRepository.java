package com.bootcamp.ms.transaction.repository;

import com.bootcamp.ms.transaction.entity.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MovementRepository extends ReactiveMongoRepository<Movement, String> {
}
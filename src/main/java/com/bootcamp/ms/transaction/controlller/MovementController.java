package com.bootcamp.ms.transaction.controlller;


import com.bootcamp.ms.transaction.entity.Movement;
import com.bootcamp.ms.transaction.service.MovementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("transactions/movement")
@Tag(name = "Movement", description = "Manage movements origin to generate the transactions")
@CrossOrigin(value = {"*"})
@RequiredArgsConstructor
public class MovementController {

    public final MovementService service;

    @GetMapping//(value = "/fully")
    public Mono<ResponseEntity<Flux<Movement>>> getAll() {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.getAll())
        );
    }

    @PostMapping
    public Mono<ResponseEntity<Movement>> create(@RequestBody Movement movement) {

        return service.save(movement).map(p -> ResponseEntity
                .created(URI.create("/Movement/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(p)
        );
    }

    @PutMapping
    public Mono<ResponseEntity<Movement>> update(@RequestBody Movement movement) {
        return service.update(movement)
                .map(p -> ResponseEntity.created(URI.create("/Movement/"
                                .concat(p.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public Mono<ResponseEntity<Movement>> delete(@RequestBody String id) {
        return service.delete(id)
                .map(p -> ResponseEntity.created(URI.create("/Movement/"
                                .concat(p.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}

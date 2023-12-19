package com.bootcamp.ms.transaction.controlller;

import com.bootcamp.ms.transaction.entity.Transaction;
import com.bootcamp.ms.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("transactions")
@Tag(name = "Transactions", description = "Manage transactions generate in each operation")
@CrossOrigin(value = {"*"})
@RequiredArgsConstructor
public class TransactionController {

    public final TransactionService service;

    @GetMapping//(value = "/fully")
    public Mono<ResponseEntity<Flux<Transaction>>> getAll() {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.getAll())
        );
    }

    @PostMapping
    public Mono<ResponseEntity<Transaction>> create(@RequestBody Transaction transaction) {

        return service.save(transaction).map(p -> ResponseEntity
                .created(URI.create("/Transaction/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(p)
        );
    }

    @PutMapping
    public Mono<ResponseEntity<Transaction>> update(@RequestBody Transaction transaction) {
        return service.update(transaction)
                .map(p -> ResponseEntity.created(URI.create("/Transaction/"
                                .concat(p.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public Mono<ResponseEntity<Transaction>> delete(@RequestBody String id) {
        return service.delete(id)
                .map(p -> ResponseEntity.created(URI.create("/Transaction/"
                                .concat(p.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/debtExpired/{idOriginTransaction}")
    public Mono<ResponseEntity<Mono<Boolean>>> anyDebtExpired(@PathVariable String idOriginTransaction) {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.anyDebtExpired(idOriginTransaction))
        );
    }

    @GetMapping("/{idOriginTransaction}")
    public Mono<ResponseEntity<Mono<BigDecimal>>> getProductBalance(@PathVariable String idOriginTransaction) {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.getProductBalance(idOriginTransaction))
        );
    }

    @GetMapping("/top10/{idOriginTransaction}")
    public Mono<ResponseEntity<Flux<Transaction>>> getTo10Movement(@PathVariable String idOriginTransaction) {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.getTop10ByIdOriginTransactionOrderByInsertionDateDesc(idOriginTransaction))
        );
    }

}

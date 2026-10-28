package com.wetech.demo.web3j.controller;

import com.wetech.demo.web3j.service.EscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/escrow")
@RequiredArgsConstructor
public class EscrowController {

    private final EscrowService escrowService;

    /**
     * Deploy a new Escrow contract. The backend account is the arbiter.
     */
    @PostMapping("/deploy")
    public CompletableFuture<ResponseEntity<Map<String, String>>> deploy() {
        return escrowService.deploy()
                .thenApply(contract -> {
                    String transactionHash = contract.getTransactionReceipt()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Deployment completed without a transaction receipt"))
                            .getTransactionHash();

                    return ResponseEntity.ok(Map.of(
                            "contractAddress", escrowService.getContractAddress(),
                            "transactionHash", transactionHash,
                            "deployBlock", escrowService.getDeployBlock().toString()
                    ));
                });
    }

    /**
     * Load an existing Escrow contract and its real deployment block.
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, String>> load(
            @RequestParam String address,
            @RequestParam BigInteger fromBlock
    ) {
        escrowService.load(address, fromBlock);
        return ResponseEntity.ok(Map.of(
                "contractAddress", escrowService.getContractAddress()
        ));
    }

    /**
     * Return the loaded contract address and on-chain arbiter address.
     */
    @GetMapping("/address")
    public CompletableFuture<ResponseEntity<Map<String, String>>> getAddress() {
        return escrowService.getArbiter()
                .thenApply(arbiter -> ResponseEntity.ok(Map.of(
                        "contractAddress", escrowService.getContractAddress(),
                        "arbiter", arbiter
                )));
    }

    /**
     * Read one order and return its current, human-readable status.
     */
    @GetMapping("/orders/{id}")
    public CompletableFuture<ResponseEntity<Map<String, String>>> getOrder(
            @PathVariable BigInteger id
    ) {
        return escrowService.getOrder(id)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Rebuild all order IDs from OrderCreated events.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, String>>> listOrders() throws Exception {
        return ResponseEntity.ok(escrowService.listOrders());
    }

    /**
     * Resolve a disputed order with the backend account as arbiter.
     */
    @PostMapping("/orders/{id}/resolve")
    public CompletableFuture<ResponseEntity<Map<String, String>>> resolve(
            @PathVariable BigInteger id,
            @RequestParam boolean sellerWins
    ) {
        return escrowService.resolve(id, sellerWins)
                .thenCompose(receipt -> escrowService.getOrder(id)
                        .thenApply(order -> ResponseEntity.ok(Map.of(
                                "transactionHash", receipt.getTransactionHash(),
                                "blockNumber", receipt.getBlockNumber().toString(),
                                "status", order.get("status")
                        ))));
    }
}

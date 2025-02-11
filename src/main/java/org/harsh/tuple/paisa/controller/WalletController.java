package org.harsh.tuple.paisa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harsh.tuple.paisa.model.Transaction;
import org.harsh.tuple.paisa.repository.UserRepository;
import org.harsh.tuple.paisa.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallet")
@Slf4j
public class WalletController {


    private final WalletService walletService;
    private final UserRepository userRepository;


    // Wallet Recharge
    @PostMapping("/recharge")
    public ResponseEntity<?> rechargeWallet(@RequestParam double amount) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Transaction transaction = walletService.rechargeWallet(userId, amount);
        return ResponseEntity.ok(transaction);
    }

    // Wallet Transfer
    @PostMapping("/transfer")
    public ResponseEntity<?> transferWallet(
            @RequestParam String recipientUsername,
            @RequestParam double amount) {

        log.info("Received request to transfer wallet from {} to {}", recipientUsername, amount);
        String recipientId = userRepository.findByUsername(recipientUsername).get().getId();
        //? Extracting senderId from JWT token
        String senderId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Transaction> transactions = walletService.transferWallet(senderId, recipientId, amount);
        return ResponseEntity.ok(transactions);

    }

    // View Account Statement
    @GetMapping("/statement")
    public List<Object> getCombinedHistory(int page, int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return walletService.getCombinedHistory(userId, page, size);
    }



    @GetMapping("/balance")
    public Map<String, Double> getWalletBalance() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        double balance = walletService.getBalance(userId);
        return Map.of("balance", balance);
    }
}

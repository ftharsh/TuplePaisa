package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harsh.tuple.paisa.dto.EmailDetails;
import org.harsh.tuple.paisa.exception.InsufficientBalanceException;
import org.harsh.tuple.paisa.exception.InvalidTransactionAmountException;
import org.harsh.tuple.paisa.exception.WalletNotFoundException;
import org.harsh.tuple.paisa.model.*;
import org.harsh.tuple.paisa.repository.CashbackRepository;
import org.harsh.tuple.paisa.repository.TransactionRepository;
import org.harsh.tuple.paisa.repository.UserRepository;
import org.harsh.tuple.paisa.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CashbackService cashbackService;
    //    private final EmailService emailService;
    private final UserRepository userRepository;
    private final CashbackRepository cashbackRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    LocalDateTime now = LocalDateTime.now();
    private final Map<String, List<Object>> userHistoryMap = new ConcurrentHashMap<>();

    @Transactional
    public Transaction rechargeWallet(String userId, double amount) {
        if (amount <= 0) {
            throw new InvalidTransactionAmountException(amount);
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        // ?Update balance
        wallet.setBalance(wallet.getBalance() + amount);

        // ?Save wallet with version cntrl
        walletRepository.save(wallet);


        // ?Record transaction
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .recipientId("self")
                .walletId(wallet.getId())
                .type(TransactionType.RECHARGE)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);

        // ?Apply cshbck
        cashbackService.applyCashback(userId, amount);
        //?Snd email
        sendEmail(userId, amount);

        return transaction;
    }


    @Transactional
    public List<Transaction> transferWallet(String senderId, String recipientId, double amount) {
        if (amount <= 0) {
            throw new InvalidTransactionAmountException(amount);
        }

        Wallet senderWallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new WalletNotFoundException(senderId));
        Wallet recipientWallet = walletRepository.findByUserId(recipientId)
                .orElseThrow(() -> new WalletNotFoundException(recipientId));

        if (senderWallet.getBalance() < amount) {
            throw new InsufficientBalanceException(senderId, senderWallet.getBalance(), amount);
        }

        // ?Update balances
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        recipientWallet.setBalance(recipientWallet.getBalance() + amount);

        // Save wallets with version control
        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);


        //? Recording trnsaction for both sndr and receiver
        Transaction senderTransaction = Transaction.builder()
                .userId(senderId)
                .senderId(null)
                .walletId(senderWallet.getId())
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .recipientId(recipientId)
                .timestamp(LocalDateTime.now())
                .build();

        Transaction recipientTransaction = Transaction.builder()
                .userId(recipientId)
                .senderId(senderId)
                .walletId(recipientWallet.getId())
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .recipientId(null)
                .timestamp(LocalDateTime.now())
                .build();

        transactionRepository.save(senderTransaction);
        transactionRepository.save(recipientTransaction);

        sendEmail(recipientId, amount);
        return List.of(senderTransaction, recipientTransaction);
    }

    public double getBalance(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
        return wallet.getBalance();
    }

    public List<Object> getCombinedHistory(String userId, int page, int size) {
        List<Object> combinedList = new ArrayList<>();


        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageable);
        List<Transaction> transactionList = transactions.getContent();
        Page<Cashback> cashbacks = cashbackRepository.findByUserId(userId, pageable);
        List<Cashback> cashbackList = cashbacks.getContent();


        combinedList.addAll(transactionList);
        combinedList.addAll(cashbackList);


        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction)
                    ? ((Transaction) a).getTimestamp()
                    : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction)
                    ? ((Transaction) b).getTimestamp()
                    : ((Cashback) b).getTimestamp();


            int timeComparison = time2.compareTo(time1);
            if (timeComparison != 0) {
                return timeComparison; // Use timestamp comparison if timestamps differ
            }


            if (a instanceof Cashback && b instanceof Transaction) {
                return -1;
            } else if (a instanceof Transaction && b instanceof Cashback) {
                return 1;
            }

            return 0;
        });

        return combinedList;
    }


    public void addHistory(String userId, List<Map<String, Object>> history) {
        log.info("adding history");

        userHistoryMap.computeIfAbsent(userId, _ -> new ArrayList<>()).addAll(history);

        log.info("history added calling get history");
        getHistory(userId);
    }

    public List<Object> getHistory(String userId) {
        log.info("getting history and returning list");
        return new ArrayList<>(userHistoryMap.getOrDefault(userId, Collections.emptyList()));
    }




    //!with kafka
    public void sendEmail(String receiverId, double amount) {
        userRepository.findById(receiverId).ifPresent(user -> {
            String emailMessage = String.format(
                    "Hello %s,\n\nYou have successfully received %.2f in your Account.\nThank you for using our service.\n -Tuple Paisa",
                    user.getUsername(), amount);
            EmailDetails emailDetails = new EmailDetails(
                    user.getEmail(),
                    emailMessage,
                    "Transaction Successful"

            );
            try {
                kafkaTemplate.send("email-notifications", objectMapper.writeValueAsString(emailDetails));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
//        * avg time --> 30.498 [83.48]
    }

}


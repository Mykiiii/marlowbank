package com.marlow.marlowbank.controller;

import com.marlow.marlowbank.entity.Account;
import com.marlow.marlowbank.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<String> deposit(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        logger.info("Deposit request for account: {}, amount: {}", accountNumber, amount);
        try {
            BigDecimal balance = accountService.deposit(accountNumber, amount);
            logger.info("Deposit successful for account: {}. New balance: {}", accountNumber, balance);
            return ResponseEntity.ok("Deposit successful! New balance: " + balance);
        } catch (Exception e) {
            logger.error("Error during deposit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<String> withdraw(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        logger.info("Withdrawal request for account: {}, amount: {}", accountNumber, amount);
        try {
            BigDecimal balance = accountService.withdraw(accountNumber, amount);
            logger.info("Withdrawal successful for account: {}. New balance: {}", accountNumber, balance);
            return ResponseEntity.ok("Withdrawal successful! New balance: " + balance);
        } catch (Exception e) {
            logger.error("Error during withdrawal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(@RequestParam String accountNumber, @RequestParam String name, @RequestParam String balance) {
        return ResponseEntity.ok(accountService.createAccount(accountNumber, name, balance));
    }
}

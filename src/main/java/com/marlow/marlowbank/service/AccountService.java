package com.marlow.marlowbank.service;

import com.marlow.marlowbank.entity.Account;
import com.marlow.marlowbank.entity.Transaction;
import com.marlow.marlowbank.entity.TransactionType;
import com.marlow.marlowbank.repository.AccountRepository;
import com.marlow.marlowbank.repository.TransactionRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import org.apache.catalina.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class AccountService {

    @Value("${account.withdrawal.limit}")
    private BigDecimal withdrawalLimit;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Transactional
    public BigDecimal deposit(String accountNumber, BigDecimal amount) {
        validateAccountNumberEmptyAndBalanceEmptiness(accountNumber, amount);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        kafkaProducerService.sendMessage("Deposit: " + amount + " to account: " + accountNumber);
        return account.getBalance();
    }

    private static void validateAccountNumberEmptyAndBalanceEmptiness(String accountNumber, BigDecimal amount) {
        if (StringUtils.isBlank(accountNumber) || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid account number or amount");
        }
    }

    @Transactional
    public BigDecimal withdraw(String accountNumber, BigDecimal amount) {
        validateAccountNumberEmptyAndBalanceEmptiness(accountNumber, amount);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (amount.compareTo(withdrawalLimit) > 0) {
            throw new RuntimeException("Withdrawal amount exceeds the limit of " + withdrawalLimit);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        kafkaProducerService.sendMessage("Withdraw: " + amount + " from account: " + accountNumber);
        return account.getBalance();
    }

    @Transactional
    public Account createAccount(String accountNumber, String name, String balanceString) {
        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account already exists");
        }
        BigDecimal balance = StringUtils.isNotBlank(balanceString) ? BigDecimal.valueOf(Long.parseLong(balanceString)) : BigDecimal.ZERO;
        Account account = new Account(accountNumber, name, balance);
        return accountRepository.save(account);
    }
}

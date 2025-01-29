package com.marlow.marlowbank.service;

import com.marlow.marlowbank.entity.ChangeLog;
import com.marlow.marlowbank.repository.ChangeLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private ChangeLogRepository changeLogRepository;

    @KafkaListener(topics = "${kafka.topic.change-log}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        logger.info("Received Kafka message: {}", message);

        // Extract data from message using regex
        Pattern pattern = Pattern.compile("(Deposit|Withdraw): (\\d+.?\\d*) to account: (\\d+)");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String type = matcher.group(1);
            BigDecimal amount = new BigDecimal(matcher.group(2));
            String accountNumber = matcher.group(3);

            // Save to change log (audit table)
            ChangeLog changeLog = new ChangeLog(accountNumber, type, amount);
            changeLogRepository.save(changeLog);
        }
    }
}

package ru.hvayon.StatisticsService.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.hvayon.StatisticsService.model.LogMessage;
import ru.hvayon.StatisticsService.service.StatisticsService;
import ru.hvayon.StatisticsService.util.LogMessageMapper;

@Slf4j
@Component
public class KafkaConsumer {

    private final StatisticsService service;

    @Autowired
    public KafkaConsumer(StatisticsService service) {
        this.service = service;
    }

    @KafkaListener(topics = "statistics", groupId = "statistics")
    public void listen(String message) throws JsonProcessingException {
        LogMessage logMessage = LogMessageMapper.convertMessage(message);
        System.out.println("[STATISTICS]: got from topic: " + logMessage);
        service.process(logMessage);
    }
}

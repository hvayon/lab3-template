package ru.hvayon.Stats.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import ru.hvayon.Stats.model.LogMessage;
import ru.hvayon.Stats.service.StatsService;
import ru.hvayon.Stats.util.LogMessageMapper;

public class KafkaConsumer {

    private final StatsService service;

    @Autowired
    public KafkaConsumer(StatsService service) {
        this.service = service;
    }

    @KafkaListener(topics = "stats-topic", groupId = "statistics")
    public void listen(String message) throws JsonProcessingException {
        LogMessage logMessage = LogMessageMapper.convertMessage(message);
        System.out.println("[STATISTICS]: received from stats-topic: " + logMessage);
        service.process(logMessage);
    }
}

package ru.hvayon.Gateway.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyKafkaProducer {
    @Value(value = "stats-topic")
    private String topic;

    private final StreamBridge streamBridge;

    public MyKafkaProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void send(LogMessage message) {
        try {
            streamBridge.send(topic, message);
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }
}

package ru.hvayon.Stats.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.hvayon.Stats.model.LogMessage;
import ru.hvayon.Stats.repository.StatsRepository;

import java.util.List;

@Service
public class StatsService {
    private final StatsRepository statsRepository;

    @Autowired
    public StatsService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public void process(LogMessage msg) {
        statsRepository.save(msg);
    }

    public List<LogMessage> select() {
        return statsRepository.findAll();
    }
}

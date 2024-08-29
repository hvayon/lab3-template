package ru.hvayon.StatisticsService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.hvayon.StatisticsService.model.LogMessage;
import ru.hvayon.StatisticsService.repository.StatisticsRepository;

import java.util.List;

@Service
public class StatisticsService {
    private final StatisticsRepository repo;

    @Autowired
    public StatisticsService(StatisticsRepository repo) {
        this.repo = repo;
    }
    
    public void process(LogMessage msg) {
        repo.save(msg);
    }

    public List<LogMessage> select() {
        return repo.findAll();
    }
}

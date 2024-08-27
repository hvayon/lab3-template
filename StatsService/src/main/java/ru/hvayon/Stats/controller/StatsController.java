package ru.hvayon.Stats.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hvayon.Stats.model.LogMessage;
import ru.hvayon.Stats.service.StatsService;

import java.util.List;

@RestController
@RequestMapping("api")
public class StatsController {

    @Autowired
    private StatsService statsService;
    @RequestMapping("/v1/stats")
    public List<LogMessage> stats() {
        return statsService.select();
    }

}

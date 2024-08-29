package ru.hvayon.StatisticsService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor(staticName = "build")
@NoArgsConstructor
@Data
public class StatsResponse {
    List<LogMessage> logMessages;
}

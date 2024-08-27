package ru.hvayon.Stats.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hvayon.Stats.model.LogMessage;

import java.util.List;

@AllArgsConstructor(staticName = "build")
@NoArgsConstructor
@Data
public class StatsResponse {
    List<LogMessage> logMessages;
}

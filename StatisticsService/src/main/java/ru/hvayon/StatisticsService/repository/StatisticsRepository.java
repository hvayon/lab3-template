package ru.hvayon.StatisticsService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hvayon.StatisticsService.model.LogMessage;

@Repository
public interface StatisticsRepository extends JpaRepository<LogMessage, Integer> { }

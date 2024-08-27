package ru.hvayon.Stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hvayon.Stats.model.LogMessage;

@Repository
public interface StatsRepository extends JpaRepository<LogMessage, Integer> { }


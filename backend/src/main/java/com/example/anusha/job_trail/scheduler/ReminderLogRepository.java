package com.example.anusha.job_trail.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, UUID> {

    boolean existsByApplicationIdAndReminderDate(UUID applicationId, LocalDate reminderDate);
}

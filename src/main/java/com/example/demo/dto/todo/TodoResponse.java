package com.example.demo.dto.todo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse(
	Long id,
	String title,
	String description,
	LocalDate dueDate,
	String priority,
	boolean completed,
	Integer reminderDays,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
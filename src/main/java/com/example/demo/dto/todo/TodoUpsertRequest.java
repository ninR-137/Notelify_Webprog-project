package com.example.demo.dto.todo;

public record TodoUpsertRequest(
	String title,
	String description,
	String dueDate,
	String priority,
	Integer reminderDays,
	Boolean completed
) {
}
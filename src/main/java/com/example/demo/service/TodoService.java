package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.todo.TodoResponse;
import com.example.demo.dto.todo.TodoUpsertRequest;
import com.example.demo.entity.TodoEntity;
import com.example.demo.repository.TodoRepository;

@Service
public class TodoService {

	private final TodoRepository todoRepository;

	public TodoService(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
	}

	public List<TodoResponse> listTodos(String ownerEmail) {
		return todoRepository.findByOwnerEmailOrderByCreatedAtDesc(ownerEmail)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	public TodoResponse create(String ownerEmail, TodoUpsertRequest request) {
		TodoEntity todo = new TodoEntity();
		todo.setOwnerEmail(ownerEmail);
		LocalDateTime now = LocalDateTime.now();
		todo.setCreatedAt(now);
		todo.setUpdatedAt(now);
		applyUpsert(todo, request);
		todo.setCompleted(request != null && request.completed() != null && request.completed());
		return toResponse(todoRepository.save(todo));
	}

	public TodoResponse update(String ownerEmail, Long id, TodoUpsertRequest request) {
		TodoEntity todo = requireOwnerTodo(ownerEmail, id);
		applyUpsert(todo, request);
		if (request != null && request.completed() != null) {
			todo.setCompleted(request.completed());
		}
		todo.setUpdatedAt(LocalDateTime.now());
		return toResponse(todoRepository.save(todo));
	}

	public TodoResponse setCompleted(String ownerEmail, Long id, boolean completed) {
		TodoEntity todo = requireOwnerTodo(ownerEmail, id);
		todo.setCompleted(completed);
		todo.setUpdatedAt(LocalDateTime.now());
		return toResponse(todoRepository.save(todo));
	}

	public void delete(String ownerEmail, Long id) {
		TodoEntity todo = requireOwnerTodo(ownerEmail, id);
		todoRepository.delete(todo);
	}

	private void applyUpsert(TodoEntity todo, TodoUpsertRequest request) {
		String title = request == null || isBlank(request.title()) ? "Untitled task" : request.title().trim();
		todo.setTitle(title);
		todo.setDescription(request == null ? null : nullIfBlank(request.description()));
		todo.setPriority(normalizePriority(request == null ? null : request.priority()));
		todo.setDueDate(parseDate(request == null ? null : request.dueDate()));
		todo.setReminderMinutes(toMinutes(request == null ? null : request.reminderDays()));
	}

	private TodoEntity requireOwnerTodo(String ownerEmail, Long id) {
		TodoEntity todo = todoRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Task not found."));
		if (!ownerEmail.equalsIgnoreCase(todo.getOwnerEmail())) {
			throw new IllegalArgumentException("Task not found.");
		}
		return todo;
	}

	private TodoResponse toResponse(TodoEntity todo) {
		return new TodoResponse(
			todo.getId(),
			todo.getTitle(),
			todo.getDescription(),
			todo.getDueDate(),
			todo.getPriority(),
			todo.isCompleted(),
			toDays(todo.getReminderMinutes()),
			todo.getCreatedAt(),
			todo.getUpdatedAt()
		);
	}

	private Integer toMinutes(Integer reminderDays) {
		if (reminderDays == null || reminderDays <= 0) {
			return 0;
		}
		return reminderDays * 24 * 60;
	}

	private Integer toDays(Integer reminderMinutes) {
		if (reminderMinutes == null || reminderMinutes <= 0) {
			return 0;
		}
		return Math.max(1, reminderMinutes / (24 * 60));
	}

	private String normalizePriority(String priority) {
		if (priority == null) {
			return "medium";
		}
		String safe = priority.trim().toLowerCase();
		if (!"high".equals(safe) && !"medium".equals(safe) && !"low".equals(safe)) {
			return "medium";
		}
		return safe;
	}

	private LocalDate parseDate(String dueDate) {
		if (isBlank(dueDate)) {
			return null;
		}
		try {
			return LocalDate.parse(dueDate);
		} catch (Exception ex) {
			return null;
		}
	}

	private String nullIfBlank(String value) {
		if (isBlank(value)) {
			return null;
		}
		return value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.todo.TodoFlagRequest;
import com.example.demo.dto.todo.TodoResponse;
import com.example.demo.dto.todo.TodoUpsertRequest;
import com.example.demo.service.TodoService;

@RestController
@RequestMapping("/api/todos")
public class TodoApiController {

	private final TodoService todoService;

	public TodoApiController(TodoService todoService) {
		this.todoService = todoService;
	}

	@GetMapping
	public ResponseEntity<List<TodoResponse>> list(Authentication authentication) {
		return ResponseEntity.ok(todoService.listTodos(authentication.getName()));
	}

	@PostMapping
	public ResponseEntity<TodoResponse> create(Authentication authentication, @RequestBody TodoUpsertRequest request) {
		return ResponseEntity.ok(todoService.create(authentication.getName(), request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TodoResponse> update(
		Authentication authentication,
		@PathVariable Long id,
		@RequestBody TodoUpsertRequest request
	) {
		return ResponseEntity.ok(todoService.update(authentication.getName(), id, request));
	}

	@PatchMapping("/{id}/completed")
	public ResponseEntity<TodoResponse> completed(
		Authentication authentication,
		@PathVariable Long id,
		@RequestBody TodoFlagRequest request
	) {
		return ResponseEntity.ok(todoService.setCompleted(authentication.getName(), id, request.value()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remove(Authentication authentication, @PathVariable Long id) {
		todoService.delete(authentication.getName(), id);
		return ResponseEntity.noContent().build();
	}
}
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.note.NoteFlagRequest;
import com.example.demo.dto.note.NoteResponse;
import com.example.demo.dto.note.NoteUpsertRequest;
import com.example.demo.service.NoteService;

@RestController
@RequestMapping("/api/notes")
public class NoteApiController {

	private final NoteService noteService;

	public NoteApiController(NoteService noteService) {
		this.noteService = noteService;
	}

	@GetMapping
	public ResponseEntity<List<NoteResponse>> list(
		Authentication authentication,
		@RequestParam(defaultValue = "all") String view,
		@RequestParam(defaultValue = "") String search,
		@RequestParam(defaultValue = "recent") String sort
	) {
		return ResponseEntity.ok(noteService.listNotes(authentication.getName(), view, search, sort));
	}

	@PostMapping
	public ResponseEntity<NoteResponse> create(Authentication authentication, @RequestBody NoteUpsertRequest request) {
		return ResponseEntity.ok(noteService.create(authentication.getName(), request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<NoteResponse> update(
		Authentication authentication,
		@PathVariable Long id,
		@RequestBody NoteUpsertRequest request
	) {
		return ResponseEntity.ok(noteService.update(authentication.getName(), id, request));
	}

	@PatchMapping("/{id}/favorite")
	public ResponseEntity<NoteResponse> favorite(
		Authentication authentication,
		@PathVariable Long id,
		@RequestBody NoteFlagRequest request
	) {
		return ResponseEntity.ok(noteService.setFavorited(authentication.getName(), id, request.value()));
	}

	@PatchMapping("/{id}/deleted")
	public ResponseEntity<NoteResponse> deleted(
		Authentication authentication,
		@PathVariable Long id,
		@RequestBody NoteFlagRequest request
	) {
		return ResponseEntity.ok(noteService.setDeleted(authentication.getName(), id, request.value()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remove(Authentication authentication, @PathVariable Long id) {
		noteService.deletePermanently(authentication.getName(), id);
		return ResponseEntity.noContent().build();
	}
}
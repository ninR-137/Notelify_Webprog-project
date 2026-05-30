package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.demo.dto.note.NoteResponse;
import com.example.demo.dto.note.NoteUpsertRequest;
import com.example.demo.entity.NoteEntity;
import com.example.demo.repository.NoteRepository;

@Service
public class NoteService {

	private final NoteRepository noteRepository;

	public NoteService(NoteRepository noteRepository) {
		this.noteRepository = noteRepository;
	}

	public List<NoteResponse> listNotes(String ownerEmail, String view, String search, String sort) {
		List<NoteEntity> notes = noteRepository.findByOwnerEmailOrderByUpdatedAtDesc(ownerEmail);
		String safeView = view == null ? "all" : view;
		String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

		List<NoteEntity> filtered = notes.stream()
			.filter(note -> matchesView(note, safeView))
			.filter(note -> matchesSearch(note, query))
			.toList();

		List<NoteEntity> sorted = new ArrayList<>(filtered);
		applySort(sorted, sort);
		return sorted.stream().map(this::toResponse).toList();
	}

	public NoteResponse create(String ownerEmail, NoteUpsertRequest request) {
		NoteEntity note = new NoteEntity();
		note.setOwnerEmail(ownerEmail);
		LocalDateTime now = LocalDateTime.now();
		note.setCreatedAt(now);
		note.setUpdatedAt(now);
		applyUpsert(note, request);
		note.setDeleted(false);
		return toResponse(noteRepository.save(note));
	}

	public NoteResponse update(String ownerEmail, Long id, NoteUpsertRequest request) {
		NoteEntity note = requireOwnerNote(ownerEmail, id);
		applyUpsert(note, request);
		note.setUpdatedAt(LocalDateTime.now());
		return toResponse(noteRepository.save(note));
	}

	public NoteResponse setFavorited(String ownerEmail, Long id, boolean favorited) {
		NoteEntity note = requireOwnerNote(ownerEmail, id);
		note.setFavorited(favorited);
		note.setUpdatedAt(LocalDateTime.now());
		return toResponse(noteRepository.save(note));
	}

	public NoteResponse setDeleted(String ownerEmail, Long id, boolean deleted) {
		NoteEntity note = requireOwnerNote(ownerEmail, id);
		note.setDeleted(deleted);
		note.setUpdatedAt(LocalDateTime.now());
		return toResponse(noteRepository.save(note));
	}

	public void deletePermanently(String ownerEmail, Long id) {
		NoteEntity note = requireOwnerNote(ownerEmail, id);
		noteRepository.delete(note);
	}

	private void applyUpsert(NoteEntity note, NoteUpsertRequest request) {
		String title = request == null || isBlank(request.title()) ? "Untitled" : request.title().trim();
		String content = request == null || request.content() == null ? "" : request.content();
		String category = request == null || isBlank(request.category()) ? "Personal" : request.category().trim();
		List<String> tags = request == null || request.tags() == null
			? new ArrayList<>()
			: request.tags().stream()
				.filter(tag -> !isBlank(tag))
				.map(String::trim)
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));

		note.setTitle(title);
		note.setContent(content);
		note.setCategory(category);
		note.setTags(tags);
		note.setFavorited(request != null && request.favorited());
	}

	private boolean matchesView(NoteEntity note, String view) {
		return switch (view) {
			case "favorites" -> note.isFavorited() && !note.isDeleted();
			case "trash" -> note.isDeleted();
			default -> !note.isDeleted();
		};
	}

	private boolean matchesSearch(NoteEntity note, String query) {
		if (query.isBlank()) {
			return true;
		}
		if (note.getTitle().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		if (note.getContent().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		return note.getCategory().toLowerCase(Locale.ROOT).contains(query);
	}

	private void applySort(List<NoteEntity> notes, String sort) {
		String safeSort = sort == null ? "recent" : sort;
		if ("alpha".equals(safeSort)) {
			notes.sort(Comparator.comparing(NoteEntity::getTitle, String.CASE_INSENSITIVE_ORDER));
			return;
		}
		if ("oldest".equals(safeSort)) {
			notes.sort(Comparator.comparing(NoteEntity::getUpdatedAt));
			return;
		}
		notes.sort(Comparator.comparing(NoteEntity::getUpdatedAt).reversed());
	}

	private NoteEntity requireOwnerNote(String ownerEmail, Long id) {
		NoteEntity note = noteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Note not found."));
		if (!ownerEmail.equalsIgnoreCase(note.getOwnerEmail())) {
			throw new IllegalArgumentException("Note not found.");
		}
		return note;
	}

	private NoteResponse toResponse(NoteEntity note) {
		return new NoteResponse(
			note.getId(),
			note.getTitle(),
			note.getContent(),
			note.getCategory(),
			note.getTags(),
			note.isFavorited(),
			note.isDeleted(),
			note.getCreatedAt(),
			note.getUpdatedAt()
		);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
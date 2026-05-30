package com.example.demo.dto.note;

import java.time.LocalDateTime;
import java.util.List;

public record NoteResponse(
	Long id,
	String title,
	String content,
	String category,
	List<String> tags,
	boolean favorited,
	boolean deleted,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
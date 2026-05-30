package com.example.demo.dto.note;

import java.util.List;

public record NoteUpsertRequest(
	String title,
	String content,
	String category,
	List<String> tags,
	boolean favorited
) {
}
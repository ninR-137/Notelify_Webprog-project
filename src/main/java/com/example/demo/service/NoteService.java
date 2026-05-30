package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
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

	public byte[] exportPdf(String ownerEmail, Long id) {
		NoteEntity note = requireOwnerNote(ownerEmail, id);
		String html = buildPdfHtml(note);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(html, null);
			builder.toStream(output);
			builder.run();
			return output.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("Could not export note as PDF.", ex);
		}
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

	private String buildPdfHtml(NoteEntity note) {
		String title = escapeHtml(note.getTitle());
		String category = escapeHtml(note.getCategory());
		String updated = note.getUpdatedAt() == null ? "" : note.getUpdatedAt().toString();
		String contentHtml = renderMarkdown(note.getContent());
		return """
			<?xml version=\"1.0\" encoding=\"UTF-8\"?>
			<html xmlns=\"http://www.w3.org/1999/xhtml\">
			<head>
			<meta charset=\"UTF-8\" />
			<style>
			  @page { size: A4; margin: 24mm 18mm; }
			  body { font-family: Arial, sans-serif; color: #1f2937; line-height: 1.5; font-size: 12pt; }
			  .title { font-size: 22pt; font-weight: 700; margin: 0 0 6px; }
			  .meta { color: #6b7280; font-size: 10pt; margin: 0 0 18px; }
			  .badge { display: inline-block; background: #eef2ff; color: #4338ca; border-radius: 999px; padding: 3px 10px; font-size: 9pt; font-weight: 700; margin-right: 8px; }
			  h4 { font-size: 14pt; margin: 14px 0 6px; }
			  p { margin: 0 0 8px; }
			  ul, ol { margin: 0 0 10px; padding-left: 22px; }
			  li { margin: 2px 0; }
			  a { color: #1d4ed8; text-decoration: underline; }
			</style>
			</head>
			<body>
			  <h1 class=\"title\">%s</h1>
			  <p class=\"meta\"><span class=\"badge\">%s</span>Updated %s</p>
			  %s
			</body>
			</html>
			""".formatted(title, category, escapeHtml(updated), contentHtml);
	}

	private String renderMarkdown(String value) {
		String[] lines = value == null ? new String[0] : value.replace("\r\n", "\n").split("\n", -1);
		StringBuilder out = new StringBuilder();
		boolean inUl = false;
		boolean inOl = false;

		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				if (inUl) {
					out.append("</ul>");
					inUl = false;
				}
				if (inOl) {
					out.append("</ol>");
					inOl = false;
				}
				out.append("<p><br/></p>");
				continue;
			}

			Matcher heading = Pattern.compile("^#\\s+(.+)$").matcher(trimmed);
			if (heading.find()) {
				if (inUl) {
					out.append("</ul>");
					inUl = false;
				}
				if (inOl) {
					out.append("</ol>");
					inOl = false;
				}
				out.append("<h4>").append(renderInline(heading.group(1))).append("</h4>");
				continue;
			}

			Matcher bullet = Pattern.compile("^[-*]\\s+(.+)$").matcher(trimmed);
			if (bullet.find()) {
				if (inOl) {
					out.append("</ol>");
					inOl = false;
				}
				if (!inUl) {
					out.append("<ul>");
					inUl = true;
				}
				out.append("<li>").append(renderInline(bullet.group(1))).append("</li>");
				continue;
			}

			Matcher number = Pattern.compile("^\\d+\\.\\s+(.+)$").matcher(trimmed);
			if (number.find()) {
				if (inUl) {
					out.append("</ul>");
					inUl = false;
				}
				if (!inOl) {
					out.append("<ol>");
					inOl = true;
				}
				out.append("<li>").append(renderInline(number.group(1))).append("</li>");
				continue;
			}

			if (inUl) {
				out.append("</ul>");
				inUl = false;
			}
			if (inOl) {
				out.append("</ol>");
				inOl = false;
			}
			out.append("<p>").append(renderInline(line)).append("</p>");
		}

		if (inUl) {
			out.append("</ul>");
		}
		if (inOl) {
			out.append("</ol>");
		}
		return out.toString();
	}

	private String renderInline(String text) {
		String html = escapeHtml(text == null ? "" : text);
		html = html.replaceAll("&lt;u&gt;([\\s\\S]*?)&lt;/u&gt;", "<u>$1</u>");
		html = html.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
		html = html.replaceAll("\\*([^*]+)\\*", "<em>$1</em>");
		html = html.replaceAll("\\[([^\\]]+)\\]\\(((?:https?://|mailto:)[^)\\s]+)\\)", "<a href=\"$2\">$1</a>");
		return html;
	}

	private String escapeHtml(String raw) {
		if (raw == null) {
			return "";
		}
		return raw
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}
}
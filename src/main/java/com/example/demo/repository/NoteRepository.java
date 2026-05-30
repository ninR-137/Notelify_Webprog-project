package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.NoteEntity;

public interface NoteRepository extends JpaRepository<NoteEntity, Long> {
	List<NoteEntity> findByOwnerEmailOrderByUpdatedAtDesc(String ownerEmail);
}
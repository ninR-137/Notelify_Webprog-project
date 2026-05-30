package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TodoEntity;

public interface TodoRepository extends JpaRepository<TodoEntity, Long> {
	List<TodoEntity> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
}
package com.nexus.idp.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.idp.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
}

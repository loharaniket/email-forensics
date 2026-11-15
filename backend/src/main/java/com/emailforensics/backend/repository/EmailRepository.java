package com.emailforensics.backend.repository;

import com.emailforensics.backend.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    Optional<Email> findBySha256(String sha256);

    List<Email> findAllByOrderByCreatedAtDesc();
}

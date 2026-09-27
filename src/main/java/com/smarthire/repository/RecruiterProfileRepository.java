package com.smarthire.repository;
import com.smarthire.entity.RecruiterProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> { Optional<RecruiterProfile> findByUserId(Long userId); }

package com.smarthire.repository;
import com.smarthire.entity.Resume; import java.util.List; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface ResumeRepository extends JpaRepository<Resume, Long> { List<Resume> findByCandidateProfileIdOrderByUploadedAtDesc(Long candidateProfileId); Optional<Resume> findByIdAndCandidateProfileId(Long id, Long candidateProfileId); }

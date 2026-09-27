package com.smarthire.repository;
import com.smarthire.entity.Application; import java.util.List; import org.springframework.data.jpa.repository.JpaRepository;
public interface ApplicationRepository extends JpaRepository<Application, Long> { boolean existsByJobIdAndCandidateProfileId(Long jobId, Long candidateProfileId); List<Application> findByCandidateProfileIdOrderByAppliedAtDesc(Long candidateProfileId); List<Application> findByJobIdOrderByAppliedAtDesc(Long jobId); }

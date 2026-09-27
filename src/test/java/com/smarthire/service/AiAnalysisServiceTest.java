package com.smarthire.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.entity.Application;
import com.smarthire.entity.Job;
import com.smarthire.entity.Resume;
import com.smarthire.enums.JobType;
import com.smarthire.enums.MatchAnalysisStatus;
import com.smarthire.enums.WorkMode;
import com.smarthire.repository.ApplicationRepository;
import com.smarthire.repository.ResumeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiAnalysisServiceTest {
    @Test void javaDoesNotCountAsJavaScriptAndWordsDoNotBecomeSkills() {
        ApplicationRepository applications = mock(ApplicationRepository.class);
        AiAnalysisService service = new AiAnalysisService(mock(ResumeRepository.class), applications, new ObjectMapper(), "", "unused");
        Application application = new Application(1L, 1L, 1L);
        when(applications.findById(1L)).thenReturn(Optional.of(application));
        service.scoreApplicationNow(1L, job("JavaScript developer", "JavaScript"), new Resume(1L, "test.pdf", "test.pdf", "Java developer"));
        assertEquals(0, application.getMatchScore());
        assertFalse(service.hasRecognizableJobSkills(job("Maintain digital retail systems", "Team player")));
    }

    @Test void missingRequirementsProduceUnavailableMatchInsteadOfFakeZero() {
        ApplicationRepository applications = mock(ApplicationRepository.class);
        AiAnalysisService service = new AiAnalysisService(mock(ResumeRepository.class), applications, new ObjectMapper(), "", "unused");
        Application application = new Application(1L, 1L, 1L);
        when(applications.findById(1L)).thenReturn(Optional.of(application));
        service.scoreApplicationNow(1L, job("Team member", "Good communication"), new Resume(1L, "test.pdf", "test.pdf", "Java"));
        assertNull(application.getMatchScore());
        assertEquals(MatchAnalysisStatus.INSUFFICIENT_DATA, application.getMatchAnalysisStatus());
    }

    private Job job(String description, String requirements) {
        return new Job(1L, "Developer", "Test Company", "Remote", JobType.FULL_TIME, WorkMode.REMOTE, description, requirements, "");
    }
}

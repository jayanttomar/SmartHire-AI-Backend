package com.smarthire.entity;

import static org.junit.jupiter.api.Assertions.*;
import com.smarthire.enums.ApplicationStatus;
import com.smarthire.enums.MatchAnalysisStatus;
import com.smarthire.enums.MatchSource;
import org.junit.jupiter.api.Test;

class ApplicationTest {
    @Test void storesRecruiterDecisionAndAiMatch() {
        Application application = new Application(10L, 20L, 30L);
        application.completeMatch(82, "Strong Java experience", "[\"Java\"]", "[\"AWS\"]");
        application.updateStatus(ApplicationStatus.SHORTLISTED);
        assertEquals(82, application.getMatchScore());
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
        assertEquals("[\"AWS\"]", application.getMissingSkills());
    }

    @Test void tracksCoverLetterWithdrawalAndMatchLifecycle() {
        Application application = new Application(10L, 20L, 30L, "I would love to contribute to this team.");
        assertEquals(MatchAnalysisStatus.PENDING, application.getMatchAnalysisStatus());
        assertEquals("I would love to contribute to this team.", application.getCoverLetter());
        application.markMatchProcessing();
        application.completeMatch(105, "Strong fit", "[\"Java\"]", "[]", MatchSource.AI);
        application.withdraw();
        assertEquals(100, application.getMatchScore());
        assertEquals(MatchAnalysisStatus.COMPLETED, application.getMatchAnalysisStatus());
        assertEquals(MatchSource.AI, application.getMatchSource());
        assertEquals(ApplicationStatus.WITHDRAWN, application.getStatus());
    }
}

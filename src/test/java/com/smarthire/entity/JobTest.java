package com.smarthire.entity;

import static org.junit.jupiter.api.Assertions.*;
import com.smarthire.enums.JobType;
import com.smarthire.enums.WorkMode;
import org.junit.jupiter.api.Test;

class JobTest {
    @Test void updateAndDeactivateChangeJobState() {
        Job job = new Job(1L, "Java Developer", "SmartHire", "Remote", JobType.FULL_TIME, WorkMode.REMOTE, "Build APIs", "Java", "100k");
        job.update("Senior Java Developer", "SmartHire", "LA", JobType.CONTRACT, WorkMode.HYBRID, "Lead APIs", "Java, Spring", "120k");
        job.deactivate();
        assertEquals("Senior Java Developer", job.getTitle());
        assertFalse(job.isActive());
        assertNotNull(job.getUpdatedAt());
    }
}

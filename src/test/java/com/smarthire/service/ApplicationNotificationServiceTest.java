package com.smarthire.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import com.smarthire.enums.JobType;
import com.smarthire.enums.Role;
import com.smarthire.enums.WorkMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class ApplicationNotificationServiceTest {
    @Test
    void sendsAProfessionalShortlistNotificationToTheCandidate() {
        JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        User candidate = new User("Ayesha Khan", "ayesha@example.com", "hash", Role.CANDIDATE);
        Job job = new Job(4L, "Frontend Engineer", "FlowWorks", "Remote", JobType.FULL_TIME,
                WorkMode.REMOTE, "Build accessible product experiences.", "React", "");

        new ApplicationNotificationService(mailSender).sendShortlistedEmail(candidate, job);

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertEquals("ayesha@example.com", email.getValue().getTo()[0]);
        assertTrue(email.getValue().getSubject().contains("shortlisted"));
        assertTrue(email.getValue().getText().contains("Frontend Engineer"));
        assertTrue(email.getValue().getText().contains("FlowWorks"));
    }
}

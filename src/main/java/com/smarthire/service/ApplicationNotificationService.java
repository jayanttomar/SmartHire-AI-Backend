package com.smarthire.service;

import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ApplicationNotificationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationNotificationService.class);
    private final JavaMailSender mailSender;

    public ApplicationNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendShortlistedEmail(User candidate, Job job) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(candidate.getEmail());
        message.setSubject("You have been shortlisted - " + singleLine(job.getTitle()));
        message.setText("Hello " + singleLine(candidate.getName()) + ",\n\n"
                + "Great news! You have been shortlisted for the " + singleLine(job.getTitle())
                + " role at " + singleLine(job.getCompanyName()) + ".\n\n"
                + "The hiring team will review your application and contact you with the next steps.\n\n"
                + "Best regards,\n" + singleLine(job.getCompanyName()) + " hiring team\n"
                + "via SmartHire AI");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // A notification problem must not prevent the recruiter from updating the pipeline.
            log.warn("Could not send shortlist email for application to candidate {}", candidate.getId(), exception);
        }
    }

    private String singleLine(String value) {
        return value == null ? "SmartHire" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}

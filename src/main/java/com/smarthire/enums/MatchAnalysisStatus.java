package com.smarthire.enums;

/** Lifecycle of a resume-to-job comparison. A score is meaningful only when COMPLETED. */
public enum MatchAnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    INSUFFICIENT_DATA,
    FAILED
}

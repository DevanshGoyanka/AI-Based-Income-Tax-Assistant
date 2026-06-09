package com.itr.application.filing;

/**
 * FilingWorkflowUseCase — step machine for the ITR filing workflow.
 * <p>
 * States: DRAFT → PREFILLED → COMPUTED → VALIDATED → SUBMITTED → VERIFIED
 * Each transition is a method. The use case ensures correct ordering.
 */
@org.springframework.stereotype.Service
public class FilingWorkflowUseCase {

    public enum FilingStatus {
        DRAFT,
        PREFILLED,
        COMPUTED,
        VALIDATED,
        SUBMITTED,
        VERIFIED,
        REVISION_IN_PROGRESS,
        FILED
    }

    private FilingStatus currentStatus;

    public FilingWorkflowUseCase() {
        this.currentStatus = FilingStatus.DRAFT;
    }

    public FilingStatus prefill() {
        validateTransition(FilingStatus.DRAFT, FilingStatus.PREFILLED);
        this.currentStatus = FilingStatus.PREFILLED;
        return currentStatus;
    }

    public FilingStatus compute() {
        validateTransition(FilingStatus.PREFILLED, FilingStatus.COMPUTED);
        this.currentStatus = FilingStatus.COMPUTED;
        return currentStatus;
    }

    public FilingStatus validate() {
        validateTransition(FilingStatus.COMPUTED, FilingStatus.VALIDATED);
        this.currentStatus = FilingStatus.VALIDATED;
        return currentStatus;
    }

    public FilingStatus submit() {
        validateTransition(FilingStatus.VALIDATED, FilingStatus.SUBMITTED);
        this.currentStatus = FilingStatus.SUBMITTED;
        return currentStatus;
    }

    public FilingStatus verify() {
        validateTransition(FilingStatus.SUBMITTED, FilingStatus.VERIFIED);
        this.currentStatus = FilingStatus.VERIFIED;
        return currentStatus;
    }

    public FilingStatus revise() {
        if (currentStatus != FilingStatus.VERIFIED && currentStatus != FilingStatus.FILED) {
            throw new IllegalStateException("Cannot revise from status: " + currentStatus);
        }
        this.currentStatus = FilingStatus.REVISION_IN_PROGRESS;
        return currentStatus;
    }

    public FilingStatus getCurrentStatus() {
        return currentStatus;
    }

    private void validateTransition(FilingStatus from, FilingStatus to) {
        if (currentStatus != from) {
            throw new IllegalStateException(
                "Cannot transition to " + to + " from " + currentStatus + ". Required status: " + from);
        }
    }
}

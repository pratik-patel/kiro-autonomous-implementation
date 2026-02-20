package com.hex.service;

import com.hex.model.AttributeStatus;
import com.hex.model.LoanAttribute;
import com.hex.model.LoanDecisionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pure business logic for determining loan decision status based on attribute evaluations.
 * Priority order: REPURCHASE > RECLASS_APPROVED > PARTIALLY_APPROVED > APPROVED / REJECTED.
 */
@Service
public class StatusDeterminationService {

    private static final Logger log = LoggerFactory.getLogger(StatusDeterminationService.class);

    /**
     * Determines the overall loan decision status from a list of evaluated attributes.
     * All attributes must be in a terminal state (not PENDING_REVIEW).
     *
     * @param attributes the list of loan attributes with their statuses
     * @return the determined loan decision status
     * @throws IllegalArgumentException if the attribute list is null or empty
     */
    public LoanDecisionStatus determineStatus(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("Attributes list must not be null or empty");
        }

        boolean hasRepurchase = false;
        boolean hasReclass = false;
        boolean hasApproved = false;
        boolean hasRejected = false;

        for (LoanAttribute attribute : attributes) {
            switch (attribute.getAttributeStatus()) {
                case REPURCHASE -> hasRepurchase = true;
                case RECLASS -> hasReclass = true;
                case APPROVED -> hasApproved = true;
                case REJECTED -> hasRejected = true;
                default -> log.warn("Unexpected attribute status: {}", attribute.getAttributeStatus());
            }
        }

        // Priority: REPURCHASE > RECLASS_APPROVED > PARTIALLY_APPROVED > APPROVED / REJECTED
        if (hasRepurchase) {
            return LoanDecisionStatus.REPURCHASE;
        }
        if (hasReclass) {
            return LoanDecisionStatus.RECLASS_APPROVED;
        }
        if (hasApproved && hasRejected) {
            return LoanDecisionStatus.PARTIALLY_APPROVED;
        }
        if (hasApproved) {
            return LoanDecisionStatus.APPROVED;
        }
        return LoanDecisionStatus.REJECTED;
    }

    /**
     * Checks whether any attribute in the list is still pending review.
     *
     * @param attributes the list of loan attributes
     * @return true if at least one attribute has PENDING_REVIEW status
     */
    public boolean hasPendingAttributes(List<LoanAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        return attributes.stream()
                .anyMatch(attr -> attr.getAttributeStatus() == AttributeStatus.PENDING_REVIEW);
    }
}

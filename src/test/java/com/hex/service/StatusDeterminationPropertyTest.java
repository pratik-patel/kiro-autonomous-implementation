package com.hex.service;

import com.hex.model.AttributeStatus;
import com.hex.model.LoanAttribute;
import com.hex.model.LoanDecisionStatus;
import net.jqwik.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for StatusDeterminationService.
 * Feature: ldc-loan-review-workflow
 */
class StatusDeterminationPropertyTest {

    private final StatusDeterminationService service = new StatusDeterminationService();

    // ========================================================================
    // Property 4: Pending attribute detection controls workflow progression
    // Validates: Requirements 3.5, 3.6
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 4a: List with at least one PENDING_REVIEW returns true")
    void pendingAttributeDetected(
            @ForAll("nonEmptyAttributeList") List<LoanAttribute> baseAttributes,
            @ForAll("insertionIndex") int index) {

        LoanAttribute pending = LoanAttribute.builder()
                .attributeName("pending-attr")
                .attributeStatus(AttributeStatus.PENDING_REVIEW)
                .build();

        List<LoanAttribute> attributes = new ArrayList<>(baseAttributes);
        int insertAt = Math.abs(index) % (attributes.size() + 1);
        attributes.add(insertAt, pending);

        assertThat(service.hasPendingAttributes(attributes)).isTrue();
    }

    @Property(tries = 100)
    @Label("Property 4b: List with no PENDING_REVIEW returns false")
    void noPendingAttributeDetected(
            @ForAll("nonPendingAttributeList") List<LoanAttribute> attributes) {

        assertThat(service.hasPendingAttributes(attributes)).isFalse();
    }

    // ========================================================================
    // Property 5: Homogeneous approved attributes yield APPROVED
    // Validates: Requirements 4.1
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 5: All APPROVED attributes yield APPROVED status")
    void allApprovedYieldsApproved(
            @ForAll("approvedOnlyList") List<LoanAttribute> attributes) {

        assertThat(service.determineStatus(attributes)).isEqualTo(LoanDecisionStatus.APPROVED);
    }

    // ========================================================================
    // Property 6: Homogeneous rejected attributes yield REJECTED
    // Validates: Requirements 4.2
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 6: All REJECTED attributes yield REJECTED status")
    void allRejectedYieldsRejected(
            @ForAll("rejectedOnlyList") List<LoanAttribute> attributes) {

        assertThat(service.determineStatus(attributes)).isEqualTo(LoanDecisionStatus.REJECTED);
    }

    // ========================================================================
    // Property 7: Mixed approved/rejected yield PARTIALLY_APPROVED
    // Validates: Requirements 4.3
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 7: Mix of APPROVED and REJECTED yields PARTIALLY_APPROVED")
    void mixedApprovedRejectedYieldsPartiallyApproved(
            @ForAll("mixedApprovedRejectedList") List<LoanAttribute> attributes) {

        assertThat(service.determineStatus(attributes)).isEqualTo(LoanDecisionStatus.PARTIALLY_APPROVED);
    }

    // ========================================================================
    // Property 8: Repurchase takes priority
    // Validates: Requirements 4.4
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 8: Any REPURCHASE attribute yields REPURCHASE regardless of others")
    void repurchaseTakesPriority(
            @ForAll("listWithRepurchase") List<LoanAttribute> attributes) {

        assertThat(service.determineStatus(attributes)).isEqualTo(LoanDecisionStatus.REPURCHASE);
    }

    // ========================================================================
    // Property 9: Reclass takes priority over partial/approved/rejected
    // Validates: Requirements 4.5
    // ========================================================================

    @Property(tries = 100)
    @Label("Property 9: RECLASS without REPURCHASE yields RECLASS_APPROVED")
    void reclassTakesPriorityOverOthers(
            @ForAll("listWithReclassNoRepurchase") List<LoanAttribute> attributes) {

        assertThat(service.determineStatus(attributes)).isEqualTo(LoanDecisionStatus.RECLASS_APPROVED);
    }

    // ========================================================================
    // Arbitraries (generators)
    // ========================================================================

    @Provide
    Arbitrary<Integer> insertionIndex() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<List<LoanAttribute>> nonEmptyAttributeList() {
        return attributeWithStatus(
                Arbitraries.of(AttributeStatus.APPROVED, AttributeStatus.REJECTED,
                        AttributeStatus.REPURCHASE, AttributeStatus.RECLASS))
                .list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<LoanAttribute>> nonPendingAttributeList() {
        return attributeWithStatus(
                Arbitraries.of(AttributeStatus.APPROVED, AttributeStatus.REJECTED,
                        AttributeStatus.REPURCHASE, AttributeStatus.RECLASS))
                .list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<LoanAttribute>> approvedOnlyList() {
        return attributeWithStatus(Arbitraries.just(AttributeStatus.APPROVED))
                .list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<LoanAttribute>> rejectedOnlyList() {
        return attributeWithStatus(Arbitraries.just(AttributeStatus.REJECTED))
                .list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<LoanAttribute>> mixedApprovedRejectedList() {
        Arbitrary<LoanAttribute> approved = attributeWithStatus(Arbitraries.just(AttributeStatus.APPROVED));
        Arbitrary<LoanAttribute> rejected = attributeWithStatus(Arbitraries.just(AttributeStatus.REJECTED));

        // Ensure at least one of each
        return Combinators.combine(
                approved.list().ofMinSize(1).ofMaxSize(5),
                rejected.list().ofMinSize(1).ofMaxSize(5)
        ).as((approvedList, rejectedList) -> {
            List<LoanAttribute> combined = new ArrayList<>(approvedList);
            combined.addAll(rejectedList);
            return combined;
        });
    }

    @Provide
    Arbitrary<List<LoanAttribute>> listWithRepurchase() {
        Arbitrary<LoanAttribute> repurchase = attributeWithStatus(Arbitraries.just(AttributeStatus.REPURCHASE));
        Arbitrary<LoanAttribute> anyNonPending = attributeWithStatus(
                Arbitraries.of(AttributeStatus.APPROVED, AttributeStatus.REJECTED,
                        AttributeStatus.REPURCHASE, AttributeStatus.RECLASS));

        return Combinators.combine(
                repurchase.list().ofMinSize(1).ofMaxSize(3),
                anyNonPending.list().ofMinSize(0).ofMaxSize(5)
        ).as((repurchaseList, otherList) -> {
            List<LoanAttribute> combined = new ArrayList<>(repurchaseList);
            combined.addAll(otherList);
            return combined;
        });
    }

    @Provide
    Arbitrary<List<LoanAttribute>> listWithReclassNoRepurchase() {
        Arbitrary<LoanAttribute> reclass = attributeWithStatus(Arbitraries.just(AttributeStatus.RECLASS));
        Arbitrary<LoanAttribute> nonRepurchaseNonPending = attributeWithStatus(
                Arbitraries.of(AttributeStatus.APPROVED, AttributeStatus.REJECTED, AttributeStatus.RECLASS));

        return Combinators.combine(
                reclass.list().ofMinSize(1).ofMaxSize(3),
                nonRepurchaseNonPending.list().ofMinSize(0).ofMaxSize(5)
        ).as((reclassList, otherList) -> {
            List<LoanAttribute> combined = new ArrayList<>(reclassList);
            combined.addAll(otherList);
            return combined;
        });
    }

    private Arbitrary<LoanAttribute> attributeWithStatus(Arbitrary<AttributeStatus> statusArbitrary) {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15),
                statusArbitrary
        ).as((name, status) -> LoanAttribute.builder()
                .attributeName(name)
                .attributeStatus(status)
                .build());
    }
}

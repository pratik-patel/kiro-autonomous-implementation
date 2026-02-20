package com.hex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Represents a single loan attribute with its review status.
 * Uses DynamoDbBean for nested document persistence in WorkflowState.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class LoanAttribute {
    private String attributeName;
    private AttributeStatus attributeStatus;
}

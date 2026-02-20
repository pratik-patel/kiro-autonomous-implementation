package com.hex.model.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO representing a loan attribute in API requests.
 */
@Value
@Builder
public class LoanAttributeDto {
    String attributeName;
    String attributeStatus;
}

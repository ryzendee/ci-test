package ru.caselab.edm.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttributeValueCreateDTO {

    @NotNull
    private Long attributeId;
    @NotNull
    private Long documentId;
    private String value;
}

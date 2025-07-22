package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.jfc.gnn.model.ProductSpecifications;
import com.jfc.owl.model.bom.BomComponent;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingBom {
    private String masterItemCode;
    private ProductSpecifications specifications;
    private List<BomComponent> components;
    private boolean isValidated;
}
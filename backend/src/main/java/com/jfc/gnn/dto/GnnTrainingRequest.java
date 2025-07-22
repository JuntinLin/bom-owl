package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnTrainingRequest {
    private String modelName;
    private List<TrainingBom> trainingBoms;
    private TrainingOptions trainingOptions;
    private String description;
}
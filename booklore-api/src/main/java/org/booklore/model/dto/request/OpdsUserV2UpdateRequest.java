package org.booklore.model.dto.request;

import org.booklore.model.enums.OpdsSortOrder;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpdsUserV2UpdateRequest {
    @NotNull(message = "Sort order is required")
    private OpdsSortOrder sortOrder;
    private Boolean epubOptimizationEnabled;
    private Integer epubJpegQuality;
    private Boolean epubEnableGrayscale;
    private Boolean epubResizeImages;
    private Integer epubMaxImageWidth;
    private Integer epubMaxImageHeight;
    private Integer epubConversionLimitMb;
}

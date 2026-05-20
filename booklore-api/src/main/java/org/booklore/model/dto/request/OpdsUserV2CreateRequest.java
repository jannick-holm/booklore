package org.booklore.model.dto.request;

import org.booklore.model.enums.OpdsSortOrder;
import lombok.Data;

@Data
public class OpdsUserV2CreateRequest {
    private String username;
    private String password;
    private OpdsSortOrder sortOrder;
    private boolean epubOptimizationEnabled = false;
    private int epubJpegQuality = 85;
    private boolean epubEnableGrayscale = false;
    private boolean epubResizeImages = false;
    private int epubMaxImageWidth = 480;
    private int epubMaxImageHeight = 800;
    private int epubConversionLimitMb = 100;
}

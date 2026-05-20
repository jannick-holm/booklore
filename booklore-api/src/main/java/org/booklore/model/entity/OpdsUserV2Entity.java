package org.booklore.model.entity;

import org.booklore.model.enums.OpdsSortOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "opds_user_v2")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpdsUserV2Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private BookLoreUserEntity user;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_order", length = 20)
    @Builder.Default
    private OpdsSortOrder sortOrder = OpdsSortOrder.RECENT;

    @Column(name = "epub_optimization_enabled", nullable = false)
    @Builder.Default
    private boolean epubOptimizationEnabled = false;

    @Column(name = "epub_jpeg_quality", nullable = false)
    @Builder.Default
    private int epubJpegQuality = 85;

    @Column(name = "epub_enable_grayscale", nullable = false)
    @Builder.Default
    private boolean epubEnableGrayscale = false;

    @Column(name = "epub_resize_images", nullable = false)
    @Builder.Default
    private boolean epubResizeImages = false;

    @Column(name = "epub_max_image_width", nullable = false)
    @Builder.Default
    private int epubMaxImageWidth = 480;

    @Column(name = "epub_max_image_height", nullable = false)
    @Builder.Default
    private int epubMaxImageHeight = 800;

    @Column(name = "epub_conversion_limit_mb", nullable = false)
    @Builder.Default
    private int epubConversionLimitMb = 100;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

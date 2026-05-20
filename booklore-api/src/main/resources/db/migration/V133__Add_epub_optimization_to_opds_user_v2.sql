ALTER TABLE opds_user_v2
    ADD COLUMN epub_optimization_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN epub_jpeg_quality          INT     NOT NULL DEFAULT 85,
    ADD COLUMN epub_enable_grayscale      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN epub_resize_images         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN epub_max_image_width       INT     NOT NULL DEFAULT 480,
    ADD COLUMN epub_max_image_height      INT     NOT NULL DEFAULT 800,
    ADD COLUMN epub_conversion_limit_mb   INT     NOT NULL DEFAULT 100;

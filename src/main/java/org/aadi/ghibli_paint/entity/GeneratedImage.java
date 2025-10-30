package org.aadi.ghibli_paint.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore  // Prevent recursion during JSON serialization
    private User user;

    @Column(name = "prompt", nullable = false, length = 1000)
    private String prompt;  // VARCHAR(1000) instead of @Lob/TEXT to avoid LOB auto-commit issues

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "image_type")
    private String imageType; // "text-to-image" or "image-to-image"

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;
}
package org.aadi.ghibli_paint.controller;

import lombok.RequiredArgsConstructor;
import org.aadi.ghibli_paint.entity.GeneratedImage;
import org.aadi.ghibli_paint.service.StabilityAIService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImageController {

    private final StabilityAIService stabilityAIService;

    @PostMapping(value = "/generate/text-to-image", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneratedImage> generateImageFromText(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            GeneratedImage image = stabilityAIService.generateImageFromText(prompt);
            return ResponseEntity.ok(image);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/generate/image-to-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneratedImage> generateImageFromImage(
            @RequestParam("prompt") String prompt,
            @RequestParam("image") MultipartFile imageFile) {
        try {
            GeneratedImage image = stabilityAIService.generateImageFromImage(prompt, imageFile);
            return ResponseEntity.ok(image);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/gallery")
    public ResponseEntity<List<GeneratedImage>> getUserGallery() {
        List<GeneratedImage> images = stabilityAIService.getUserImages();
        return ResponseEntity.ok(images);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        try {
            stabilityAIService.deleteImage(imageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/download/{imageId}")
    public ResponseEntity<Resource> downloadImage(@PathVariable Long imageId) {
        try {
            GeneratedImage image = stabilityAIService.getUserImages().stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Image not found"));

            Path path = Paths.get(image.getImageUrl());
            Resource resource = new FileSystemResource(path);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getOriginalFilename() + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/serve/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path path = Paths.get("generated-images").resolve(filename);
            Resource resource = new FileSystemResource(path);

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
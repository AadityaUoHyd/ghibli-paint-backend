package org.aadi.ghibli_paint.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aadi.ghibli_paint.entity.GeneratedImage;
import org.aadi.ghibli_paint.entity.User;
import org.aadi.ghibli_paint.repository.GeneratedImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StabilityAIService {

    @Value("${stability.api.key}")
    private String apiKey;

    @Value("${stability.api.url}")
    private String apiUrl;

    private final GeneratedImageRepository generatedImageRepository;
    private final UserService userService;

    private static final String IMAGE_STORAGE_PATH = "generated-images/";

    public GeneratedImage generateImageFromText(String prompt) throws IOException {
        User currentUser = userService.getCurrentUser();

        WebClient webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, "image/*")  // As per docs curl
                .build();

        String enhancedPrompt = "Studio Ghibli style, anime, " + prompt;

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("prompt", enhancedPrompt);
        builder.part("aspect_ratio", "1:1");
        builder.part("output_format", "png");
        builder.part("seed", 42);

        log.info("Sending multipart request to Stability AI Core with prompt: {}", enhancedPrompt);

        try {
            byte[] imageBytes = webClient.post()
                    .uri("/generate/core")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return Flux.just(bytes);
                    })
                    .reduce((bytes1, bytes2) -> {
                        byte[] combined = new byte[bytes1.length + bytes2.length];
                        System.arraycopy(bytes1, 0, combined, 0, bytes1.length);
                        System.arraycopy(bytes2, 0, combined, bytes1.length, bytes2.length);
                        return combined;
                    })
                    .block();

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("No image data returned from Stability AI");
            }

            log.info("Received image bytes: {} bytes", imageBytes.length);

            String filename = UUID.randomUUID().toString() + ".png";
            String imagePath = saveImage(imageBytes, filename);

            GeneratedImage generatedImage = new GeneratedImage();
            generatedImage.setUser(currentUser);
            generatedImage.setPrompt(prompt);
            generatedImage.setImageUrl(imagePath);
            generatedImage.setImageType("text-to-image");
            generatedImage.setOriginalFilename(filename);
            generatedImage.setFileSize((long) imageBytes.length);
            generatedImage.setWidth(1024);
            generatedImage.setHeight(1024);

            return generatedImageRepository.save(generatedImage);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.error("Stability AI HTTP error {}: {}", ex.getStatusCode(), errorBody);
            throw new RuntimeException("Stability AI request failed: " + errorBody, ex);
        } catch (Exception ex) {
            log.error("Unexpected error during Stability AI generation: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to generate image from text", ex);
        }
    }

    public GeneratedImage generateImageFromImage(String prompt, MultipartFile imageFile) throws IOException {
        User currentUser = userService.getCurrentUser();

        WebClient webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, "image/*")  // As per docs curl
                .build();

        String enhancedPrompt = "Studio Ghibli style, anime, " + prompt;

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("prompt", enhancedPrompt);
        builder.part("image", imageFile.getResource());  // init_image -> image
        builder.part("strength", "0.35");  // image_strength -> strength
        builder.part("aspect_ratio", "1:1");
        builder.part("output_format", "png");
        builder.part("seed", 42);
        builder.part("mode", "image-to-image");  // Required for SD3 image-to-image

        log.info("Sending image-to-image multipart request to Stability AI SD3 with prompt: {}", enhancedPrompt);

        try {
            byte[] imageBytes = webClient.post()
                    .uri("/generate/sd3")  // Use SD3 endpoint for image-to-image
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return Flux.just(bytes);
                    })
                    .reduce((bytes1, bytes2) -> {
                        byte[] combined = new byte[bytes1.length + bytes2.length];
                        System.arraycopy(bytes1, 0, combined, 0, bytes1.length);
                        System.arraycopy(bytes2, 0, combined, bytes1.length, bytes2.length);
                        return combined;
                    })
                    .block();

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("No image data returned from Stability AI");
            }

            log.info("Received image bytes: {} bytes", imageBytes.length);

            String filename = UUID.randomUUID().toString() + ".png";
            String imagePath = saveImage(imageBytes, filename);

            GeneratedImage generatedImage = new GeneratedImage();
            generatedImage.setUser(currentUser);
            generatedImage.setPrompt(prompt);
            generatedImage.setImageUrl(imagePath);
            generatedImage.setImageType("image-to-image");
            generatedImage.setOriginalFilename(filename);
            generatedImage.setFileSize((long) imageBytes.length);
            generatedImage.setWidth(1024);
            generatedImage.setHeight(1024);

            return generatedImageRepository.save(generatedImage);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.error("Stability AI HTTP error {}: {}", ex.getStatusCode(), errorBody);
            throw new RuntimeException("Stability AI request failed: " + errorBody, ex);
        } catch (Exception ex) {
            log.error("Unexpected error during Stability AI image-to-image generation: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to generate image from image", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<GeneratedImage> getUserImages() {
        User currentUser = userService.getCurrentUser();
        return generatedImageRepository.findByUserOrderByCreatedAtDesc(currentUser);
    }

    public void deleteImage(Long imageId) {
        User currentUser = userService.getCurrentUser();
        GeneratedImage image = generatedImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!image.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to delete this image");
        }

        try {
            Path path = Paths.get(image.getImageUrl());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Error deleting image file", e);
        }

        generatedImageRepository.delete(image);
    }

    private String saveImage(byte[] imageBytes, String filename) throws IOException {
        Path directory = Paths.get(IMAGE_STORAGE_PATH);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path filePath = directory.resolve(filename);
        Files.write(filePath, imageBytes);

        return filePath.toString();
    }
}
package com.corc.backend.controller;

import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "uploads") String directory) {
        String url = storageService.uploadFile(file, directory);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded", Map.of("url", url)));
    }

    @PostMapping("/upload/product-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {
        String url = storageService.uploadFile(file, "products");
        return ResponseEntity.ok(ApiResponse.ok("Product image uploaded", Map.of("url", url)));
    }

    @PostMapping("/upload/custom-design")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCustomDesign(
            @RequestParam("file") MultipartFile file) {
        String url = storageService.uploadFile(file, "custom-designs");
        return ResponseEntity.ok(ApiResponse.ok("Design uploaded", Map.of("url", url)));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam String url) {
        storageService.deleteFile(url);
        return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
    }
}

package com.corc.backend.controller;

import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService configService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getAllConfigs()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(configService.getConfig(key)));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Config updated",
                configService.updateConfig(key, body.get("value"))));
    }
}

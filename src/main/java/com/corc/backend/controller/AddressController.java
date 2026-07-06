package com.corc.backend.controller;

import com.corc.backend.dto.request.AddressRequest;
import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddresses(userDetails.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        Map<String, Object> address = addressService.addAddress(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address Added", address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        addressService.removeAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Address Removed", null));
    }
}

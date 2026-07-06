package com.corc.backend.service;

import com.corc.backend.dto.request.AddressRequest;
import com.corc.backend.entity.Address;
import com.corc.backend.entity.User;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.AddressRepository;
import com.corc.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAddresses(String email) {
        User user = findUser(email);
        return addressRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> addAddress(String email, AddressRequest request) {
        User user = findUser(email);
        Address address = Address.builder()
                .user(user)
                .type(request.getType())
                .street(request.getStreet())
                .city(request.getCity())
                .zip(request.getZip())
                .country(request.getCountry())
                .isDefault(request.isDefault())
                .build();

        address = addressRepository.save(address);
        return mapToMap(address);
    }

    @Transactional
    public void removeAddress(String email, Long addressId) {
        User user = findUser(email);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        addressRepository.delete(address);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Map<String, Object> mapToMap(Address addr) {
        return Map.of(
                "id", addr.getId(),
                "type", addr.getType(),
                "street", addr.getStreet(),
                "city", addr.getCity(),
                "zip", addr.getZip(),
                "country", addr.getCountry() != null ? addr.getCountry() : "",
                "isDefault", addr.isDefault()
        );
    }
}

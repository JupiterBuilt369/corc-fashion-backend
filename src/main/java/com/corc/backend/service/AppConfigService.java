package com.corc.backend.service;

import com.corc.backend.entity.AppConfig;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    @Transactional(readOnly = true)
    public Map<String, String> getConfig(String key) {
        AppConfig config = appConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("AppConfig", "key", key));
        return Map.of("key", config.getConfigKey(), "value", config.getConfigValue());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllConfigs() {
        var configs = appConfigRepository.findAll();
        var result = new java.util.HashMap<String, String>();
        configs.forEach(c -> result.put(c.getConfigKey(), c.getConfigValue()));
        return result;
    }

    @Transactional
    public Map<String, String> updateConfig(String key, String value) {
        AppConfig config = appConfigRepository.findByConfigKey(key)
                .orElse(AppConfig.builder().configKey(key).build());
        config.setConfigValue(value);
        appConfigRepository.save(config);
        return Map.of("key", config.getConfigKey(), "value", config.getConfigValue());
    }
}

package com.corc.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_configs", indexes = {
    @Index(name = "idx_config_key", columnList = "configKey", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;

    private String description;

    @UpdateTimestamp
    private Instant updatedAt;
}

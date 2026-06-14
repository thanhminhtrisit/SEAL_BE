package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "system_configs")
@Getter @Setter @NoArgsConstructor
public class SystemConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;
    @Lob @Column(name = "config_value", nullable = false)
    private String configValue;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

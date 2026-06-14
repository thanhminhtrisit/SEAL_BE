package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.PublicationType;
@Entity @Table(name = "result_publications")
@Getter @Setter @NoArgsConstructor
public class ResultPublication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id")
    private Round round;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id")
    private Category category;
    @Enumerated(EnumType.STRING) @Column(name = "publication_type", nullable = false, length = 50)
    private PublicationType publicationType = PublicationType.ROUND_RESULT;
    @Column(nullable = false, length = 200) private String title;
    @Lob private String description;
    @Column(name = "is_public", nullable = false) private Boolean isPublic = false;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "published_by")
    private User publishedBy;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

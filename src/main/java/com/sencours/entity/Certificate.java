package com.sencours.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "certificates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_number", unique = true, nullable = false)
    private String certificateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "completion_date", nullable = false)
    private LocalDateTime completionDate;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
        if (certificateNumber == null) {
            certificateNumber = "SC-" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                    "-" + String.format("%05d", (int) (Math.random() * 100000));
        }
    }
}

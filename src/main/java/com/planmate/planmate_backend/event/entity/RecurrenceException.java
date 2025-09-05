package com.planmate.planmate_backend.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "recurrence_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, includeFieldNames = true)
@EntityListeners(AuditingEntityListener.class)
public class RecurrenceException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalDate exceptionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "override_event_id")
    private Event overrideEvent;
}

package com.sencours.entity;

import com.sencours.enums.LessonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private Integer duration;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "is_free", nullable = false)
    private Boolean isFree = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY)
    private List<Progress> progresses = new ArrayList<>();
}

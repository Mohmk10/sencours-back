package com.sencours.service;

import com.sencours.dto.request.LessonRequest;
import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.response.LessonResponse;
import com.sencours.entity.Lesson;
import com.sencours.entity.Section;
import com.sencours.enums.LessonType;
import com.sencours.exception.LessonNotFoundException;
import com.sencours.exception.SectionNotFoundException;
import com.sencours.mapper.LessonMapper;
import com.sencours.repository.LessonRepository;
import com.sencours.repository.SectionRepository;
import com.sencours.service.impl.LessonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LessonMapper lessonMapper;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private LessonRequest lessonRequest;
    private Lesson lesson;
    private LessonResponse lessonResponse;
    private Section section;

    @BeforeEach
    void setUp() {
        section = new Section();
        section.setId(1L);
        section.setTitle("Introduction");

        lessonRequest = LessonRequest.builder()
                .title("Bienvenue")
                .type(LessonType.VIDEO)
                .content("http://video.url")
                .duration(10)
                .isFree(true)
                .build();

        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("Bienvenue");
        lesson.setType(LessonType.VIDEO);
        lesson.setContent("http://video.url");
        lesson.setDuration(10);
        lesson.setOrderIndex(1);
        lesson.setIsFree(true);
        lesson.setSection(section);

        lessonResponse = LessonResponse.builder()
                .id(1L)
                .title("Bienvenue")
                .type(LessonType.VIDEO)
                .content("http://video.url")
                .duration(10)
                .orderIndex(1)
                .isFree(true)
                .sectionId(1L)
                .build();
    }

    @Nested
    @DisplayName("Tests pour create()")
    class CreateTests {

        @Test
        @DisplayName("Devrait créer une leçon avec orderIndex auto-incrémenté")
        void shouldCreateLessonWithAutoIncrementedOrderIndex() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
            when(lessonRepository.countBySectionId(1L)).thenReturn(2);
            when(lessonMapper.toEntity(lessonRequest, section)).thenReturn(lesson);
            when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);
            when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

            LessonResponse result = lessonService.create(1L, lessonRequest);

            assertThat(result).isNotNull();
            assertThat(lesson.getOrderIndex()).isEqualTo(3);
            verify(lessonRepository).save(any(Lesson.class));
        }

        @Test
        @DisplayName("Devrait lever SectionNotFoundException si section non trouvée")
        void shouldThrowExceptionWhenSectionNotFound() {
            when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.create(999L, lessonRequest))
                    .isInstanceOf(SectionNotFoundException.class);

            verify(lessonRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Devrait retourner une leçon par ID")
        void shouldReturnLessonById() {
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

            LessonResponse result = lessonService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever LessonNotFoundException")
        void shouldThrowLessonNotFoundException() {
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.getById(999L))
                    .isInstanceOf(LessonNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getBySectionId()")
    class GetBySectionIdTests {

        @Test
        @DisplayName("Devrait retourner les leçons d'une section ordonnées")
        void shouldReturnLessonsOrderedByIndex() {
            Lesson lesson2 = new Lesson();
            lesson2.setId(2L);
            lesson2.setTitle("Chapitre 1");
            lesson2.setOrderIndex(2);

            LessonResponse response2 = LessonResponse.builder()
                    .id(2L)
                    .title("Chapitre 1")
                    .orderIndex(2)
                    .build();

            when(sectionRepository.existsById(1L)).thenReturn(true);
            when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(1L))
                    .thenReturn(Arrays.asList(lesson, lesson2));
            when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);
            when(lessonMapper.toResponse(lesson2)).thenReturn(response2);

            List<LessonResponse> result = lessonService.getBySectionId(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrderIndex()).isEqualTo(1);
            assertThat(result.get(1).getOrderIndex()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Tests pour getFreeLessons()")
    class GetFreeLessonsTests {

        @Test
        @DisplayName("Devrait retourner les leçons gratuites")
        void shouldReturnFreeLessons() {
            when(lessonRepository.findByIsFreeTrue()).thenReturn(List.of(lesson));
            when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

            List<LessonResponse> result = lessonService.getFreeLessons();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsFree()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests pour update()")
    class UpdateTests {

        @Test
        @DisplayName("Devrait mettre à jour une leçon")
        void shouldUpdateLesson() {
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);
            when(lessonMapper.toResponse(lesson)).thenReturn(lessonResponse);

            LessonResponse result = lessonService.update(1L, lessonRequest);

            assertThat(result).isNotNull();
            verify(lessonMapper).updateEntityFromRequest(lessonRequest, lesson);
        }
    }

    @Nested
    @DisplayName("Tests pour delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer une leçon et réorganiser les index")
        void shouldDeleteAndReorderLessons() {
            Lesson lesson2 = new Lesson();
            lesson2.setId(2L);
            lesson2.setOrderIndex(2);
            lesson2.setSection(section);

            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(1L))
                    .thenReturn(List.of(lesson2));

            lessonService.delete(1L);

            verify(lessonRepository).delete(lesson);
        }
    }

    @Nested
    @DisplayName("Tests pour reorder()")
    class ReorderTests {

        @Test
        @DisplayName("Devrait réorganiser les leçons")
        void shouldReorderLessons() {
            Lesson lesson2 = new Lesson();
            lesson2.setId(2L);
            lesson2.setOrderIndex(2);
            lesson2.setSection(section);

            when(sectionRepository.existsById(1L)).thenReturn(true);
            when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson2));
            when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
            when(lessonMapper.toResponse(any(Lesson.class))).thenReturn(lessonResponse);

            ReorderRequest request = ReorderRequest.builder()
                    .orderedIds(Arrays.asList(2L, 1L))
                    .build();

            List<LessonResponse> result = lessonService.reorder(1L, request);

            assertThat(result).hasSize(2);
            assertThat(lesson2.getOrderIndex()).isEqualTo(1);
            assertThat(lesson.getOrderIndex()).isEqualTo(2);
        }
    }
}

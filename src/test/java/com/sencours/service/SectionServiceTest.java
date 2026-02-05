package com.sencours.service;

import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.request.SectionRequest;
import com.sencours.dto.response.SectionResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Section;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.exception.SectionNotFoundException;
import com.sencours.mapper.SectionMapper;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.SectionRepository;
import com.sencours.service.impl.SectionServiceImpl;
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
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SectionMapper sectionMapper;

    @InjectMocks
    private SectionServiceImpl sectionService;

    private SectionRequest sectionRequest;
    private Section section;
    private SectionResponse sectionResponse;
    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Java pour débutants");

        sectionRequest = SectionRequest.builder()
                .title("Introduction")
                .build();

        section = new Section();
        section.setId(1L);
        section.setTitle("Introduction");
        section.setOrderIndex(1);
        section.setCourse(course);

        sectionResponse = SectionResponse.builder()
                .id(1L)
                .title("Introduction")
                .orderIndex(1)
                .courseId(1L)
                .courseTitle("Java pour débutants")
                .build();
    }

    @Nested
    @DisplayName("Tests pour create()")
    class CreateTests {

        @Test
        @DisplayName("Devrait créer une section avec orderIndex auto-incrémenté")
        void shouldCreateSectionWithAutoIncrementedOrderIndex() {
            when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
            when(sectionRepository.countByCourseId(1L)).thenReturn(2);
            when(sectionMapper.toEntity(sectionRequest, course)).thenReturn(section);
            when(sectionRepository.save(any(Section.class))).thenReturn(section);
            when(sectionMapper.toResponseWithoutLessons(section)).thenReturn(sectionResponse);

            SectionResponse result = sectionService.create(1L, sectionRequest);

            assertThat(result).isNotNull();
            assertThat(section.getOrderIndex()).isEqualTo(3);
            verify(sectionRepository).save(any(Section.class));
        }

        @Test
        @DisplayName("Devrait lever exception si cours non trouvé")
        void shouldThrowExceptionWhenCourseNotFound() {
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.create(999L, sectionRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cours");

            verify(sectionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests pour getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Devrait retourner une section par ID")
        void shouldReturnSectionById() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
            when(sectionMapper.toResponse(section)).thenReturn(sectionResponse);

            SectionResponse result = sectionService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Devrait lever SectionNotFoundException")
        void shouldThrowSectionNotFoundException() {
            when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.getById(999L))
                    .isInstanceOf(SectionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests pour getByCourseId()")
    class GetByCourseIdTests {

        @Test
        @DisplayName("Devrait retourner les sections d'un cours ordonnées")
        void shouldReturnSectionsOrderedByIndex() {
            Section section2 = new Section();
            section2.setId(2L);
            section2.setTitle("Chapitre 1");
            section2.setOrderIndex(2);

            SectionResponse response2 = SectionResponse.builder()
                    .id(2L)
                    .title("Chapitre 1")
                    .orderIndex(2)
                    .build();

            when(courseRepository.existsById(1L)).thenReturn(true);
            when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L))
                    .thenReturn(Arrays.asList(section, section2));
            when(sectionMapper.toResponse(section)).thenReturn(sectionResponse);
            when(sectionMapper.toResponse(section2)).thenReturn(response2);

            List<SectionResponse> result = sectionService.getByCourseId(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrderIndex()).isEqualTo(1);
            assertThat(result.get(1).getOrderIndex()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Tests pour update()")
    class UpdateTests {

        @Test
        @DisplayName("Devrait mettre à jour une section")
        void shouldUpdateSection() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
            when(sectionRepository.save(any(Section.class))).thenReturn(section);
            when(sectionMapper.toResponse(section)).thenReturn(sectionResponse);

            SectionResponse result = sectionService.update(1L, sectionRequest);

            assertThat(result).isNotNull();
            verify(sectionMapper).updateEntityFromRequest(sectionRequest, section);
        }
    }

    @Nested
    @DisplayName("Tests pour delete()")
    class DeleteTests {

        @Test
        @DisplayName("Devrait supprimer une section et réorganiser les index")
        void shouldDeleteAndReorderSections() {
            Section section2 = new Section();
            section2.setId(2L);
            section2.setOrderIndex(2);
            section2.setCourse(course);

            when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
            when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L))
                    .thenReturn(List.of(section2));

            sectionService.delete(1L);

            verify(sectionRepository).delete(section);
        }
    }

    @Nested
    @DisplayName("Tests pour reorder()")
    class ReorderTests {

        @Test
        @DisplayName("Devrait réorganiser les sections")
        void shouldReorderSections() {
            Section section2 = new Section();
            section2.setId(2L);
            section2.setOrderIndex(2);
            section2.setCourse(course);

            when(courseRepository.existsById(1L)).thenReturn(true);
            when(sectionRepository.findById(2L)).thenReturn(Optional.of(section2));
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
            when(sectionRepository.save(any(Section.class))).thenAnswer(i -> i.getArgument(0));
            when(sectionMapper.toResponseWithoutLessons(any(Section.class))).thenReturn(sectionResponse);

            ReorderRequest request = ReorderRequest.builder()
                    .orderedIds(Arrays.asList(2L, 1L))
                    .build();

            List<SectionResponse> result = sectionService.reorder(1L, request);

            assertThat(result).hasSize(2);
            assertThat(section2.getOrderIndex()).isEqualTo(1);
            assertThat(section.getOrderIndex()).isEqualTo(2);
        }
    }
}

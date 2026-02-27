package com.sencours.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.sencours.dto.response.CertificateResponse;
import com.sencours.entity.*;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProgressRepository progressRepository;

    @Override
    @Transactional
    public byte[] generateCertificatePdf(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new BadRequestException("Vous n'êtes pas inscrit à ce cours"));

        // Calculer la progression en temps réel
        int totalLessons = 0;
        for (com.sencours.entity.Section section : course.getSections()) {
            totalLessons += section.getLessons().size();
        }
        Long completedLessons = progressRepository.countCompletedLessonsByUserAndCourse(user.getId(), courseId);
        int progressPercentage = totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0;

        if (progressPercentage < 100) {
            throw new BadRequestException(
                    "Vous devez compléter 100% du cours pour obtenir le certificat. Progression actuelle: "
                            + progressPercentage + "%");
        }

        Certificate certificate = certificateRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseGet(() -> {
                    Certificate newCert = Certificate.builder()
                            .user(user)
                            .course(course)
                            .completionDate(enrollment.getCompletedAt() != null
                                    ? enrollment.getCompletedAt() : LocalDateTime.now())
                            .build();
                    return certificateRepository.save(newCert);
                });

        return generatePdf(certificate, user, course);
    }

    private byte[] generatePdf(Certificate certificate, User user, Course course) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4.rotate(), 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            Color primaryColor = new Color(86, 36, 208);
            Color darkColor = new Color(28, 29, 31);
            Color grayColor = new Color(106, 111, 115);

            Font titleFont = new Font(Font.HELVETICA, 36, Font.BOLD, primaryColor);
            Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, grayColor);
            Font nameFont = new Font(Font.HELVETICA, 28, Font.BOLD, darkColor);
            Font textFont = new Font(Font.HELVETICA, 14, Font.NORMAL, darkColor);
            Font courseFont = new Font(Font.HELVETICA, 20, Font.BOLD, darkColor);
            Font smallFont = new Font(Font.HELVETICA, 10, Font.NORMAL, grayColor);

            // Bordure décorative
            PdfContentByte canvas = writer.getDirectContent();
            canvas.setColorStroke(primaryColor);
            canvas.setLineWidth(3);
            canvas.rectangle(30, 30,
                    document.getPageSize().getWidth() - 60,
                    document.getPageSize().getHeight() - 60);
            canvas.stroke();

            // Bordure intérieure
            canvas.setLineWidth(1);
            canvas.rectangle(40, 40,
                    document.getPageSize().getWidth() - 80,
                    document.getPageSize().getHeight() - 80);
            canvas.stroke();

            document.add(new Paragraph("\n\n"));

            Paragraph logo = new Paragraph("SenCours", titleFont);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);

            Paragraph subtitle = new Paragraph("CERTIFICAT DE RÉUSSITE", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingBefore(10);
            document.add(subtitle);

            document.add(new Paragraph("\n"));
            Paragraph line = new Paragraph(
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    new Font(Font.HELVETICA, 12, Font.NORMAL, primaryColor));
            line.setAlignment(Element.ALIGN_CENTER);
            document.add(line);
            document.add(new Paragraph("\n"));

            Paragraph certText = new Paragraph("Ce certificat est décerné à", textFont);
            certText.setAlignment(Element.ALIGN_CENTER);
            document.add(certText);

            Paragraph studentName = new Paragraph(
                    user.getFirstName() + " " + user.getLastName(), nameFont);
            studentName.setAlignment(Element.ALIGN_CENTER);
            studentName.setSpacingBefore(15);
            studentName.setSpacingAfter(15);
            document.add(studentName);

            Paragraph completedText = new Paragraph(
                    "pour avoir complété avec succès le cours", textFont);
            completedText.setAlignment(Element.ALIGN_CENTER);
            document.add(completedText);

            Paragraph courseName = new Paragraph(
                    "« " + course.getTitle() + " »", courseFont);
            courseName.setAlignment(Element.ALIGN_CENTER);
            courseName.setSpacingBefore(15);
            courseName.setSpacingAfter(15);
            document.add(courseName);

            String instructorFullName = course.getInstructor().getFirstName()
                    + " " + course.getInstructor().getLastName();
            Paragraph instructor = new Paragraph(
                    "Dispensé par " + instructorFullName, textFont);
            instructor.setAlignment(Element.ALIGN_CENTER);
            document.add(instructor);

            document.add(new Paragraph("\n"));
            document.add(line);
            document.add(new Paragraph("\n\n"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "dd MMMM yyyy", java.util.Locale.FRENCH);
            String dateStr = certificate.getCompletionDate().format(formatter);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell dateCell = new PdfPCell(
                    new Phrase("Date de complétion\n" + dateStr, smallFont));
            dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setPaddingTop(10);

            PdfPCell certNumCell = new PdfPCell(
                    new Phrase("Numéro de certificat\n" + certificate.getCertificateNumber(), smallFont));
            certNumCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            certNumCell.setBorder(Rectangle.NO_BORDER);
            certNumCell.setPaddingTop(10);

            table.addCell(dateCell);
            table.addCell(certNumCell);
            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph verifyText = new Paragraph(
                    "Vérifiez ce certificat sur : sencours.sn/verify/" + certificate.getCertificateNumber(),
                    smallFont);
            verifyText.setAlignment(Element.ALIGN_CENTER);
            document.add(verifyText);

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du certificat PDF", e);
        }

        return baos.toByteArray();
    }

    @Override
    public CertificateResponse getCertificate(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Certificate certificate = certificateRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificat non trouvé"));

        return mapToResponse(certificate);
    }

    @Override
    public List<CertificateResponse> getMyCertificates(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return certificateRepository.findByUserIdOrderByIssuedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CertificateResponse verifyCertificate(String certificateNumber) {
        Certificate certificate = certificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Certificat non trouvé ou invalide"));

        return mapToResponse(certificate);
    }

    private CertificateResponse mapToResponse(Certificate certificate) {
        Course course = certificate.getCourse();
        User user = certificate.getUser();

        return CertificateResponse.builder()
                .id(certificate.getId())
                .certificateNumber(certificate.getCertificateNumber())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseThumbnail(course.getThumbnailUrl())
                .instructorName(course.getInstructor().getFirstName()
                        + " " + course.getInstructor().getLastName())
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .issuedAt(certificate.getIssuedAt())
                .completionDate(certificate.getCompletionDate())
                .build();
    }
}

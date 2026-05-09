package com.hpc.jobagent.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.hpc.jobagent.config.AgentProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ResumeFileExtractorTest {

    private final ResumeFileExtractor extractor = new ResumeFileExtractor(new AgentProperties());

    @Test
    void extractsTextFromPdf() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 720);
                content.showText("Java Spring Boot Redis backend internship");
                content.endText();
            }
            document.save(output);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                output.toByteArray()
        );

        assertThat(extractor.extract(file)).contains("Java Spring Boot Redis");
    }

    @Test
    void extractsTextFromDocx() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph()
                    .createRun()
                    .setText("Java 后端开发实习，熟悉 Spring Boot、MyBatis 和 Redis");
            document.write(output);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                output.toByteArray()
        );

        assertThat(extractor.extract(file)).contains("Spring Boot", "MyBatis", "Redis");
    }
}

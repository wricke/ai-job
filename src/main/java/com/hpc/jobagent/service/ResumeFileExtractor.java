package com.hpc.jobagent.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hpc.jobagent.config.AgentProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeFileExtractor {

    private final AgentProperties properties;

    public ResumeFileExtractor(AgentProperties properties) {
        this.properties = properties;
    }

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先选择一个简历文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extension(filename);
        if (extension.isBlank()) {
            extension = extensionFromContentType(file.getContentType());
        }
        try {
            String text = switch (extension) {
                case "pdf" -> extractPdf(file);
                case "docx" -> extractDocx(file);
                case "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
                case "png", "jpg", "jpeg", "bmp", "tif", "tiff" -> extractImage(file, extension);
                default -> throw new IllegalArgumentException("暂不支持该文件类型：" + extension + "，请上传 PDF、DOCX、TXT 或图片");
            };
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("没有从文件中解析到文字，请确认文件不是扫描件或空文件");
            }
            return normalize(text);
        } catch (IOException ex) {
            throw new IllegalArgumentException("简历文件解析失败：" + ex.getMessage(), ex);
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<String> parts = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (!paragraph.getText().isBlank()) {
                    parts.add(paragraph.getText());
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        if (!cell.getText().isBlank()) {
                            parts.add(cell.getText());
                        }
                    }
                }
            }
            return String.join("\n", parts);
        }
    }

    private String extractImage(MultipartFile file, String extension) throws IOException {
        Path imagePath = Files.createTempFile("resume-image-", "." + extension);
        Path outputBase = Files.createTempFile("resume-ocr-", "");
        Files.deleteIfExists(outputBase);
        try {
            Files.write(imagePath, file.getBytes());
            ProcessResult result = runTesseract(imagePath, outputBase, properties.getOcr().getLanguages());
            if (result.exitCode() != 0 && !properties.getOcr().getLanguages().equals("eng")) {
                result = runTesseract(imagePath, outputBase, "eng");
            }
            if (result.exitCode() != 0) {
                throw new IllegalArgumentException("图片 OCR 失败：" + result.stderr());
            }
            Path textPath = Path.of(outputBase + ".txt");
            return Files.exists(textPath) ? Files.readString(textPath, StandardCharsets.UTF_8) : "";
        } catch (IOException ex) {
            if (isCommandMissing(ex)) {
                throw new IllegalArgumentException("当前电脑未安装 Tesseract OCR，图片简历暂不能解析。PDF 和 DOCX 可以直接上传。", ex);
            }
            throw ex;
        } finally {
            Files.deleteIfExists(imagePath);
            Files.deleteIfExists(Path.of(outputBase + ".txt"));
            Files.deleteIfExists(outputBase);
        }
    }

    private ProcessResult runTesseract(Path imagePath, Path outputBase, String languages) throws IOException {
        List<String> command = List.of(
                properties.getOcr().getCommand(),
                imagePath.toString(),
                outputBase.toString(),
                "-l",
                languages,
                "--psm",
                "6"
        );
        Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
        try {
            byte[] stderr = process.getErrorStream().readAllBytes();
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, new String(stderr, StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图片 OCR 被中断", ex);
        }
    }

    private boolean isCommandMissing(IOException ex) {
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("cannot run program");
    }

    private String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.equals("application/pdf")) {
            return "pdf";
        }
        if (normalized.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return "docx";
        }
        if (normalized.startsWith("image/")) {
            return normalized.substring("image/".length()).replace("jpeg", "jpg");
        }
        return "";
    }

    private record ProcessResult(int exitCode, String stderr) {
    }
}

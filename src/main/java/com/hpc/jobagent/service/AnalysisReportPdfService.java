package com.hpc.jobagent.service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpc.jobagent.domain.AnalysisStatus;
import com.hpc.jobagent.domain.AnalysisTask;
import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

@Service
public class AnalysisReportPdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float MARGIN = 48;
    private static final float LINE_HEIGHT = 17;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
    };

    private final AnalysisService analysisService;
    private final ResumeService resumeService;
    private final JobPostingService jobPostingService;
    private final ObjectMapper objectMapper;

    public AnalysisReportPdfService(AnalysisService analysisService,
                                    ResumeService resumeService,
                                    JobPostingService jobPostingService,
                                    ObjectMapper objectMapper) {
        this.analysisService = analysisService;
        this.resumeService = resumeService;
        this.jobPostingService = jobPostingService;
        this.objectMapper = objectMapper;
    }

    public byte[] generate(Long analysisId) {
        AnalysisTask task = analysisService.getEntity(analysisId);
        if (task.getStatus() != AnalysisStatus.COMPLETED) {
            throw new IllegalArgumentException("分析完成后才能下载 PDF 报告");
        }
        ResumeProfile resume = resumeService.getEntityForUser(task.getResumeId(), task.getUserId());
        JobPosting job = jobPostingService.getEntityForUser(task.getJobId(), task.getUserId());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             FontResource fontResource = loadFont(document)) {
            PdfWriter writer = new PdfWriter(document, fontResource.font());
            writeReport(writer, task, resume, job);
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成 PDF 报告失败：" + ex.getMessage(), ex);
        }
    }

    private void writeReport(PdfWriter writer, AnalysisTask task, ResumeProfile resume, JobPosting job) throws IOException {
        Map<String, Object> resumeInsight = jsonMap(task.getResumeInsight());
        Map<String, Object> jobInsight = jsonMap(task.getJobInsight());
        Map<String, Object> matchDetail = jsonMap(task.getMatchDetail());
        Map<String, Object> suggestions = jsonMap(task.getSuggestions());
        Map<String, Object> interview = jsonMap(task.getInterviewQuestions());
        List<Map<String, Object>> trace = jsonList(task.getAgentTrace());

        writer.title("职途雷达 AI 求职分析报告");
        writer.muted("报告生成时间：" + format(LocalDateTime.now()));
        writer.gap(6);

        writer.section("一、报告概览");
        writer.kv("分析任务 ID", String.valueOf(task.getId()));
        writer.kv("分析状态", "已完成");
        writer.kv("匹配分数", task.getMatchScore() == null ? "-" : task.getMatchScore() + " 分");
        writer.kv("完成时间", format(task.getCompletedAt()));
        writer.kv("简历名称", resume.getTitle());
        writer.kv("候选人", blankToDash(resume.getOwnerName()));
        writer.kv("目标岗位", blankToDash(resume.getTargetRole()));
        writer.kv("分析岗位", job.getTitle());
        writer.kv("岗位来源", blankToDash(job.getSource()));
        writer.paragraph(blankToDash(task.getSummary()));

        writer.section("二、简历能力提取");
        writer.list("技能提取", list(resumeInsight, "skills"));
        writer.list("项目经历信号", list(resumeInsight, "projects"));
        writer.list("优势能力", list(resumeInsight, "strengths"));
        writer.paragraph(str(resumeInsight, "aiSummary", "ai_summary"));

        writer.section("三、岗位要求解析");
        writer.list("核心要求", list(jobInsight, "requiredSkills", "required_skills"));
        writer.list("岗位职责", list(jobInsight, "responsibilities"));
        writer.list("加分项", list(jobInsight, "bonusItems", "bonus_items"));
        writer.paragraph(str(jobInsight, "aiSummary", "ai_summary"));

        writer.section("四、匹配评分与能力短板");
        writer.kv("匹配分数", value(matchDetail, "score", task.getMatchScore()));
        writer.paragraph(str(matchDetail, "reason"));
        writer.list("已匹配技能", list(matchDetail, "matchedSkills", "matched_skills"));
        writer.list("待补强技能", list(matchDetail, "missingSkills", "missing_skills"));
        writer.list("投递风险", list(matchDetail, "risks"));

        writer.section("五、简历优化建议");
        writer.list("简历表达优化", list(suggestions, "resumeImprovements", "resume_improvements"));
        writer.list("项目经历改写", list(suggestions, "projectRewriteTips", "project_rewrite_tips"));
        writer.list("学习计划", list(suggestions, "learningPlan", "learning_plan"));
        writer.paragraph(str(suggestions, "aiAdvice", "ai_advice"));

        writer.section("六、面试准备");
        writer.list("面试问题", list(interview, "questions"));
        writer.list("回答要点", list(interview, "talkingPoints", "talking_points"));
        writer.paragraph(str(interview, "aiAdvice", "ai_advice"));

        writer.section("七、执行过程");
        if (trace.isEmpty()) {
            writer.paragraph("暂无执行过程记录。");
            return;
        }
        for (Map<String, Object> step : trace) {
            String line = "%s / %s / %s ms".formatted(
                    stepTitle(str(step, "agentName", "agent")),
                    blankToDash(str(step, "status")),
                    blankToDash(value(step, "durationMs", value(step, "duration_ms", "-")))
            );
            writer.bullet(line);
            writer.paragraph(str(step, "detail"));
        }
    }

    private Map<String, Object> jsonMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (IOException ex) {
            return Map.of("raw", value);
        }
    }

    private List<Map<String, Object>> jsonList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, LIST_MAP_TYPE);
        } catch (IOException ex) {
            return List.of(Map.of("detail", value));
        }
    }

    private List<String> list(Map<String, Object> source, String... keys) {
        Object value = first(source, keys);
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(item -> item == null ? "" : String.valueOf(item))
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        if (value instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }

    private String str(Map<String, Object> source, String... keys) {
        Object value = first(source, keys);
        return value == null ? "" : String.valueOf(value);
    }

    private String value(Map<String, Object> source, String key, Object fallback) {
        Object value = source.get(key);
        return value == null ? String.valueOf(fallback) : String.valueOf(value);
    }

    private Object first(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                return source.get(key);
            }
        }
        return null;
    }

    private String stepTitle(String value) {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("ResumeParserAgent", "简历解析");
        names.put("JobAnalyzerAgent", "岗位解析");
        names.put("MatchScoringAgent", "匹配评分");
        names.put("SuggestionAgent", "优化建议");
        names.put("InterviewAgent", "面试准备");
        names.put("ProfileAnalysisAgent", "简历与岗位分析");
        names.put("MatchEvaluationAgent", "匹配评估");
        names.put("CareerAdviceAgent", "建议与面试准备");
        return names.getOrDefault(value, blankToDash(value));
    }

    private String format(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.strip();
    }

    private FontResource loadFont(PDDocument document) throws IOException {
        List<String> fontPaths = List.of(
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        );
        for (String path : fontPaths) {
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }
            if (path.endsWith(".ttc")) {
                FontResource font = loadFromCollection(document, file);
                if (font != null) {
                    return font;
                }
            } else {
                return new FontResource(PDType0Font.load(document, file), null);
            }
        }
        throw new IOException("未找到可用于生成中文 PDF 的字体");
    }

    private FontResource loadFromCollection(PDDocument document, File file) throws IOException {
        TrueTypeCollection collection = new TrueTypeCollection(file);
        FontHolder holder = new FontHolder();
        try {
            collection.processAllFonts(ttf -> {
                if (holder.font == null && hasGlyfTable(ttf)) {
                    holder.font = PDType0Font.load(document, ttf, true);
                }
            });
            if (holder.font == null) {
                collection.close();
                return null;
            }
            return new FontResource(holder.font, collection);
        } catch (IOException | RuntimeException ex) {
            collection.close();
            throw ex;
        }
    }

    private boolean hasGlyfTable(TrueTypeFont font) {
        try {
            return font.getGlyph() != null;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    private static final class FontHolder {
        private PDFont font;
    }

    private record FontResource(PDFont font, Closeable closeable) implements Closeable {
        @Override
        public void close() throws IOException {
            if (closeable != null) {
                closeable.close();
            }
        }
    }

    private static final class PdfWriter {
        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private PdfWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void title(String text) throws IOException {
            write(text, 20, 24);
        }

        private void section(String text) throws IOException {
            gap(10);
            write(text, 15, 20);
        }

        private void muted(String text) throws IOException {
            write(text, 10, 14);
        }

        private void kv(String key, String value) throws IOException {
            write(key + "：" + value, 11, LINE_HEIGHT);
        }

        private void paragraph(String text) throws IOException {
            if (text == null || text.isBlank() || "-".equals(text)) {
                return;
            }
            write(text, 11, LINE_HEIGHT);
        }

        private void list(String title, List<String> values) throws IOException {
            write(title + "：", 11, LINE_HEIGHT);
            if (values == null || values.isEmpty()) {
                bullet("暂无");
                return;
            }
            for (String value : values) {
                bullet(value);
            }
        }

        private void bullet(String text) throws IOException {
            write("- " + text, 11, LINE_HEIGHT);
        }

        private void gap(float amount) throws IOException {
            ensureSpace(amount);
            y -= amount;
        }

        private void write(String text, float size, float lineHeight) throws IOException {
            for (String line : wrap(clean(text), size)) {
                ensureSpace(lineHeight);
                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(line);
                stream.endText();
                y -= lineHeight;
            }
        }

        private List<String> wrap(String text, float size) throws IOException {
            List<String> lines = new ArrayList<>();
            float width = page.getMediaBox().getWidth() - MARGIN * 2;
            for (String paragraph : text.split("\\R", -1)) {
                if (paragraph.isBlank()) {
                    lines.add("");
                    continue;
                }
                StringBuilder line = new StringBuilder();
                int[] codePoints = paragraph.codePoints().toArray();
                for (int codePoint : codePoints) {
                    String next = new String(Character.toChars(codePoint));
                    String candidate = line + next;
                    if (!line.isEmpty() && textWidth(candidate, size) > width) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    line.append(next);
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
            }
            return lines;
        }

        private float textWidth(String value, float size) throws IOException {
            return font.getStringWidth(value) / 1000f * size;
        }

        private String clean(String value) throws IOException {
            if (value == null) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            int[] codePoints = stripMarkdown(value).codePoints().toArray();
            for (int codePoint : codePoints) {
                appendPdfSafe(result, codePoint);
            }
            return result.toString().replace('\t', ' ');
        }

        private String stripMarkdown(String value) {
            return value.replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .replaceAll("```[a-zA-Z0-9_-]*\\n?", "")
                    .replace("```", "")
                    .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                    .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                    .replaceAll("__([^_]+)__", "$1")
                    .replace("**", "")
                    .replaceAll("#{3,}", "")
                    .strip();
        }

        private void appendPdfSafe(StringBuilder result, int codePoint) throws IOException {
            if (codePoint == '\n' || codePoint == '\t') {
                result.appendCodePoint(codePoint);
                return;
            }
            if (Character.isISOControl(codePoint) || isEmojiControl(codePoint)) {
                return;
            }
            String replacement = replacementFor(codePoint);
            if (replacement == null) {
                replacement = new String(Character.toChars(codePoint));
            }
            if (canEncode(replacement)) {
                result.append(replacement);
                return;
            }
            for (int nestedCodePoint : replacement.codePoints().toArray()) {
                String nested = new String(Character.toChars(nestedCodePoint));
                result.append(canEncode(nested) ? nested : " ");
            }
        }

        private boolean canEncode(String value) throws IOException {
            try {
                font.encode(value);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        private boolean isEmojiControl(int codePoint) {
            return codePoint == 0x200D
                    || codePoint == 0x20E3
                    || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                    || (codePoint >= 0xE0100 && codePoint <= 0xE01EF)
                    || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF);
        }

        private String replacementFor(int codePoint) {
            return switch (codePoint) {
                case 0x2705, 0x2713, 0x2714, 0x2611 -> "[完成]";
                case 0x274C, 0x274E, 0x2716, 0x2717 -> "[失败]";
                case 0x26A0 -> "[警告]";
                case 0x1F4A1 -> "[提示]";
                case 0x1F680 -> "[启动]";
                case 0x1F525 -> "[重点]";
                default -> codePoint > 0xFFFF ? " " : null;
            };
        }

        private void ensureSpace(float amount) throws IOException {
            if (y - amount < MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}

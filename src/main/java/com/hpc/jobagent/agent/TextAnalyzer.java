package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TextAnalyzer {

    private static final int MAX_SKILLS = 18;
    private static final Pattern LATIN_TOKEN = Pattern.compile("(?<![A-Za-z0-9+#.-])([A-Za-z0-9][A-Za-z0-9+#.-]*)(?![A-Za-z0-9+#.-])");
    private static final List<String> SKILL_ANCHORS = List.of(
            "目标岗位", "求职方向", "目标方向", "目标", "主攻", "技能", "能力", "工具",
            "熟悉", "掌握", "了解", "使用", "运用", "擅长", "具备", "负责", "参与"
    );
    private static final List<String> PROJECT_CUES = List.of("项目", "作品", "实践", "案例", "经历", "负责", "参与");
    private static final List<String> RESPONSIBILITY_CUES = List.of("岗位职责", "职责", "负责", "参与", "协助", "推进", "完成");
    private static final List<String> BONUS_CUES = List.of("优先", "加分", "熟悉", "了解", "经验", "具备");
    private static final Set<String> GENERIC_WORDS = Set.of(
            "目标", "目标岗位", "求职方向", "目标方向", "技能", "能力", "工具", "项目", "作品", "实践",
            "经历", "实习", "实习生", "岗位", "方向", "负责", "参与", "熟悉", "掌握", "了解", "使用",
            "运用", "具备", "擅长", "做", "和", "或", "及", "与", "以及", "and", "or", "the", "for",
            "with", "from", "this", "that", "每周"
    );

    public List<String> findSkills(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String sentence : splitSentences(text)) {
            for (String segment : anchoredSegments(sentence, SKILL_ANCHORS)) {
                for (String candidate : splitCandidates(segment)) {
                    addSkill(result, candidate);
                    if (result.size() >= MAX_SKILLS) {
                        return new ArrayList<>(result);
                    }
                }
            }
        }
        addLatinSignals(result, text);
        return new ArrayList<>(result);
    }

    public List<String> extractProjectSignals(String text) {
        return pickSentences(text, PROJECT_CUES, 6);
    }

    public List<String> extractResponsibilities(String text) {
        return pickSentences(text, RESPONSIBILITY_CUES, 6);
    }

    public List<String> extractBonusItems(String text) {
        return pickSentences(text, BONUS_CUES, 5);
    }

    private List<String> pickSentences(String text, List<String> keywords, int limit) {
        List<String> sentences = splitSentences(text);
        List<String> picked = new ArrayList<>();
        for (String sentence : sentences) {
            String normalized = normalize(sentence);
            for (String keyword : keywords) {
                if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    picked.add(trim(sentence, 120));
                    break;
                }
            }
            if (picked.size() >= limit) {
                break;
            }
        }
        return picked;
    }

    private List<String> splitSentences(String text) {
        String[] parts = (text == null ? "" : text).replace("\r", "\n").split("[\\n。；;]");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String value = part.strip();
            if (!value.isBlank()) {
                sentences.add(value);
            }
        }
        return sentences;
    }

    private List<String> anchoredSegments(String sentence, List<String> anchors) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        List<String> segments = new ArrayList<>();
        for (String anchor : anchors) {
            if ("目标".equals(anchor) && (lower.contains("目标岗位") || lower.contains("目标方向"))) {
                continue;
            }
            int index = lower.indexOf(anchor.toLowerCase(Locale.ROOT));
            if (index >= 0) {
                int start = index + anchor.length();
                String segment = sentence.substring(Math.min(start, sentence.length()));
                segment = segment.replaceFirst("^[：:是为\\s]+", "");
                if (!segment.isBlank()) {
                    segments.add(segment);
                }
            }
        }
        return segments;
    }

    private List<String> splitCandidates(String segment) {
        String value = segment
                .replace("（", "(")
                .replace("）", ")")
                .replaceAll("[()]", " ")
                .replaceAll("等(能力|工具|技能|经验)?", " ");
        String[] parts = value.split("[,，、]|\\s+和\\s+|和|以及|及|与|并|或");
        List<String> candidates = new ArrayList<>();
        for (String part : parts) {
            String cleaned = cleanCandidate(part);
            if (!cleaned.isBlank()) {
                candidates.add(cleaned);
            }
        }
        return candidates;
    }

    private void addSkill(Set<String> result, String candidate) {
        String value = cleanCandidate(candidate);
        if (value.isBlank()) {
            return;
        }
        if (value.contains("/") || value.contains("／")) {
            for (String expanded : expandSlashCompound(value)) {
                addSkill(result, expanded);
            }
            return;
        }
        if (isUsefulCandidate(value)) {
            result.add(value);
        }
    }

    private List<String> expandSlashCompound(String value) {
        String[] parts = value.replace("／", "/").split("/");
        if (parts.length < 2) {
            return List.of(value);
        }
        String suffix = commonChineseSuffix(parts[parts.length - 1]);
        List<String> expanded = new ArrayList<>();
        for (String part : parts) {
            String item = cleanCandidate(part);
            if (!suffix.isBlank() && !item.endsWith(suffix)) {
                item = item + suffix;
            }
            if (!item.isBlank()) {
                expanded.add(item);
            }
        }
        return expanded;
    }

    private String commonChineseSuffix(String value) {
        String cleaned = value == null ? "" : value.strip();
        int length = cleaned.length();
        if (length >= 3 && isCjk(cleaned.charAt(length - 1)) && isCjk(cleaned.charAt(length - 2))) {
            return cleaned.substring(length - 2);
        }
        return "";
    }

    private boolean isCjk(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block)
                || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)
                || Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block);
    }

    private void addLatinSignals(Set<String> result, String text) {
        List<Token> tokens = latinTokens(text);
        String source = text == null ? "" : text;
        for (int i = 0; i < tokens.size() - 1 && result.size() < MAX_SKILLS; i++) {
            Token left = tokens.get(i);
            Token right = tokens.get(i + 1);
            if (left.separatedByWhitespaceBefore(right, source) && looksLikeLatinPhrase(left.value(), right.value())) {
                addSkill(result, left.value() + " " + right.value());
            }
        }
        for (Token token : tokens) {
            if (result.size() >= MAX_SKILLS) {
                break;
            }
            addSkill(result, token.value());
        }
    }

    private List<Token> latinTokens(String text) {
        Matcher matcher = LATIN_TOKEN.matcher(text == null ? "" : text);
        List<Token> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(new Token(matcher.group(1), matcher.start(1), matcher.end(1)));
        }
        return tokens;
    }

    private boolean looksLikeLatinPhrase(String left, String right) {
        return hasUppercase(left)
                || hasUppercase(right)
                || hasDigit(left)
                || hasDigit(right)
                || right.length() <= 3;
    }

    private boolean hasUppercase(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isUpperCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private String cleanCandidate(String value) {
        String cleaned = (value == null ? "" : value).strip()
                .replaceAll("^(目标岗位|求职方向|目标方向|目标|岗位|主攻|技能|能力|工具|熟悉|掌握|了解|使用|运用|擅长|具备|负责|参与)[：:为是\\s]*", "")
                .replaceAll("(实习生?|校招|岗位|职位)$", "")
                .replaceAll("^(做|进行|完成|输出|负责|参与)[\\s]*", "")
                .replaceAll("[。；;，,、]+$", "")
                .strip();
        int actionIndex = firstActionIndex(cleaned);
        if (actionIndex > 1) {
            cleaned = cleaned.substring(0, actionIndex).strip();
        }
        return trim(cleaned, 40);
    }

    private int firstActionIndex(String value) {
        int index = -1;
        for (String marker : List.of(" 做", "进行", "完成", "输出", "负责", "参与", "项目经历", "项目", "作品")) {
            int current = value.indexOf(marker);
            if (current > 0 && (index < 0 || current < index)) {
                index = current;
            }
        }
        return index;
    }

    private boolean isUsefulCandidate(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.length() < 2 || GENERIC_WORDS.contains(value) || GENERIC_WORDS.contains(lower)) {
            return false;
        }
        if (value.matches("\\d+")) {
            return false;
        }
        return value.length() <= 40;
    }

    private String normalize(String text) {
        return (" " + (text == null ? "" : text) + " ").toLowerCase(Locale.ROOT);
    }

    private String trim(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record Token(String value, int start, int end) {

        boolean separatedByWhitespaceBefore(Token other, String source) {
            if (other.start <= end) {
                return false;
            }
            String separator = source.substring(end, other.start);
            return !separator.isEmpty() && separator.strip().isEmpty();
        }
    }
}

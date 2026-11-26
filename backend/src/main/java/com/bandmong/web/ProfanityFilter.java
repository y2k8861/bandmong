package com.bandmong.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 욕설 거르기 (애플 심사 가이드라인 1.2: 사용자 글이 있는 앱은 부적절한 내용을 거르는 장치가 있어야 한다).
 *
 * 띄어쓰기는 낱말 경계로 둔다. "다시 발표"를 "시발"로 잘못 잡지 않기 위해서다.
 * 대신 한 글자씩 띄운 "씨 발", 기호를 끼운 "씨.발", 숫자를 끼운 "씨1발"은 붙여서 검사한다.
 * 목록은 resources/moderation/*.txt — 완벽하지 않으니 신고·차단과 함께 쓴다.
 */
@Component
public class ProfanityFilter {

    public static final String REJECT_MESSAGE = "부적절한 표현이 들어 있어요. 고쳐서 다시 적어 주세요";

    private final List<String> banned;
    private final List<String> allowed;

    public ProfanityFilter() {
        this(load("moderation/banned-words.txt"), load("moderation/allowed-words.txt"));
    }

    ProfanityFilter(List<String> banned, List<String> allowed) {
        this.banned = banned;
        this.allowed = allowed;
    }

    public boolean contains(String text) {
        return text != null && !ranges(text).isEmpty();
    }

    /** 걸리면 400 */
    public void check(String text) {
        if (contains(text)) throw new ApiException(HttpStatus.BAD_REQUEST, REJECT_MESSAGE);
    }

    /** 걸린 글자만 *로. 띄어쓰기·기호는 그대로 둔다. */
    public String mask(String text) {
        if (text == null) return null;
        List<int[]> hits = ranges(text);
        if (hits.isEmpty()) return text;
        char[] out = text.toCharArray();
        for (int[] positions : hits) {
            for (int p : positions) out[p] = '*';
        }
        return new String(out);
    }

    /** 걸린 부분마다, 원문에서 가릴 글자 위치들 */
    private List<int[]> ranges(String text) {
        Normalized n = normalize(text);
        String t = n.text;
        boolean[] safe = new boolean[t.length()];
        for (String a : allowed) {
            for (int i = t.indexOf(a); i >= 0; i = t.indexOf(a, i + 1)) {
                Arrays.fill(safe, i, i + a.length(), true);
            }
        }
        List<int[]> hits = new ArrayList<>();
        for (String b : banned) {
            for (int i = t.indexOf(b); i >= 0; i = t.indexOf(b, i + 1)) {
                boolean allSafe = true;
                for (int k = i; k < i + b.length(); k++) allSafe &= safe[k];
                if (allSafe) continue;
                int[] pos = new int[b.length()];
                for (int k = 0; k < b.length(); k++) pos[k] = n.index[i + k];
                hits.add(pos);
            }
        }
        return hits;
    }

    private record Normalized(String text, int[] index) {
    }

    /**
     * 글자(한글·영문)만 남기고 소문자로. 띄어쓰기는 한 칸으로 남기되,
     * 한 글자짜리 토막끼리 이어진 곳("씨 발")은 띄어쓰기를 지워 붙인다.
     */
    static Normalized normalize(String s) {
        // 1) 글자와 띄어쓰기만, 원래 위치 기억 (기호·숫자는 건너뛰어 "씨.발", "씨1발"을 붙인다)
        StringBuilder a = new StringBuilder();
        List<Integer> ai = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                a.append(Character.toLowerCase(c));
                ai.add(i);
            } else if (Character.isWhitespace(c) && a.length() > 0 && a.charAt(a.length() - 1) != ' ') {
                a.append(' ');
                ai.add(-1);
            }
        }
        // 2) 토막으로 나눠, 한 글자 토막이 이어지면 붙인다
        String[] tokens = a.toString().trim().split(" ");
        StringBuilder out = new StringBuilder();
        List<Integer> oi = new ArrayList<>();
        int cursor = 0;
        for (int t = 0; t < tokens.length; t++) {
            String tok = tokens[t];
            if (tok.isEmpty()) continue;
            while (cursor < a.length() && a.charAt(cursor) == ' ') cursor++;
            boolean glue = t > 0 && tok.length() == 1 && tokens[t - 1].length() == 1;
            if (out.length() > 0 && !glue) {
                out.append(' ');
                oi.add(-1);
            }
            for (int k = 0; k < tok.length(); k++) {
                out.append(tok.charAt(k));
                oi.add(ai.get(cursor + k));
            }
            cursor += tok.length();
        }
        return new Normalized(out.toString(), oi.stream().mapToInt(Integer::intValue).toArray());
    }

    private static List<String> load(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(String::toLowerCase).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

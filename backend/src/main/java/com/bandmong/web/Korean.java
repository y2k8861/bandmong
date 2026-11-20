package com.bandmong.web;

/** 받침에 따라 조사를 고른다: 드럼으로 / 기타로 / 보컬로(ㄹ 받침은 '로') */
public final class Korean {

    private Korean() {
    }

    private static int jong(String word) {
        if (word == null || word.isEmpty()) return 0;
        char c = word.charAt(word.length() - 1);
        if (c < 0xAC00 || c > 0xD7A3) return 0;
        return (c - 0xAC00) % 28;
    }

    /** (으)로 */
    public static String ro(String word) {
        int j = jong(word);
        return word + (j == 0 || j == 8 ? "로" : "으로");
    }

    /** 이/가 */
    public static String iga(String word) {
        return word + (jong(word) == 0 ? "가" : "이");
    }
}

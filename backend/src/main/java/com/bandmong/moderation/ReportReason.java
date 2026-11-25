package com.bandmong.moderation;

public enum ReportReason {
    ABUSE("욕설·비하"),
    SPAM("광고·도배"),
    NO_SHOW("합주 노쇼·잠수"),
    FAKE("거짓 정보"),
    SEXUAL("성적인 내용"),
    OTHER("기타");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

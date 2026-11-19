package com.bandmong.user;

/** 맡는 악기. JSON에는 이름(VOCAL…)으로 오가고, label은 서버 안내 문구에 쓴다. */
public enum Instrument {
    VOCAL("보컬"), GUITAR("기타"), BASS("베이스"), DRUM("드럼"), KEYS("키보드"), ETC("기타 악기");

    private final String label;

    Instrument(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

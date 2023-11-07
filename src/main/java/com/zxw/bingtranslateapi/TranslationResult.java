package com.zxw.bingtranslateapi;

import lombok.Data;

public class TranslationResult {

    private String text;
    private String userLang;
    private String translation;
    private String correctedText;
    private LanguageInfo languageInfo;
    private String rawResponse;

    @Data
    public static class LanguageInfo {
        private String from;
        private String to;
        private Double score;
    }

    public static TranslationResult empty() {
        TranslationResult result = new TranslationResult();
        return result;
    }
}

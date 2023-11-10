package com.zxw.bingtranslateapi;

import lombok.Builder;
import lombok.Data;

@Data
public class TranslationResult {

    private String text;
    private String translation;
    private LanguageInfo languageInfo;
    private String rawResponse;

    @Data
    @Builder
    public static class LanguageInfo {
        private String from;
        private String to;
        private Double score;
    }
}

package com.zxw.bingtranslateapi;

import lombok.Data;

import java.util.List;

@Data
public class RawTranslationResponse {

    private DetectedLanguage detectedLanguage;
    private List<Translation> translations;

    @Data
    public static class DetectedLanguage {
        private String language;
        private Double score;
    }

    @Data
    public static class Translation {
        private String text;
        private String to;
        private SentLen sentLen;

        @Data
        public static class SentLen {
            private Integer[] srcSentLen;
            private Integer[] transSentLen;
        }
    }
}

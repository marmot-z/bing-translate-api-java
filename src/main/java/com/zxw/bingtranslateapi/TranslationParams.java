package com.zxw.bingtranslateapi;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslationParams {
    private String text;
    private String fromLang = Languages.DEFAULT_FROM_LANG;
    private String toLang = Languages.DEFAULT_TO_LANG;
    private String userAgent;
}

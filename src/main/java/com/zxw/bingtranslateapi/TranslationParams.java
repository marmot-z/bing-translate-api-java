package com.zxw.bingtranslateapi;

import lombok.Data;

@Data
public class TranslationParams {
    private String text;
    private String fromLang = Languages.DEFAULT_FROM_LANG;
    private String toLang = Languages.DEFAULT_TO_LANG;
    private boolean correct;
    private boolean showRaw;
    private String userAgent;
}

package com.zxw.bingtranslateapi.exception;

public class TranslationConfigLoadException extends RuntimeException {

    public TranslationConfigLoadException(String message) {
        super(message);
    }

    public TranslationConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

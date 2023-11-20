package com.zxw.bingtranslateapi.exception;

public class TranslateException extends RuntimeException {

    public TranslateException(String message) {
        super(message);
    }

    public TranslateException(String message, Throwable cause) {
        super(message, cause);
    }
}

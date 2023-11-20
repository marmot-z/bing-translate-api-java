package com.zxw.bingtranslateapi;

import com.zxw.bingtranslateapi.entity.TranslationParams;
import com.zxw.bingtranslateapi.entity.TranslationResult;
import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class BingTranslatorTests {

    public static void main(String[] args) throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080));
        OkHttpClient httpClient = new OkHttpClient().newBuilder()
                .proxy(proxy)
                .build();
        BingTranslator translator = new BingTranslator(httpClient);

        TranslationResult result;
        try {
            TranslationParams params = TranslationParams.builder()
                    .text("你好")
                    .fromLang(Languages.DEFAULT_FROM_LANG)
                    .toLang(Languages.DEFAULT_TO_LANG)
                    .build();
            result = translator.translate(params);

            System.out.println(result);
        } finally {
            translator.close();
        }
    }
}

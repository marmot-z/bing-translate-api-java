package com.zxw.bingtranslateapi;

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

        TranslationParams params = TranslationParams.builder()
                .text("你好")
                .fromLang(Languages.DEFAULT_FROM_LANG)
                .toLang(Languages.DEFAULT_TO_LANG)
                .build();
        TranslationResult result = translator.translate(params);

        System.out.println(result);
    }
}
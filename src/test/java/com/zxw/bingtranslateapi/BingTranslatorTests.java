package com.zxw.bingtranslateapi;

import com.zxw.bingtranslateapi.entity.TranslationParams;
import com.zxw.bingtranslateapi.entity.TranslationResult;
import com.zxw.bingtranslateapi.exception.TranslationException;
import com.zxw.bingtranslateapi.exception.TranslationConfigLoadException;
import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class BingTranslatorTests {

    public static void main(String[] args) throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080));
        OkHttpClient httpClient = new OkHttpClient().newBuilder()
                // use http proxy
                .proxy(proxy)
                .build();
        BingTranslator translator = new BingTranslator(httpClient);

        try {
            TranslationParams params = TranslationParams.builder()
                    .text("你好")
                    .fromLang("auto-detect")
                    .toLang("en")
                    .build();
            TranslationResult result = translator.translate(params);

            System.out.println(result);
        }
        // the following exception thrown when an error occurs
        // in translate (or getting translation config)
        catch (TranslationException | TranslationConfigLoadException e) {
            e.printStackTrace();
        } finally {
            translator.close();
        }
    }
}

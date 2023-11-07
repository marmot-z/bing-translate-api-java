package com.zxw.bingtranslateapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.StatusLine;

import java.io.IOException;
import java.util.List;

public class BingTranslator {

    private static final String TRANSLATE_API_ROOT = "https://{s}bing.com";
    private static final String TRANSLATE_WEBSITE = TRANSLATE_API_ROOT + "/translator";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";

    public TranslationResult translate(TranslationParams params) {
        String text = params.getText();

        if (text == null || (text = text.trim()).isBlank()) {
            return TranslationResult.empty();
        }

        GlobalConfig globalConfig = getGlobalConfig();

        if (globalConfig.isTokenExpired()) {
            refetchGlobalConfig(params.getUserAgent(), params.getProxyAgents());
            globalConfig = getGlobalConfig()
        }

        if (!Languages.isSupport(params.getFromLang()) || !Languages.isSupport(params.getToLang())) {
            throw new IllegalArgumentException("Unsupported lang, fromLang: " + params.getFromLang() + ", toLang: " + params.getToLang());
        }

        String rawResponse = doTranslateRequest(params, globalConfig);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(rawResponse);
    }

    private String doTranslateRequest(TranslationParams params, GlobalConfig globalConfig) throws IOException {
        String requestUrl = generateRequestUrl(false);
        HttpEntity body = generateRequestBody(false);

        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            final HttpPost httppost = new HttpPost(requestUrl);
            final String userAgent = params.getUserAgent() == null || params.getUserAgent().isBlank() ?
                    DEFAULT_USER_AGENT :
                    params.getToLang();

            httppost.setEntity(body);
            httppost.setHeader(HttpHeaders.USER_AGENT, userAgent);
            httppost.setHeader(HttpHeaders.REFERER, replaceSubdomain(TRANSLATE_WEBSITE, globalConfig.getSubdomian()));
            httppost.setHeader(HttpHeaders.COOKIE, globalConfig.getCookie());
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

            return httpclient.execute(httppost, response -> EntityUtils.toString(response.getEntity()));
        }
    }
}

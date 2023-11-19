package com.zxw.bingtranslateapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class BingTranslator {

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";

    private OkHttpClient httpClient;

    private TranslateConfigManager translateConfigManager;

    public BingTranslator(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.translateConfigManager = new TranslateConfigManager(httpClient);
    }

    public TranslationResult translate(TranslationParams params) throws Exception {
        String text = params.getText();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text must not blank.");
        }

        if (!Languages.isSupport(params.getFromLang()) || !Languages.isSupport(params.getToLang())) {
            throw new IllegalArgumentException("Unsupported lang, fromLang: " + params.getFromLang() + ", toLang: " + params.getToLang());
        }

        String response = doTranslateRequest(params);
        List<RawTranslationResponse> rawTranslationResponses;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            rawTranslationResponses = objectMapper.readValue(response, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Response body scheme illegal: {}", response);
            return null;
        }

        if (rawTranslationResponses.isEmpty()) {
            return null;
        }

        RawTranslationResponse translationResponse = rawTranslationResponses.get(0);
        List<RawTranslationResponse.Translation> translations = translationResponse.getTranslations();
        RawTranslationResponse.DetectedLanguage detectedLanguage = translationResponse.getDetectedLanguage();
        TranslationResult result = new TranslationResult();

        result.setRawResponse(response);
        result.setText(params.getText());
        result.setTranslation(translations.get(0).getText());
        TranslationResult.LanguageInfo languageInfo = TranslationResult.LanguageInfo
                .builder()
                .from(detectedLanguage.getLanguage())
                .to(translations.get(0).getTo())
                .score(detectedLanguage.getScore())
                .build();
        result.setLanguageInfo(languageInfo);

        return result;
    }

    private String doTranslateRequest(TranslationParams params) throws IOException {
        TranslateConfig translateConfig = translateConfigManager.getTranslateConfig();
        String requestUrl = createRequestUrl(translateConfig);
        RequestBody requestBody = createRequestBody(translateConfig, params);
        String userAgent = params.getUserAgent() == null || params.getUserAgent().isBlank() ?
                DEFAULT_USER_AGENT :
                params.getToLang();

        Request request = new Request.Builder()
                .url(requestUrl)
                .method("POST", requestBody)
                .addHeader("user-agent", userAgent)
                .addHeader("origin", translateConfigManager.getTranslateDomain())
                .addHeader("referer", translateConfigManager.getTranslatePageUrl())
                .addHeader("cookie", translateConfig.getCookie())
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();

            if (Objects.isNull(responseBody)) {
                throw new IOException("Request " + requestUrl + " body failed.");
            }

            return responseBody.string();
        }
    }

    private String createRequestUrl(TranslateConfig translateConfig) {
        return String.format("%s&&IG=%s&IID=%s", translateConfigManager.getTranslateApiUrl(), translateConfig.getIG(), translateConfig.getIID());
    }

    private RequestBody createRequestBody(TranslateConfig translateConfig, TranslationParams params) {
        Map<String, String> paramMap = new HashMap<>();

        paramMap.put("fromLang", params.getFromLang());
        paramMap.put("text", params.getText().trim());
        paramMap.put("token", translateConfig.getToken());
        paramMap.put("key", translateConfig.getKey().toString());
        paramMap.put("to", params.getToLang());
        paramMap.put("tryFetchingGenderDebiasedTranslations", "true");

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String paramString = paramMap.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        return RequestBody.create(paramString, mediaType);
    }

    public void close() {
        translateConfigManager.close();
    }
}

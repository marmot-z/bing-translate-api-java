package com.zxw.bingtranslateapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class BingTranslator {

    private static final String TRANSLATE_API_ROOT = "https://www.bing.com";
    private static final String TRANSLATE_WEBSITE = TRANSLATE_API_ROOT + "/translator";
    private static final String TRANSLATE_API = TRANSLATE_API_ROOT + "/ttranslatev3?isVertical=1";
    private static final String TRANSLATE_API_SPELL_CHECK = TRANSLATE_API_ROOT + "/tspellcheckv3?isVertical=1";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36";

    private OkHttpClient httpClient;

    private volatile GlobalConfig globalConfig;

    public BingTranslator(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public TranslationResult translate(TranslationParams params) throws Exception {
        String text = params.getText();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text must not blank.");
        }

        if (!Languages.isSupport(params.getFromLang()) || !Languages.isSupport(params.getToLang())) {
            throw new IllegalArgumentException("Unsupported lang, fromLang: " + params.getFromLang() + ", toLang: " + params.getToLang());
        }

        loadGlobalConfig();

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

    private void loadGlobalConfig() {
        if (globalConfig != null && !globalConfig.isTokenExpired()) {
                return;
        }

        reloadGlobalConfig(3);
    }

    private synchronized void reloadGlobalConfig(int retryLeft) {
        if (retryLeft <= 0) {
            throw new RuntimeException("Retry 3 times, fetch global config fail.");
        }

        Request request = new Request.Builder()
                .addHeader("user-agent", DEFAULT_USER_AGENT)
                .url(TRANSLATE_WEBSITE)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            globalConfig = new GlobalConfig();

            Pattern IGPattern = Pattern.compile("IG:\"([^\"]+)\"");
            Pattern IIDPattern = Pattern.compile("data-iid=\"([^\"]+)\"");
            Pattern paramsPattern = Pattern.compile("params_AbusePreventionHelper\\s*=\\s*\\[\\s*(\\d+),\\s*\"(.*?)\",\\s*(\\d+)\\s*\\]");
            ResponseBody responseBody = response.body();
            String bodyContent = Objects.nonNull(responseBody) ? responseBody.string() : "";

            if (bodyContent.isBlank()) {
                throw new IOException("Get " + TRANSLATE_WEBSITE + " body failed.");
            }

            Matcher IGMatcher = IGPattern.matcher(bodyContent);
            if (IGMatcher.find()) globalConfig.setIG(IGMatcher.group(1));

            Matcher IIDMatcher = IIDPattern.matcher(bodyContent);
            if (IIDMatcher.find()) globalConfig.setIID(IIDMatcher.group(1));

            Matcher paramsMatcher = paramsPattern.matcher(bodyContent);
            if (paramsMatcher.find()) {
                Long key = Long.valueOf(paramsMatcher.group(1));
                globalConfig.setKey(key);
                globalConfig.setTokenTs(key);
                globalConfig.setToken(paramsMatcher.group(2));
                globalConfig.setTokenExpiryInterval(Long.valueOf(paramsMatcher.group(3)));
            }

            Headers responseHeaders = response.headers();
            List<String> cookies = new ArrayList<>(responseHeaders.size());
            for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                if ("set-cookie".equalsIgnoreCase(responseHeaders.name(i))) {
                    String headerValue = responseHeaders.value(i);
                    cookies.add(headerValue.split(";")[0]);
                }
            }

            globalConfig.setCookie(String.join("; ", cookies));
            globalConfig.setCount(0);
        } catch (IOException e) {
            reloadGlobalConfig(--retryLeft);
            log.error("Load global config occur a error", e);
        }
    }

    private String doTranslateRequest(TranslationParams params) throws IOException {
        String requestUrl = createRequestUrl();
        RequestBody requestBody = createRequestBody(params);
        String userAgent = params.getUserAgent() == null || params.getUserAgent().isBlank() ?
                DEFAULT_USER_AGENT :
                params.getToLang();

        Request request = new Request.Builder()
                .url(requestUrl)
                .method("POST", requestBody)
                .addHeader("user-agent", userAgent)
                .addHeader("origin", TRANSLATE_API_ROOT)
                .addHeader("referer", TRANSLATE_WEBSITE)
                .addHeader("cookie", globalConfig.getCookie())
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

    private String createRequestUrl() {
        return String.format("%s&&IG=%s&IID=%s", TRANSLATE_API, globalConfig.getIG(), globalConfig.getIID());
    }

    private RequestBody createRequestBody(TranslationParams params) {
        Map<String, String> paramMap = new HashMap<>();

        paramMap.put("fromLang", params.getFromLang());
        paramMap.put("text", params.getText().trim());
        paramMap.put("token", globalConfig.getToken());
        paramMap.put("key", globalConfig.getKey().toString());
        paramMap.put("to", params.getToLang());
        paramMap.put("tryFetchingGenderDebiasedTranslations", "true");

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String paramString = paramMap.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        return RequestBody.create(paramString, mediaType);
    }
}

package com.zxw.bingtranslateapi;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TranslateConfigManager {

    private final int reloadThreshold = 1000;

    private final Lock lock = new ReentrantLock();

    private final Condition initCondition = lock.newCondition();
    private final Condition loadConfigCondition = lock.newCondition();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private final OkHttpClient okHttpClient;

    private volatile boolean inited = false;
    private volatile TranslateConfig translateConfig;
    private String translateDomain;
    private String translatePageUrl;
    private String translateApiUrl;

    public TranslateConfigManager(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;

        try {
            determineTranslateDomain();
            scheduledExecutorService.schedule(this::loadConfig, 1000, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            throw new RuntimeException("Load bing translator config failed.", e);
        }
    }

    private void determineTranslateDomain() throws IOException {
        Request request = new Request.Builder()
                .addHeader("user-agent", BingTranslator.DEFAULT_USER_AGENT)
                .url("https://bing.com/translator")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Load bing translator page failed.");
            }

            // okhttp 自动处理重定向
            String url = response.request().url().url().toString();

            lock.lock();
            try {
                translateConfig = parseTranslatorPage(response);
                translateDomain = url.substring(0, url.lastIndexOf('/'));
                translatePageUrl = url;
                translateApiUrl = translateDomain + "/ttranslatev3?isVertical=1";
                inited = true;

                initCondition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    private void loadConfig() {
        if (translateConfig != null && !isExpirationSoon(translateConfig)) {
            return;
        }

        for (int i = 0, maxRetryTimes = 3; i < maxRetryTimes; i++) {
            Request request = new Request.Builder()
                    .addHeader("user-agent", BingTranslator.DEFAULT_USER_AGENT)
                    .url(translatePageUrl)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                TranslateConfig config = parseTranslatorPage(response);

                lock.lock();
                try {
                    translateConfig = config;
                    loadConfigCondition.signalAll();
                    return;
                } finally {
                    lock.unlock();
                }
            } catch (IOException e) {
                log.error("Load bing translator failed.", e);
            }
        }

        throw new RuntimeException("Load bing translator failed, retry 3 times.");
    }

    private boolean isExpirationSoon(TranslateConfig config) {
        return new Date().getTime() - reloadThreshold - config.getTokenTs() > config.getTokenExpiryInterval();
    }

    private TranslateConfig parseTranslatorPage(Response response) throws IOException {
        Pattern IGPattern = Pattern.compile("IG:\"([^\"]+)\"");
        Pattern IIDPattern = Pattern.compile("data-iid=\"([^\"]+)\"");
        Pattern paramsPattern = Pattern.compile("params_AbusePreventionHelper\\s*=\\s*\\[\\s*(\\d+),\\s*\"(.*?)\",\\s*(\\d+)\\s*\\]");
        ResponseBody responseBody = response.body();
        String bodyContent = Objects.nonNull(responseBody) ? responseBody.string() : "";
        TranslateConfig config = new TranslateConfig();

        if (bodyContent.isBlank()) {
            throw new IOException("Bing translator page is blank.");
        }

        Matcher IGMatcher = IGPattern.matcher(bodyContent);
        if (IGMatcher.find()) config.setIG(IGMatcher.group(1));

        Matcher IIDMatcher = IIDPattern.matcher(bodyContent);
        if (IIDMatcher.find()) config.setIID(IIDMatcher.group(1));

        Matcher paramsMatcher = paramsPattern.matcher(bodyContent);
        if (paramsMatcher.find()) {
            Long key = Long.valueOf(paramsMatcher.group(1));
            config.setKey(key);
            config.setTokenTs(key);
            config.setToken(paramsMatcher.group(2));
            config.setTokenExpiryInterval(Long.valueOf(paramsMatcher.group(3)));
        }

        Headers responseHeaders = response.headers();
        List<String> cookies = new ArrayList<>(responseHeaders.size());
        for (int i = 0, size = responseHeaders.size(); i < size; i++) {
            if ("set-cookie".equalsIgnoreCase(responseHeaders.name(i))) {
                String headerValue = responseHeaders.value(i);
                cookies.add(headerValue.split(";")[0]);
            }
        }

        config.setCookie(String.join("; ", cookies));
        config.setCount(0);

        return config;
    }

    public TranslateConfig getTranslateConfig() {
        return blockGetUntilInitSuccessful(() -> {
            while (translateConfig.isTokenExpired()) {
                lock.lock();
                try {
                    loadConfigCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                } finally {
                    lock.unlock();
                }
            }

            return translateConfig;
        });
    }

    private <T> T blockGetUntilInitSuccessful(Supplier<T> supplier) {
        while (!inited) {
            lock.lock();
            try {
                initCondition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                lock.unlock();
            }
        }

        return supplier.get();
    }

    public String getTranslateDomain() {
        return blockGetUntilInitSuccessful(() -> translateDomain);
    }

    public String getTranslatePageUrl() {
        return blockGetUntilInitSuccessful(() -> translatePageUrl);
    }

    public String getTranslateApiUrl() {
        return blockGetUntilInitSuccessful(() -> translateApiUrl);
    }

    public void close() {
        scheduledExecutorService.shutdown();
        try {
            scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

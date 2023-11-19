# bing-translate-api

An API for Java applications to access the Bing Translator interface, ported from the [project](https://github.com/plainheart/bing-translate-api).

## Usage
```java
Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080));
OkHttpClient httpClient = new OkHttpClient().newBuilder()
        // use http proxy
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
```

## Feature
- Multi-thread supported
- Auto renewable translate config

Notes:
1. When the response code is 409, it indicates that the current call frequency is too high.  
   Since this SDK is not for commercial-grade paid APIs, its supported call frequency is limited. Clients need to control the sending frequency appropriately (or use a proxy for access) to avoid the risk of IP banning.

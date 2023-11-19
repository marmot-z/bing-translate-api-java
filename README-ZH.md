# bing-translate-api

一款适用于 java 语言调用 Bing Translator 翻译接口的 API，由[项目](https://github.com/plainheart/bing-translate-api)移植而来。

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

## 特性
- 支持多线程
- 翻译配置自动刷新

注意项：
1. 当响应码为 409 时，代表当前调用频率过于频繁  
   由于该 sdk 不是商业级付费的 API，其支持的调用频率有限，需要客户端适当控制发送频率（否则可能会被封禁 ip），或者使用代理进行访问。
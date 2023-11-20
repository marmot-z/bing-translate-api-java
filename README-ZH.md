# bing-translate-api

一款可以调用 Bing Translator 翻译文本的 java 工具集，由[bing-translator-api 项目](https://github.com/plainheart/bing-translate-api)移植而来。

## Usage
```java
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
catch (TranslateException | TranslationConfigLoadException e) {
    e.printStackTrace();
} finally {
    translator.close();
}
```

## 特性
- 支持多线程
- 翻译配置自动刷新

## 常见问题 & 解决方案  
1，响应码为 401，代表请求频率过高，需要使用验证码验证。此时应该适当降低请求频率。  
2，响应内容为 `{"ShowCaptcha": true}`，代表请求频率过高，需要使用验证码验证。此时应该适当降低请求频率。

另外，使用代理池是一种更好的解决方式。
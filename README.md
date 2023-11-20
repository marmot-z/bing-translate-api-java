A Java toolkit for invoking Bing Translator to translate text, ported from the [bing-translate-api project](https://github.com/plainheart/bing-translate-api).

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

## Features
- Supports multithreading
- Automatic refresh of translation configuration

## FAQs & Solutions
- If the response code is 401, it means the request frequency is too high, and you need to use captcha verification. In this case, you should reduce the request frequency appropriately.
- If the response content is {"ShowCaptcha": true}, it means the request frequency is too high, and you need to use captcha verification. In this case, you should reduce the request frequency appropriately.

Additionally, using a proxy pool is a better solution.
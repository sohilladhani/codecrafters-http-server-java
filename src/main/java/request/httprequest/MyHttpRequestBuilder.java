package request.httprequest;

import java.util.HashMap;
import java.util.Map;

public class MyHttpRequestBuilder {
    private final MyHttpRequestType httpRequestType;
    private final String url;
    private final String protocol;
    private final Map<String, String> headers;
    private String requestBody;

    private MyHttpRequestBuilder(MyHttpRequestType httpRequestType, String url, String protocol) {
        if (httpRequestType == null || url == null || protocol == null) {
            throw new IllegalArgumentException("Method, URL, and protocol are mandatory");
        }
        this.httpRequestType = httpRequestType;
        this.url = url;
        this.protocol = protocol;
        this.headers = new HashMap<>();
    }

    public static MyHttpRequestBuilder create(MyHttpRequestType httpRequestType, String url, String protocol) {
        return new MyHttpRequestBuilder(httpRequestType, url, protocol);
    }

    public MyHttpRequestBuilder addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public MyHttpRequestBuilder addHeaders(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            this.headers.putAll(headers);
        }
        return this;
    }

    public MyHttpRequestBuilder addRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public MyHttpRequest build() {
        return new MyHttpRequest(httpRequestType, url, protocol, headers, requestBody);
    }
}

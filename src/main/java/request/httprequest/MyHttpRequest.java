package request.httprequest;

import java.util.Map;

public class MyHttpRequest {
    private final MyHttpRequestType httpRequestType;
    private final String url;
    private final String protocol;
    private final Map<String, String> headers;
    private final String requestBody;

    public MyHttpRequest(MyHttpRequestType httpRequestType, String url, String protocol,
            Map<String, String> headers, String requestBody) {
        this.httpRequestType = httpRequestType;
        this.url = url;
        this.protocol = protocol;
        this.headers = headers;
        this.requestBody = requestBody;
    }

    public String getUrl() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public MyHttpRequestType getHttpRequestType() {
        return httpRequestType;
    }

    @Override
    public String toString() {
        return "MyHttpRequest [httpRequestType=" + httpRequestType + ", url=" + url + ", protocol=" + protocol
                + ", headers=" + headers + ", body=" + requestBody + "]";
    }
}

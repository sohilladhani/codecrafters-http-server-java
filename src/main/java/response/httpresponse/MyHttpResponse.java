package response.httpresponse;

import java.util.Map;

import common.constants.HttpConstants;

public class MyHttpResponse {
    private MyHttpStatusCodes statusCode;
    private Map<String, String> headers;
    private String responseBody;
    private String statusLine;

    public MyHttpResponse(MyHttpStatusCodes statusCode, Map<String, String> headers, String responseBody) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.responseBody = responseBody;
        this.setStatusLine(statusCode);
    }

    @Override
    public String toString() {
        StringBuffer httpResponseStringBuffer = new StringBuffer();
        httpResponseStringBuffer.append(HttpConstants.HTTP_VERSION + " " + statusCode);
        httpResponseStringBuffer.append(HttpConstants.CRLF);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((key, value) -> {
                httpResponseStringBuffer.append(key + ": ");
                httpResponseStringBuffer.append(value + HttpConstants.CRLF);
            });
            httpResponseStringBuffer.append(HttpConstants.CRLF);
        }
        if (responseBody != null && !responseBody.isEmpty()) {
            httpResponseStringBuffer.append(responseBody);
            httpResponseStringBuffer.append(HttpConstants.CRLF);
        }
        httpResponseStringBuffer.append(HttpConstants.CRLF);
        return httpResponseStringBuffer.toString();
    }

    public MyHttpStatusCodes getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private void setStatusLine(MyHttpStatusCodes statusCode) {
        statusLine = HttpConstants.HTTP_VERSION + " " + getStatusCode();
    }

    public String getStatusLine() {
        return statusLine;
    }

}

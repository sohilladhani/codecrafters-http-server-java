package client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import common.constants.HttpConstants;
import request.httprequest.MyHttpRequest;
import request.httprequest.MyHttpRequestBuilder;
import request.httprequest.MyHttpRequestType;
import response.httpresponse.MyHttpResponse;
import response.httpresponse.MyHttpStatusCodes;

public class ClientRequestHandler implements Runnable {

    private final Socket clientSocket;
    private String[] cliArgs;

    public ClientRequestHandler(Socket clientSocket, String[] args) {
        this.clientSocket = clientSocket;
        this.cliArgs = args;
    }

    @Override
    public void run() {
        try {
            MyHttpRequest httpRequestFromSocket = parseHttpRequestFromSocket(clientSocket.getInputStream());
            System.out.println("Data from Socket");
            System.out.println(httpRequestFromSocket);
            MyHttpResponse dataToSocket = new MyHttpResponse(MyHttpStatusCodes.OK, null, "");
            /* Parse data from socket */
            /* Check if it's an HTTP request */
            if (httpRequestFromSocket.getHttpRequestType() == MyHttpRequestType.GET) {
                /* Create GET request object */
                String httpUrl = httpRequestFromSocket.getUrl();
                if (httpUrl.startsWith(HttpConstants.ECHO_URL)) {
                    dataToSocket = processEchoFeature(httpRequestFromSocket);
                } else if (httpUrl.startsWith(HttpConstants.USER_AGENT_URL)) {
                    dataToSocket = processUserAgentFeature(httpRequestFromSocket);
                } else if (httpUrl.startsWith(HttpConstants.FILE_URL)) {
                    String directoryArg = "--directory";
                    String directoryPath = "";
                    for (int i = 0; i < cliArgs.length; i++) {
                        if (cliArgs[i].equals(directoryArg)) {
                            directoryPath = cliArgs[i + 1];
                            break;
                        }
                    }
                    dataToSocket = processGetFilesFeature(httpRequestFromSocket, directoryPath);
                } else if (!(httpUrl.equals("/") || httpUrl.equals("/index.html"))) {
                    /* Handle existence of a web page */
                    dataToSocket = new MyHttpResponse(MyHttpStatusCodes.NOT_FOUND, null, "");
                }
            } else if (httpRequestFromSocket.getHttpRequestType() == MyHttpRequestType.POST) {
                /* Create POST request object */
                String httpUrl = httpRequestFromSocket.getUrl();
                if (httpUrl.startsWith(HttpConstants.FILE_URL)) {
                    String directoryArg = "--directory";
                    String directoryPath = "";
                    for (int i = 0; i < cliArgs.length; i++) {
                        if (cliArgs[i].equals(directoryArg)) {
                            directoryPath = cliArgs[i + 1];
                            break;
                        }
                    }
                    dataToSocket = processPostFilesFeature(httpRequestFromSocket, directoryPath);
                }
            }
            System.out.println("Data to Socket");
            System.out.println(dataToSocket);
            System.out.println("accepted new connection");
            writeToSocketStream(clientSocket.getOutputStream(), dataToSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MyHttpResponse processEchoFeature(MyHttpRequest httpGetRequest) {
        /* Handle echo feature */
        String httpUrl = httpGetRequest.getUrl();
        String echoContent = httpUrl.split(HttpConstants.ECHO_URL)[1];
        Map<String, String> requestHeaders = httpGetRequest.getHeaders();
        System.out.println("echo Content:" + echoContent);
        Map<String, String> httpResponseHeaders = new HashMap<>();
        httpResponseHeaders.put(HttpConstants.CONTENT_TYPE, "text/plain");
        MyHttpResponse echoHttpResponse = new MyHttpResponse(MyHttpStatusCodes.OK, httpResponseHeaders,
                echoContent);
        String responseBody = echoContent;
        if (requestHeaders.containsKey(HttpConstants.ACCEPT_ENCODING)) {
            List<String> encodings = new ArrayList<>();
            encodings = Arrays.asList(Arrays.stream(requestHeaders.get(HttpConstants.ACCEPT_ENCODING).split(","))
                    .map(String::trim).toArray(String[]::new));
            System.out.println("encodings: " + encodings);
            if (encodings.contains("gzip")) {
                try {
                    byte[] compressedEchoContent = compressUsingGzip(echoContent);
                    System.out.println("compressedEchoContent length: " + compressedEchoContent.length);
                    httpResponseHeaders.put(HttpConstants.CONTENT_LENGTH,
                            String.valueOf(compressedEchoContent.length));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                httpResponseHeaders.put(HttpConstants.CONTENT_ENCODING, "gzip");
            }
        } else {
            httpResponseHeaders.put(HttpConstants.CONTENT_LENGTH, String.valueOf(echoContent.length()));
            responseBody = echoContent;
        }
        echoHttpResponse = new MyHttpResponse(MyHttpStatusCodes.OK, httpResponseHeaders,
                responseBody);
        return echoHttpResponse;
    }

    private byte[] compressUsingGzip(String stringToBeCompressed) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bos);
        gzipOutputStream.write(stringToBeCompressed.getBytes());
        gzipOutputStream.close();

        /* Check if the data was decompressed properly */
        assert (stringToBeCompressed.equals(decompressGzip(bos.toByteArray())));

        /* return HexFormat.of().formatHex(bos.toByteArray()); */ 
        return bos.toByteArray();
    }

    private String decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressedData));
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = gis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        gis.close();
        bos.close();
        return new String(bos.toByteArray());
    }

    private MyHttpResponse processUserAgentFeature(MyHttpRequest httpGetRequest) {
        /* Handle user-agent feature */
        Map<String, String> httpGetHeaders = httpGetRequest.getHeaders();
        String userAgentHeaderValue = httpGetHeaders.get(HttpConstants.USER_AGENT);
        Map<String, String> httpResponseHeaders = new HashMap<>();
        httpResponseHeaders.put(HttpConstants.CONTENT_TYPE, "text/plain");
        httpResponseHeaders.put(HttpConstants.CONTENT_LENGTH,
                String.valueOf(userAgentHeaderValue.length()));
        MyHttpResponse echoHttpResponse = new MyHttpResponse(MyHttpStatusCodes.OK, httpResponseHeaders,
                userAgentHeaderValue);
        return echoHttpResponse;
    }

    private MyHttpResponse processGetFilesFeature(MyHttpRequest httpGetRequest, String directoryPath) {
        /* Handle files feature */
        String httpUrl = httpGetRequest.getUrl();
        String fileName = httpUrl.split(HttpConstants.FILE_URL)[1];
        String filePath = directoryPath + fileName;
        File fileInUrl = new File(filePath);
        String fileContent = "";
        Map<String, String> httpResponseHeaders = new HashMap<>();
        MyHttpResponse fileHttpResponse = new MyHttpResponse(MyHttpStatusCodes.NOT_FOUND, httpResponseHeaders,
                null);
        if (!fileInUrl.exists()) {
            return fileHttpResponse;
        }
        try {
            byte[] fileContentInBytes = getFileContent(fileInUrl);
            httpResponseHeaders.put(HttpConstants.CONTENT_TYPE, "application/octet-stream");
            httpResponseHeaders.put(HttpConstants.CONTENT_LENGTH, String.valueOf(fileContentInBytes.length));
            fileContent = new String(fileContentInBytes, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHttpResponse = new MyHttpResponse(MyHttpStatusCodes.OK, httpResponseHeaders,
                fileContent);

        return fileHttpResponse;
    }

    private byte[] getFileContent(File file) throws FileNotFoundException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            long fileLength = file.length();
            byte[] fileContent = new byte[(int) fileLength];
            int totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = fileInputStream.read(fileContent, totalBytesRead,
                    fileContent.length - totalBytesRead)) > 0) {
                totalBytesRead += bytesRead;
                if (totalBytesRead == fileContent.length) {
                    break;
                }
            }
            if (totalBytesRead < fileLength) {
                throw new IOException("Could not read entire file content");
            }
            return fileContent;
        }
    }

    private MyHttpResponse processPostFilesFeature(MyHttpRequest httpPostRequest, String directoryPath) {
        /* Handle files feature */
        String httpUrl = httpPostRequest.getUrl();
        String fileName = httpUrl.split(HttpConstants.FILE_URL)[1];
        String filePath = directoryPath + fileName;
        File fileInUrl = new File(filePath);
        String fileContent = httpPostRequest.getRequestBody();
        writeContentToFile(fileInUrl, fileContent);
        Map<String, String> httpResponseHeaders = new HashMap<>();
        httpResponseHeaders.put(HttpConstants.CONTENT_TYPE, "text/plain");
        return new MyHttpResponse(MyHttpStatusCodes.CREATED, httpResponseHeaders, fileContent);
    }

    private void writeContentToFile(File fileInUrl, String fileContent) {
        try (FileWriter fileWriter = new FileWriter(fileInUrl)) {
            fileWriter.write(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MyHttpRequest parseHttpRequestFromSocket(InputStream sockInputStream) {
        StringBuffer headersBuffer = new StringBuffer();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sockInputStream));
        String bufferedLine = "";
        int contentLength = 0;
        MyHttpRequest myHttpRequest = null;
        try {
            /* Get request line */
            String[] requestLine = bufferedReader.readLine().split(" ");

            /* Get headers */
            while ((bufferedLine = bufferedReader.readLine()) != null && !bufferedLine.isEmpty()) {
                headersBuffer.append(bufferedLine + HttpConstants.CRLF);
                if (bufferedLine.startsWith(HttpConstants.CONTENT_LENGTH)) {
                    contentLength = Integer.parseInt(bufferedLine.split(":")[1].trim());
                }
            }

            /* Get request body */
            String requestBody = null;
            if (contentLength > 0) {
                char[] bodyBuffer = new char[contentLength];
                bufferedReader.read(bodyBuffer, 0, contentLength);
                requestBody = new String(bodyBuffer);
            }

            String requestType = requestLine[0];
            String url = requestLine[1];
            String httpVersion = requestLine[2];
            Map<String, String> headers = new HashMap<>();
            for (String header : headersBuffer.toString().split(HttpConstants.CRLF)) {
                try {
                    String[] headerKeyValue = header.split(":", 2);
                    headers.put(headerKeyValue[0].trim(), headerKeyValue[1].trim());
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            /* Create new HTTP request */
            MyHttpRequestType httpRequestType = MyHttpRequestType.GET;
            switch (requestType) {
                case "GET":
                    httpRequestType = MyHttpRequestType.GET;
                    break;
                case "POST":
                    httpRequestType = MyHttpRequestType.POST;
                    break;
                default:
                    httpRequestType = MyHttpRequestType.GET;
                    break;
            }
            myHttpRequest = MyHttpRequestBuilder.create(httpRequestType, url, httpVersion)
                    .addHeaders(headers)
                    .addRequestBody(requestBody)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myHttpRequest;
    }

    private void writeToSocketStream(OutputStream socketOutputStream, MyHttpResponse httpResponse) {
        try {
            StringBuffer responseBuffer = new StringBuffer();
            /* Write HTTP status line */
            responseBuffer.append(httpResponse.getStatusLine() + HttpConstants.CRLF);

            /* Write headers */
            Map<String, String> responseHeaders = httpResponse.getHeaders();
            System.out.println("response headers: " + responseHeaders);
            boolean gzipEncodingHeaderExistsInResponse = false;
            if (responseHeaders != null) {
                for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                    if (responseHeaders.containsKey(HttpConstants.CONTENT_ENCODING)) {
                        if (responseHeaders.get(HttpConstants.CONTENT_ENCODING).equals("gzip")) {
                            gzipEncodingHeaderExistsInResponse = true;
                        }
                    }
                    responseBuffer.append(entry.getKey() + ": " + entry.getValue() + HttpConstants.CRLF);
                }
            }

            responseBuffer.append(HttpConstants.CRLF);

            if (gzipEncodingHeaderExistsInResponse) {
                System.out.println("Gzip present");
                socketOutputStream.write(responseBuffer.toString().getBytes());
                System.out.println("written: " + responseBuffer);
                /* Write response body */
                byte[] compressedData = compressUsingGzip(httpResponse.getResponseBody());
                socketOutputStream.write(compressedData);
                System.out.println("written: " + decompressGzip(compressedData));
            } else {
                responseBuffer.append(httpResponse.getResponseBody());
                socketOutputStream.write(responseBuffer.toString().getBytes());
                System.out.println("written: " + responseBuffer);
            }
            socketOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

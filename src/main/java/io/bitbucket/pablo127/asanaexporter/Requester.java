package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bitbucket.pablo127.asanaexporter.util.JsonMapper;
import io.bitbucket.pablo127.asanaexporter.util.SleepUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.function.Supplier;

import static io.bitbucket.pablo127.asanaexporter.Main.personalAccessToken;

public final class Requester<T> {

    private static final Logger logger = LoggerFactory.getLogger(Requester.class);
    private static final OkHttpClient okHttpClient = new OkHttpClient();

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public static Requester<BytesArrayResult> ofBytesRequester() {
        return new Requester<>(BytesArrayResult.class);
    }

    public Requester(Class<T> type) {
        this.type = type;
        objectMapper = JsonMapper.INSTANCE;
    }

    public T requestGet(Supplier<URL> urlSupplier) throws IOException {
        return request(urlSupplier, "GET", null);
    }

    public T requestPost(Supplier<URL> urlSupplier, Object body) throws IOException {
        return request(urlSupplier, "POST", body);
    }

    private T request(Supplier<URL> urlSupplier, String method, Object body) throws IOException {
        RequestBody requestBody = createRequestBody(body);
        final Request request = createRequest(urlSupplier.get(), method, requestBody);
        try (Response response = okHttpClient.newCall(request)
                .execute()) {

            final ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null)
                handleError(response);

            if (isObtainingFileType()) {
                final byte[] bytes = responseBody.bytes();
                return (T) new BytesArrayResult(bytes);
            }

            return objectMapper.readValue(responseBody.string(), type);
        } catch (RetryException | SocketTimeoutException e) {
            logger.warn("We need to retry!");
            SleepUtil.sleep(30000);
            return request(urlSupplier, method, body);
        }
    }

    private boolean isObtainingFileType() {
        return this.type.isAssignableFrom(BytesArrayResult.class);
    }

    private RequestBody createRequestBody(Object body) throws JsonProcessingException {
        if (body == null) {
            return null;
        }

        String bodyJson = objectMapper.writeValueAsString(body);
        return RequestBody.create(MediaType.parse("application/json"), bodyJson);
    }

    private Request createRequest(URL url, String method, RequestBody body) {
        return new Request.Builder()
                .url(url)
                .method(method, body)
                .headers(isObtainingFileType() ? new Headers.Builder().build() : createHeaders())
                .build();
    }

    private Headers createHeaders() {
        return new Headers.Builder()
                .add("Authorization", "Bearer " + personalAccessToken)
                .add("Accept", "application/json")
                .build();
    }

    private void handleError(Response response) throws IOException {
        if (response.code() == 429)
            throw new RetryException();

        logger.error("Unexpected error occurred: " + (response.body() != null ? response.body().string() : "response body is null"));
        throw new IOException("Unexpected error occurred");
    }

    @Data
    @RequiredArgsConstructor
    public static final class BytesArrayResult {
        private final byte[] bytes;
    }
}

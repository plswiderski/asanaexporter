package io.bitbucket.pablo127.asanaexporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.bitbucket.pablo127.asanaexporter.util.JsonMapper;
import io.bitbucket.pablo127.asanaexporter.util.SleepUtil;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;

import static io.bitbucket.pablo127.asanaexporter.Main.personalAccessToken;

public class Requester<T> {

    private static final Logger logger = LoggerFactory.getLogger(Requester.class);
    private static final OkHttpClient okHttpClient = new OkHttpClient();

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public Requester(Class<T> type) {
        this.type = type;
        objectMapper = JsonMapper.INSTANCE;
    }

    public T request(UriBuilder uriBuilder) throws IOException {
        try (Response response = okHttpClient.newCall(createRequest(uriBuilder.getUrl()))
                .execute()) {

            if (!response.isSuccessful() || response.body() == null)
                handleError(response);

            return objectMapper.readValue(response.body().string(), type);
        } catch (RetryException | SocketTimeoutException e) {
            logger.warn("We need to retry!");
            SleepUtil.sleep(30000);
            return request(uriBuilder);
        }
    }

    private Request createRequest(URL url) {
        return new Request.Builder()
                .url(url)
                .get()
                .headers(createHeaders())
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
    }
}

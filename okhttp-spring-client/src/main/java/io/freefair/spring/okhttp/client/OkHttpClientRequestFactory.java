package io.freefair.spring.okhttp.client;

import lombok.NonNull;
import okhttp3.OkHttpClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.net.URI;

/**
 * OkHttp based {@link ClientHttpRequestFactory} implementation.
 * <p>
 * Serves as replacement for the deprecated {@link org.springframework.http.client.OkHttp3ClientHttpRequestFactory}.
 *
 * @see org.springframework.http.client.ClientHttpRequest
 * @see io.freefair.spring.okhttp.client.OkHttpClientRequest
 * @see io.freefair.spring.okhttp.client.OkHttpClientResponse
 * @author Lars Grefer
 */
public record OkHttpClientRequestFactory(
        @NonNull OkHttpClient okHttpClient
) implements ClientHttpRequestFactory
{
    @Override
    public OkHttpClientRequest createRequest(URI uri, HttpMethod httpMethod) {
        return new OkHttpClientRequest(okHttpClient, uri, httpMethod);
    }
}

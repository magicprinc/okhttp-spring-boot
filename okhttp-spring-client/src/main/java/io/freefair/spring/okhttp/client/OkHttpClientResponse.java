package io.freefair.spring.okhttp.client;

import io.freefair.spring.okhttp.OkHttpUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.InputStream;

/**
 * OkHttp based {@link ClientHttpResponse} implementation.
 *
 * @author Lars Grefer
 * @see OkHttpClientRequest
 * @see org.springframework.http.client.OkHttp3ClientHttpResponse
 */
@RequiredArgsConstructor
public class OkHttpClientResponse implements ClientHttpResponse {

    @Getter private final Response okHttpResponse;

    private @Nullable HttpHeaders springHeaders;

    public okhttp3.Headers getOkHttpHeaders() {
        return okHttpResponse.headers();
    }

    /// The request that initiated this HTTP response. This is _not necessarily the same request_ issued by the application:\
    /// It may be transformed by the user's interceptors. E.g: an application interceptor may add headers like User-Agent.\
    /// It may be the request generated in response to an HTTP redirect or authentication challenge. In this case the request URL may be different than the initial request URL.
    /// Use the request of the [okhttp3.Response#networkResponse()] to get the wire-level request that was transmitted.
    /// In the case of follow-ups and redirects, also look at the request of the [okhttp3.Response#priorResponse] objects, which have its own priorResponse.
    /// @see okhttp3.Response#request
    public okhttp3.Request getOriginalOkHttpRequest() {
        return okHttpResponse.request();
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return HttpStatusCode.valueOf(okHttpResponse.code());
    }

    @Override
    public String getStatusText() {
        return okHttpResponse.message();
    }

    @Override
    public InputStream getBody() {
        ResponseBody body = okHttpResponse.body();
        return body != null ? body.byteStream()
                : InputStream.nullInputStream();
    }

    @Override
    public HttpHeaders getHeaders() {
        if (springHeaders == null){
            springHeaders = OkHttpUtils.toSpringHeaders(okHttpResponse.headers());
        }

        return springHeaders;
    }

    /// Similar to [okhttp3.Response#close()], but okHttpResponse.close() doesn't check body for null
    /// @see org.springframework.http.client.ClientHttpResponse#close
    @Override
    public void close() throws RuntimeException {
        ResponseBody body = okHttpResponse.body();
        if (body != null){
            body.close();
        }
    }
}

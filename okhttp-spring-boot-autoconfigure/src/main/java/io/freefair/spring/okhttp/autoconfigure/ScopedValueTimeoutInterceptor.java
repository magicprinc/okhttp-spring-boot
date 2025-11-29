package io.freefair.spring.okhttp.autoconfigure;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/// ```
/// ScopedValue
///   .where(CONNECT_TIMEOUT, getOrDefault(request, CONNECT_TIMEOUT, Duration.ofSeconds(10)))
///   .where(READ_TIMEOUT, getOrDefault(request, READ_TIMEOUT, Duration.ofSeconds(10)))
///   .where(WRITE_TIMEOUT, getOrDefault(request, WRITE_TIMEOUT, Duration.ofSeconds(10)))
///   .call(()->)
/// ```
/// @see okhttp3.Interceptor
/// @see okhttp3.Interceptor.Chain
@NullMarked
public class ScopedValueTimeoutInterceptor implements Interceptor {
    public static final ScopedValue<@NonNull Duration> CONNECT_TIMEOUT = ScopedValue.newInstance();

    public static final ScopedValue<@NonNull Duration> READ_TIMEOUT = ScopedValue.newInstance();

    public static final ScopedValue<@NonNull Duration> WRITE_TIMEOUT = ScopedValue.newInstance();

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (CONNECT_TIMEOUT.isBound()){
            chain = chain.withConnectTimeout((int) CONNECT_TIMEOUT.get().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (READ_TIMEOUT.isBound()){
            chain = chain.withReadTimeout((int) READ_TIMEOUT.get().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (WRITE_TIMEOUT.isBound()){
            chain = chain.withWriteTimeout((int) WRITE_TIMEOUT.get().toMillis(), TimeUnit.MILLISECONDS);
        }
        return chain.proceed(chain.request());
    }
}

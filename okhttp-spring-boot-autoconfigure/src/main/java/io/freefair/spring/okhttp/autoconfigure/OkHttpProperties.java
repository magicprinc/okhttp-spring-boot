package io.freefair.spring.okhttp.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import okhttp3.Protocol;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Lars Grefer
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
//× @ConfigurationProperties(prefix = "okhttp") × @EnableConfigurationProperties(OkHttpProperties.class) on OkHttp3AutoConfiguration
public class OkHttpProperties {

    /**
     * The default connect timeout for new connections.
     * @see okhttp3.OkHttpClient.Builder#connectTimeout(Duration)
     * @see okhttp3.Interceptor.Chain#withConnectTimeout(int, TimeUnit)
     */
    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(30);

    /**
     * The default read timeout for new connections.
     * @see okhttp3.OkHttpClient.Builder#readTimeout(Duration)
     * @see okhttp3.Interceptor.Chain#withReadTimeout(int, TimeUnit)
     */
    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(120);

    /**
     * The default write timeout for new connections.
     */
    @Builder.Default
    private Duration writeTimeout = Duration.ofSeconds(60);

    /**
     * The interval between web socket pings initiated by this client. Use this to
     * automatically send web socket ping frames until either the web socket fails or it is closed.
     * This keeps the connection alive and may detect connectivity failures early. No timeouts are
     * enforced on the acknowledging pongs.
     *
     * <p>The default value of 0 disables client-initiated pings.
     * @see okhttp3.OkHttpClient.Builder#pingInterval(Duration)
     */
    @Builder.Default
    private Duration pingInterval = Duration.ZERO;

    @NestedConfigurationProperty
    private final CacheProperties cache = new CacheProperties();

    /**
     * Whether to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
     */
    @Builder.Default
    private boolean followSslRedirects = true;

    /**
     * Whether to follow redirects.
     */
    @Builder.Default
    private boolean followRedirects = true;

    /**
     * Whether to retry or not when a connectivity problem is encountered.
     */
    @Builder.Default
    private boolean retryOnConnectionFailure = true;

    /**
     * Configure the {@link Protocol Protocols} used by this client to communicate with remote servers.
     */
    private List<Protocol> protocols;

    @Builder.Default
    private boolean addDefaultInterceptors = true;

    @NestedConfigurationProperty
    private final ConnectionPoolProperties connectionPool = new ConnectionPoolProperties();

    @NestedConfigurationProperty
    private final DispatcherProperties dispatcher = new DispatcherProperties();

    /**
     * @author Lars Grefer
     * @see okhttp3.Cache
     */
    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class CacheProperties {

        private boolean enabled;

        /**
         * The maximum number of bytes this cache should use to store.
         */
        @Builder.Default
        private DataSize maxSize = DataSize.ofMegabytes(10);

        /**
         * The path of the directory where the cache should be stored.
         */
        private File directory;
    }

    /**
     * @see okhttp3.ConnectionPool
     */
    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class ConnectionPoolProperties {

        /**
         * The maximum number of idle connections for each address.
         */
        @Builder.Default
        private int maxIdleConnections = 5;

        @Builder.Default
        private Duration keepAliveDuration = Duration.ofMinutes(5);
    }

    /**
     * @see okhttp3.Dispatcher
     */
    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class DispatcherProperties {

        /**
         * The maximum number of requests to execute concurrently. Above this requests queue in memory,
         * waiting for the running calls to complete.
         *
         * If more than [maxRequests] requests are in flight when this is invoked, those requests will remain in flight.
         * @see okhttp3.Dispatcher#getMaxRequests
         */
        @Builder.Default
        int maxRequests = 0xFF_FF;

        /**
         * The maximum number of requests for each host to execute concurrently. This limits requests by
         * the URL's host name. Note that concurrent requests to a single IP address may still exceed this
         * limit: multiple hostnames may share an IP address or be routed through the same HTTP proxy.
         *
         * If more than [maxRequestsPerHost] requests are in flight when this is invoked, those requests  will remain in flight.
         *
         * WebSocket connections to hosts **do not** count against this limit.
         * @see okhttp3.Dispatcher#getMaxRequestsPerHost
         */
        @Builder.Default
        short maxRequestsPerHost = 16;
    }
}

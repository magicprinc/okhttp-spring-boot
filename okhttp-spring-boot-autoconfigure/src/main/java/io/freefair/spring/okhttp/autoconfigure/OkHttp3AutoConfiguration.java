package io.freefair.spring.okhttp.autoconfigure;

import io.freefair.spring.okhttp.ApplicationInterceptor;
import io.freefair.spring.okhttp.NetworkInterceptor;
import io.freefair.spring.okhttp.OkHttp3Configurer;
import io.freefair.spring.okhttp.async.OkHttpVtExecutorService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.Cache;
import okhttp3.CertificatePinner;
import okhttp3.CompressionInterceptor;
import okhttp3.ConnectionPool;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.EventListener;
import okhttp3.Gzip;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.FileSystemUtils;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static io.freefair.spring.okhttp.OkHttpUtils.nonEmpty;

/**
 * https://github.com/freefair/okhttp-spring-boot
 * https://github.com/magicprinc/okhttp-spring-boot
 * @author Lars Grefer
 */
@Slf4j
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@AutoConfiguration
@ConditionalOnClass(OkHttpClient.class)
@EnableConfigurationProperties // × (OkHttpProperties.class) bean, not @ConfigurationProperties(prefix = "okhttp")
public class OkHttp3AutoConfiguration {
    static final CompressionInterceptor.DecompressionAlgorithm[] CIDA = new CompressionInterceptor.DecompressionAlgorithm[0];

    //@Autowired  @EnableConfigurationProperties(OkHttpProperties.class)
    private final OkHttpProperties defaultOkHttpProperties = new OkHttpProperties();

    @Autowired
    private ObjectProvider<OkHttp3Configurer> configurers;

    @Autowired
    @ApplicationInterceptor
    private ObjectProvider<Interceptor> applicationInterceptors;

    @Autowired
    @NetworkInterceptor
    private ObjectProvider<Interceptor> networkInterceptors;

    private File tempDirCache;

    @Bean("defaultOkHttpProperties")
    @ConfigurationProperties(prefix = "okhttp")// default prefix, e.g: okhttp.connectTimeout = 1000
    @Primary // Main configuration. You need other? → @Qualifier
    public OkHttpProperties getDefaultOkHttpProperties() {
        return defaultOkHttpProperties;
    }

    @Bean
    public OkHttpClient okHttp3Client(
            OkHttpProperties okHttpProperties,
            ObjectProvider<Cache> cache,
            ObjectProvider<CookieJar> cookieJar,
            ObjectProvider<Dns> dns,
            ObjectProvider<HostnameVerifier> hostnameVerifier,
            ObjectProvider<CertificatePinner> certificatePinner,
            ConnectionPool connectionPool,
            ObjectProvider<EventListener> eventListener,
            ObjectProvider<Dispatcher> dispatcher
    ) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        cache.ifUnique(builder::cache);

        eventListener.ifUnique(builder::eventListener);

        builder.connectTimeout(okHttpProperties.getConnectTimeout());
        builder.readTimeout(okHttpProperties.getReadTimeout());
        builder.writeTimeout(okHttpProperties.getWriteTimeout());
        builder.pingInterval(okHttpProperties.getPingInterval());

        cookieJar.ifUnique(builder::cookieJar);

        dns.ifUnique(builder::dns);

        hostnameVerifier.ifUnique(builder::hostnameVerifier);
        certificatePinner.ifUnique(builder::certificatePinner);

        builder.connectionPool(connectionPool);

        builder.followRedirects(okHttpProperties.isFollowRedirects());
        builder.followSslRedirects(okHttpProperties.isFollowSslRedirects());
        builder.retryOnConnectionFailure(okHttpProperties.isRetryOnConnectionFailure());

        if (nonEmpty(okHttpProperties.getProtocols())){
            builder.protocols(okHttpProperties.getProtocols());
        }

        applicationInterceptors.forEach(builder::addInterceptor);

        if (okHttpProperties.isAddDefaultInterceptors()){
            var da = new ArrayList<CompressionInterceptor.DecompressionAlgorithm>(3);
            try {
                da.add(okhttp3.zstd.Zstd.INSTANCE);
            } catch (Throwable ignore){}
            try {
                da.add(okhttp3.brotli.Brotli.INSTANCE);
            } catch (Throwable ignore){}
            // see also: okhttp3.brotli.BrotliInterceptor
            da.add(Gzip.INSTANCE);
            builder.addInterceptor(new CompressionInterceptor(da.toArray(CIDA)));
            builder.addInterceptor(ScopedValueTimeoutInterceptor.INSTANCE);
        }

        networkInterceptors.forEach(builder::addNetworkInterceptor);

        configurers.forEach(configurer -> configurer.configure(builder));

        dispatcher.ifUnique(builder::dispatcher);

        return builder.build();
    }

    /// @see okhttp3.Dispatcher
    /// @see io.freefair.spring.okhttp.autoconfigure.OkHttpProperties#dispatcher
    @Bean
    @ConditionalOnMissingBean
    public static Dispatcher okHttp3Dispatcher(
            OkHttpProperties okHttpProperties
    ){
        return okHttp3Dispatcher(
                okHttpProperties.getDispatcher().maxRequests,
                okHttpProperties.getDispatcher().maxRequestsPerHost
        );
    }

    public static Dispatcher okHttp3Dispatcher(
            int maxRequests,
            short maxRequestsPerHost
    ){
        val dispatcher = new Dispatcher(OkHttpVtExecutorService.INSTANCE);
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
        return dispatcher;
    }

    /// @see ConnectionPool
    @Bean
    @ConditionalOnMissingBean
    public static ConnectionPool okHttp3ConnectionPool(
            OkHttpProperties okHttpProperties
    ){
        int maxIdleConnections = okHttpProperties.getConnectionPool().getMaxIdleConnections();
        Duration keepAliveDuration = okHttpProperties.getConnectionPool().getKeepAliveDuration();
        return new ConnectionPool(maxIdleConnections, keepAliveDuration.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "okhttp.cache.enabled", havingValue = "true", matchIfMissing = true)
    public Cache okHttp3Cache(
            OkHttpProperties okHttpProperties
    ) throws IOException {
        File directory = okHttpProperties.getCache().getDirectory();
        if (directory == null) {
            tempDirCache = Files.createTempDirectory("okhttp-cache").toFile();
            directory = tempDirCache;
        }
        return new Cache(directory, okHttpProperties.getCache().getMaxSize().toBytes());
    }

    @PreDestroy
    public void deleteTempCache() {
        if (tempDirCache != null){
            log.debug("deleteTempCache: Deleting the temporary OkHttp Cache: {}", tempDirCache.getAbsolutePath());
            try {
                FileSystemUtils.deleteRecursively(tempDirCache);
            } catch (Exception e){
                log.warn("deleteTempCache: Failed to delete the temporary OkHttp Cache: {}", tempDirCache.getAbsolutePath(), e);
            }
        }
    }
}

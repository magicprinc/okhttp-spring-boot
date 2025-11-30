package io.freefair.spring.okhttp;

import io.freefair.spring.okhttp.autoconfigure.OkHttp3AutoConfiguration;
import io.freefair.spring.okhttp.autoconfigure.OkHttpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Lars Grefer
 */
@SpringBootTest(
        properties = {
                "okhttp.connectTimeout=10s",
                "okhttp.readTimeout=1d",
                "okhttp.writeTimeout=5"
        },
        classes = {OkHttp3AutoConfiguration.class}
)
public class DurationPropertiesIT {
    //@SpringBootConfiguration  @EnableConfigurationProperties(OkHttpProperties.class) × @ConfigurationProperties(prefix = "okhttp")
    //static class Config {}

    @Autowired
    private OkHttpProperties okHttpProperties;

    @Autowired
    private OkHttp3AutoConfiguration okHttp3AutoConfiguration;

    @Test
    void beSure() {
        assertThat(okHttpProperties).isNotNull();
        assertThat(okHttpProperties).isSameAs(okHttp3AutoConfiguration.getDefaultOkHttpProperties());
    }

    @Test
    public void getConnectTimeout() {
        assertThat(okHttpProperties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    public void getReadTimeout() {
        assertThat(okHttpProperties.getReadTimeout()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    public void getWriteTimeout() {
        assertThat(okHttpProperties.getWriteTimeout()).isEqualTo(Duration.ofMillis(5));
    }
}

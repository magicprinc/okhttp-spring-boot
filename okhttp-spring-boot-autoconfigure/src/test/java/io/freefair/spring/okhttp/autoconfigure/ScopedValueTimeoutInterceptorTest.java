package io.freefair.spring.okhttp.autoconfigure;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

///
/// @see ScopedValueTimeoutInterceptor
@SpringBootTest(classes = ScopedValueTimeoutInterceptorTest.class)
@EnableAutoConfiguration
@SetSystemProperty(key ="okhttp.addDefaultInterceptors", value = "true")
class ScopedValueTimeoutInterceptorTest {
    enum Foo { A, B, C }

    @Autowired
    OkHttpClient okHttpClient;

    @Test
    void basic() {
        assertSame(Foo.A.getClass(), Foo.B.getClass());
        assertNotNull(okHttpClient);
    }

}

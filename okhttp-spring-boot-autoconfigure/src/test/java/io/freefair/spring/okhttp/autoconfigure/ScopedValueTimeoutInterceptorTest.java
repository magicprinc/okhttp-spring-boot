package io.freefair.spring.okhttp.autoconfigure;

import io.freefair.spring.okhttp.async.OkHttpVtExecutorService;
import io.freefair.spring.okhttp.client.OkHttpClientRequest;
import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

///
/// @see ScopedValueTimeoutInterceptor
@SuppressWarnings({"Since15", "preview"})
@SpringBootTest(classes = ScopedValueTimeoutInterceptorTest.class)
@EnableAutoConfiguration
@SetSystemProperty(key ="okhttp.addDefaultInterceptors", value = "true")
@SetSystemProperty(key ="okhttp.connectTimeout", value = "41s")
@SetSystemProperty(key ="okhttp.readTimeout", value = "45s")
class ScopedValueTimeoutInterceptorTest {
    enum Foo { A, B, C }

    @Autowired
    OkHttpClient okHttpClient;

    @Test
    void basic() throws Exception {
        assertSame(Foo.A.getClass(), Foo.B.getClass());
        assertNotNull(okHttpClient);

        Request req = OkHttpClientRequest.buildRequest().url("https://www.coachoutlet.com/").get().build();

        var resp = okHttpClient.newCall(req).execute();
        assertEquals(403, resp.code());
        System.out.println(resp);

        var e = assertThrows(IOException.class, ()->
            ScopedValue
                .where(ScopedValueTimeoutInterceptor.CONNECT_TIMEOUT, Duration.ofMillis(1))
                .where(ScopedValueTimeoutInterceptor.READ_TIMEOUT, Duration.ofMillis(1))
                .call(()->
                    okHttpClient.newCall(req).execute()
                )
        );
        assertEquals("java.net.SocketTimeoutException: timeout", e.toString());// is InterruptedIOException is IOException
    }

    @Test  @Disabled("VirtualThreads inherit ScopedValues of the parent thread")
    void testScopedValueInheritance() throws Exception {
        val threadLocal = new ThreadLocal<Integer>();
        val inheritableThreadLocal = new InheritableThreadLocal<Integer>();
        threadLocal.set(111);
        inheritableThreadLocal.set(222);

        var result = ScopedValue
                .where(ScopedValueTimeoutInterceptor.CONNECT_TIMEOUT, Duration.ofMillis(123))
                .where(ScopedValueTimeoutInterceptor.READ_TIMEOUT, Duration.ofMillis(456))
                .call(()->
                        OkHttpVtExecutorService.INSTANCE.submit(()->{
                            var thread = Thread.currentThread();
                            assertTrue(thread.isVirtual());
                            assertTrue(thread.isDaemon());
                            assertTrue(thread.isAlive());
                            assertTrue(thread.getName().startsWith("OK-"));
                            assertEquals(123, ScopedValueTimeoutInterceptor.CONNECT_TIMEOUT.get().toMillis());
                            assertEquals(456, ScopedValueTimeoutInterceptor.READ_TIMEOUT.get().toMillis());

                            assertNull(threadLocal.get());
                            assertEquals(222, inheritableThreadLocal.get());

                            return ScopedValueTimeoutInterceptor.CONNECT_TIMEOUT.get().toMillis() + ScopedValueTimeoutInterceptor.READ_TIMEOUT.get().toMillis();
                        })
                );
        assertEquals(579, result.get());
    }
}

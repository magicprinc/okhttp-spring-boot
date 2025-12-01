package io.freefair.spring.okhttp.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jspecify.annotations.Nullable;
import org.springframework.http.StreamingHttpOutputMessage;

import java.io.IOException;

/**
 * {@link StreamingHttpOutputMessage.Body} based {@link RequestBody} implementation.
 *
 * @author Lars Grefer
 * @see OkHttpClientRequest
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
final class StreamingBodyRequestBody extends RequestBody {

    /// @see io.freefair.spring.okhttp.client.OkHttpClientRequest#streamingBody
    private final StreamingHttpOutputMessage.Body streamingBody;

    /// @see okhttp3.RequestBody#contentType
    @Getter(onMethod_=@Override) private final @Nullable MediaType contentType;

    /// @see okhttp3.RequestBody#contentLength
    @Getter(onMethod_=@Override) private final long contentLength;

    @Override
    public void writeTo(BufferedSink bufferedSink) throws IOException {
        streamingBody.writeTo(bufferedSink.outputStream());
    }

    @Override
    public boolean isOneShot() {
        return !streamingBody.repeatable();
    }
}

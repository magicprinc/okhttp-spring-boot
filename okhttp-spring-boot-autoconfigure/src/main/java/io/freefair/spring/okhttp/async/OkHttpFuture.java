package io.freefair.spring.okhttp.async;

import lombok.val;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 OkHttp Callback -to→ JDK CompletableFuture<br>

 (better use: http://FailSafe.dev ! -or- SmallRye Fault Tolerance -or- Mutiny -or- https://resilience4j.readme.io/)
 <p>
 {@link Call#enqueue(Callback)} with {@link Callback} → {@link CompletableFuture}

 Returns normal JDK {@link CompletableFuture} without FailSafe policies.

 {@snippet lang=java:
  public Mono<Response> toMono (){ return Mono.fromFuture(this); }
 }

 @see #enqueue(OkHttpClient, Request)
 @see org.springframework.http.client.OkHttp3AsyncClientHttpRequest
 @author Andrey Fink [magicprinc]
*/
@NullMarked
public class OkHttpFuture extends CompletableFuture<Response> implements Callback {
    @Override
    public Executor defaultExecutor() {
        return OkHttpVtExecutorService.INSTANCE;
    }

    @Override
    public void onResponse(Call call, Response response) {
        var th = Thread.currentThread();
        complete(response);
    }//okhttp3.Callback.onResponse

    @Override
    public void onFailure(Call call, IOException e) {
        completeExceptionally(e);
    }//okhttp3.Callback.onResponse


    /**
     [OkHttp Callback -to→ JDK CompletableFuture]<br> (better use FaultTolerance or http://FailSafe.dev !)<br>
     {@link Call#enqueue(Callback)} with {@link Callback} → {@link CompletableFuture}
     Returns normal JDK {@link CompletableFuture} without FailSafe policies.
     @see #enqueue(OkHttpClient, Request)
     @see OkHttpClient#newCall
     */
    public static OkHttpFuture enqueue(Call okHttpClientNewCall) {
        val future = new OkHttpFuture();
        okHttpClientNewCall.enqueue(future);//==>
        return future;
    }

    /** @see #enqueue(Call) */
    public static OkHttpFuture enqueue(OkHttpClient client, Request request){
        return enqueue(client.newCall(request));
    }
}

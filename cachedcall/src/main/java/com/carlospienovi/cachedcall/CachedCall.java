package com.carlospienovi.cachedcall;

import java.io.IOException;
import java.util.concurrent.Executor;

import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;

public final class CachedCall<T> implements Call<T> {

    private final Call<T> call;
    private final Executor callbackExecutor;
    private final Converter<ResponseBody, Object> responseBodyConverter;
    private final okhttp3.Call.Factory callFactory;

    public CachedCall(Call<T> call, Executor callbackExecutor, Converter<ResponseBody, Object> responseBodyConverter, okhttp3.Call.Factory callFactory) {
        this.call = call;
        this.callbackExecutor = callbackExecutor;
        this.responseBodyConverter = responseBodyConverter;
        this.callFactory = callFactory;
    }

    @Override
    public Response<T> execute() throws IOException {
        return call.execute();
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        Request request = call.clone().request();
        request = request.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build();
        okhttp3.Call newCall = callFactory.newCall(request);

        newCall.enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    return;
                }
                @SuppressWarnings("unchecked") T converted = (T) responseBodyConverter.convert(response.body());
                final Response<T> newResponse = Response.success(converted, response);
                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(CachedCall.this.call, newResponse);
                    }
                });
            }

        });


        // enqueue original
        call.enqueue(new Callback<T>() {

            @Override
            public void onResponse(final Call<T> call, final Response<T> response) {
                if (response.isSuccessful() && response.raw().cacheResponse() != null) {
                    // Is from cache. Cached request should have handled it.
                    return;
                }

                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(call, response);
                    }
                });
            }

            @Override
            public void onFailure(final Call<T> call, final Throwable t) {
                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(call, t);
                    }
                });
            }

        });

    }

    @Override
    public boolean isExecuted() {
        return call.isExecuted();
    }

    @Override
    public void cancel() {
        call.cancel();
    }

    @Override
    public boolean isCanceled() {
        return call.isCanceled();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public Call<T> clone() {
        return new CachedCall<>(call.clone(), callbackExecutor, responseBodyConverter, callFactory);
    }

    @Override
    public Request request() {
        return call.request();
    }

}

package com.carlospienovi.cachedcall;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

public final class CachedCallAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(Type returnType, final Annotation[] annotations, final Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        if (rawType != Call.class) {
            return null;
        }

        final CallAdapter callAdapter = retrofit.nextCallAdapter(this, returnType, annotations);
        final Type responseType = callAdapter.responseType();
        final Converter<ResponseBody, Object> responseBodyConverter = retrofit.responseBodyConverter(responseType, annotations);
        final okhttp3.Call.Factory callFactory = retrofit.callFactory();
        final Executor callbackExecutor = retrofit.callbackExecutor();

        return new CallAdapter<Object, Object>() {
            @Override
            public Type responseType() {
                return responseType;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Object adapt(Call call) {
                return callAdapter.adapt(new CachedCall<>(call, callbackExecutor, responseBodyConverter, callFactory));
            }

        };
    }

}
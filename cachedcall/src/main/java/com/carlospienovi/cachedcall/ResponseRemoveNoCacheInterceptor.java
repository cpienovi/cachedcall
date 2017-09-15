package com.carlospienovi.cachedcall;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Response;

public final class ResponseRemoveNoCacheInterceptor implements Interceptor {

    private static final String HEADER_PRAGMA = "Pragma";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Response response = chain.proceed(chain.request());

        CacheControl newCacheControl = removeNoCache(response.cacheControl());

        return response.newBuilder()
                .removeHeader(HEADER_PRAGMA)
                .header(HEADER_CACHE_CONTROL, newCacheControl.toString())
                .build();
    }

    private CacheControl removeNoCache(CacheControl cacheControl) {
        if (!cacheControl.noCache()) {
            return cacheControl;
        }

        String[] split = cacheControl.toString().split(",");
        String result = "";
        for (int i = 0; i < split.length; i++) {
            String value = split[i].trim();
            if (!value.equalsIgnoreCase("no-cache")) {
                result = result.concat(value);
                if (i != (split.length - 1)) {
                    result = result.concat(", ");
                }
            }
        }

        Headers headers = new Headers.Builder().add(HEADER_CACHE_CONTROL, result).build();
        return CacheControl.parse(headers);
    }

}

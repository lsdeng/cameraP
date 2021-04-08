package com.hiy.camerap.interceptor;

import com.hiy.camerap.HiyConstant;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/8
 */
public class BmobInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        request = request.newBuilder()
                .addHeader("X-Bmob-Application-Id", HiyConstant.S_BMOB_APPLICATION_ID)
                .addHeader("X-Bmob-REST-API-Key", HiyConstant.S_BMOB_REST_API_KEY)
                .build();
        return chain.proceed(request);
    }
}

package com.example.callvideo.networking;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor).build();

        if (retrofit == null ){
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}


//public class RetrofitClient {
//
//    private static final String BASE_URL = "https://f2f.ocbcnisp.com/api/";
//    private static Retrofit retrofit;
//    private OcbcNispService ocbcNispService;
//    private HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//    private OkHttpClient.Builder client = new OkHttpClient.Builder();
//    private String TRANSACTIONS;
//
//    public RetrofitClient(Context context) {
//    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//    client.addInterceptor(interceptor);
//    client.addInterceptor(new Interceptor() {
//        @Override
//        public Response intercept(Chain chain) throws IOException {
//            Request original = chain.request();
//            Request.Builder requestBuilder = original.newBuilder();
//
//            requestBuilder.method(original.method(), original.body());
//
//            return chain.proceed(requestBuilder.build());
//        }
//    });
//    }
//
//
//    public OcbcNispService getInstance(){
//        if (ocbcNispService != null){
//            return ocbcNispService;
//        }
//        if (retrofit == null){
//            initializeRetrofit();
//        }
//        ocbcNispService = retrofit.create(OcbcNispService.class);
//        return ocbcNispService;
//    }
//
//    private void initializeRetrofit() {
//        retrofit = new Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .client(client.build())
//                .addConverterFactory(ScalarsConverterFactory.create())
//                .build();
//    }
//
//
//}

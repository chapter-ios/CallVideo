package com.example.callvideo.networking;

import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(interceptor).build();
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

        if (retrofit == null ){
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
        }
        return retrofit;
    }


    public static class UnsafeOkHttpClient {
        public static OkHttpClient getUnsafeOkHttpClient() {
            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
                builder.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });

                OkHttpClient okHttpClient = builder.build();
                return okHttpClient;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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

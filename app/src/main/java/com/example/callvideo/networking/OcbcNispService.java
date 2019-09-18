package com.example.callvideo.networking;

import com.example.callvideo.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface OcbcNispService {

    @FormUrlEncoded
    @POST("api/login")
    Call<LoginResponse> postLogin(@Field("name") String name, @Field("password") String password);

    @FormUrlEncoded
    @POST("img/250K.txt")
    Call<String> testDownload();
}

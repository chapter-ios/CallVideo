package com.example.callvideo.networking;

import com.example.callvideo.util.Constants;

public class UtilsApi {

    public static  final String BASE_URL = "https://f2f.ocbcnisp.com/api/";
//public static  final String BASE_URL = "https://139.180.134.73/api/";

    public static OcbcNispService getAPIService(){
        return RetrofitClient.getClient("http://" + Constants.SOCKET_ADDRESS_HTTPS).create(OcbcNispService.class);
    }
}

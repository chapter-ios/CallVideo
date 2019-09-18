package com.example.callvideo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.callvideo.model.LoginResponse;
import com.example.callvideo.networking.OcbcNispService;
import com.example.callvideo.networking.RetrofitClient;
import com.example.callvideo.networking.UtilsApi;
import com.example.callvideo.util.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private Button bt_login;
    private EditText et_name, et_password, et_callingName;
    private RetrofitClient retrofitClient;
    private OcbcNispService ocbcNispService;
    private String nama, userNama;
    private SharedPreferences myPreferences;
    private SharedPreferences.Editor editor;
    private ProgressBar login_progress;
    private TextView version_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        myPreferences = getSharedPreferences("saveDataLogin", Context.MODE_PRIVATE);

        bt_login = findViewById(R.id.bt_login);
        et_name = findViewById(R.id.et_name);
        et_password = findViewById(R.id.et_password);
        et_callingName = findViewById(R.id.calling_name);
        login_progress = findViewById(R.id.login_progress);
        version_name = findViewById(R.id.version_code);

        versionCode();

        bt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (et_name.getText().toString().isEmpty()){
                    Toast.makeText(LoginActivity.this, "Field Cannot be Empty", Toast.LENGTH_SHORT).show();
                }else {
                    login_progress.setVisibility(View.VISIBLE);
                    requestLogin(et_name.getText().toString() ,  et_password.getText().toString(), et_callingName.getText().toString());
                }
            }
        });

        ocbcNispService = UtilsApi.getAPIService();
    }

    private void versionCode() {
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        version_name.setText(versionName);
    }

    private void requestLogin(final String name, String password, final String callingName){
        Call<LoginResponse> call = ocbcNispService.postLogin(name, password);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                login_progress.setVisibility(View.GONE);
                        LoginResponse loginResponse = response.body();
                    if (response.isSuccessful()){
                        if (loginResponse.getSuccess())
                        {
                            Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra(Constants.LOGINN_TOKEN, loginResponse.getToken());
                                intent.putExtra(Constants.MY_NAME, name);
                                intent.putExtra(Constants.CALLING_NAME, callingName);

                                saveLoginResult(loginResponse.token, name, callingName);
                                startActivity(intent);

                        }else if (!loginResponse.getSuccess()){
                            Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Cannot Login", Toast.LENGTH_SHORT).show();
                login_progress.setVisibility(View.GONE);
            }
        });
    }



    private void saveLoginResult(String token, String name, String callingName){
        editor = myPreferences.edit();
        editor.putString("spToken", token);
        editor.putString("spName", name);
        editor.putString("spCallingName", callingName);
        editor.apply();

        Log.d("TOKEN_LOGIN", token);
    }
}

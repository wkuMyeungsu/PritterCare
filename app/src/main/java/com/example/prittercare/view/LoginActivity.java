package com.example.prittercare.view;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.prittercare.databinding.ActivityLoginBinding;
import com.example.prittercare.view.main.LoginRequest;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private boolean isBackPressedOnce = false;
    private static final String TAG = "MQTT";

    // Retrofit 초기화
    private static final String BASE_URL = "http://medicine.p-e.kr"; // Spring Boot 서버 URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 로그인 화면에서는 뒤로가기 버튼 숨기기
        binding.layoutToolbar.btnBack.setVisibility(View.GONE);

        // 뒤로가기 버튼 눌렀을 때 Toast 메시지 표시
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isBackPressedOnce) {
                    // If back button pressed twice, exit the activity
                    finishAffinity(); // or call System.exit(0) to terminate the app
                } else {
                    // Show a toast message on first back button press
                    Toast.makeText(LoginActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    isBackPressedOnce = true;

                    // Reset the flag after 2 seconds
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isBackPressedOnce = false;
                        }
                    }, 2000); // Adjust time as needed
                }
            }
        });

        // Sign Up 버튼 클릭 시 SignInActivity로 이동
        binding.btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        // Find Account 버튼 클릭 시 FindAccountActivity로 이동
        binding.btnFindAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, FindAccountActivity.class);
                startActivity(intent);
            }
        });

        // 로그인 버튼 클릭 시 MainActivity로 이동
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String username = binding.tvLoginId.getText().toString();
                String password = binding.tvLoginPw.getText().toString();

                // MQTT 클라이언트 객체 생성
                String clientId = "myClienId";
                MqttClient mqttClient;
                try {
                    mqttClient = new MqttClient("tcp://localhost:1883", clientId, null);

                    // 연결 옵션 설정
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setCleanSession(true);

                    // 사용자 이름과 비밀번호 설정
                    String mqttname = ""; // 여기에 사용자의 MQTT 사용자 이름을 입력하세요.
                    String mqttpassword = ""; // 여기에 사용자의 MQTT 비밀번호를 입력하세요.
                    options.setUserName(mqttname);
                    options.setPassword(mqttpassword.toCharArray());

                    // MQTT 연결
                    mqttClient.connect(options);

                    // 콜백 설정
                    mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            Log.d(TAG, "Connection lost: " + cause.getMessage());
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            Log.d(TAG, "Message received: " + message.toString());
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            Log.d(TAG, "Delivery complete");
                        }
                    });

                    // 메시지 구독
                    mqttClient.subscribe("test/topic");

                    // 메시지 발행
                    MqttMessage message = new MqttMessage("Hello from Android!".getBytes());
                    message.setQos(1);  // QoS Level 1
                    mqttClient.publish("test/topic", message);

                } catch (MqttException e) {
                    e.printStackTrace();
                }

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "로그인 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    logIn(username, password); // logIn 메서드 호출
                }
            }
        });
    }

    private void logIn(String username, String password) {
        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) // GsonConverterFactory 추가
                .build();

        // API 인터페이스 생성
        ApiService apiService = retrofit.create(ApiService.class);

        // 요청 데이터 생성
        LoginRequest loginRequest = new LoginRequest(username, password);

        // Retrofit 비동기 요청
        Call<Boolean> call = apiService.logIn(loginRequest); // 반환값을 Boolean로 변경

        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful()) {
                    Boolean isLoginSuccessful = response.body(); // true 또는 false 값 받기
                    if (isLoginSuccessful != null && isLoginSuccessful) {
                        // 로그인 성공
                        Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        // 로그인 실패
                        Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 서버 응답이 실패한 경우
                    Toast.makeText(LoginActivity.this, "서버 오류", Toast.LENGTH_SHORT).show();
                    Log.e("LoginError", "Response Code: " + response.code());
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("LoginError", "Error Body: " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                // 네트워크 오류 등 실패한 경우
                Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                Log.e("LoginActivity", "Network Error", t); // 에러 메시지와 함께 스택 트레이스를 로그로 출력
            }
        });
    }
}

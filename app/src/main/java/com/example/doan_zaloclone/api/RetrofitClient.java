package com.example.doan_zaloclone.api;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit Client Singleton
 * Handles API communication with automatic Firebase token injection
 */
public class RetrofitClient {
    private static final String TAG = "RetrofitClient";

    // Production URL
    private static final String BASE_URL = "https://zolachat.site/api/";  // Production VPS
    // private static final String BASE_URL = "http://10.0.2.2:3000/api/";  // Localhost (for development)

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    /**
     * Get Retrofit instance
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Logging interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Auth interceptor to add Firebase token
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();

                    // Get Firebase ID token
                    String token = getFirebaseIdToken();

                    Request.Builder requestBuilder = original.newBuilder();

                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    requestBuilder.method(original.method(), original.body());

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            };

            // Build OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Build Gson with custom settings
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            // Build Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            Log.d(TAG, "Retrofit client initialized with base URL: " + BASE_URL);
        }

        return retrofit;
    }

    /**
     * Get API Service instance
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getClient().create(ApiService.class);
        }
        return apiService;
    }

    /**
     * Get Firebase ID token synchronously
     * Note: This blocks the current thread. Should only be called from background thread (OkHttp interceptor)
     */
    private static String getFirebaseIdToken() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Get token synchronously (blocks thread - OK in interceptor)
                String token = user.getIdToken(false).getResult().getToken();
                Log.d(TAG, "Firebase token retrieved successfully");
                return token;
            } else {
                Log.w(TAG, "No Firebase user logged in");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting Firebase token", e);
        }
        return null;
    }

    /**
     * Reset client (useful for logout or switching environments)
     */
    public static void reset() {
        retrofit = null;
        apiService = null;
        Log.d(TAG, "Retrofit client reset");
    }
}

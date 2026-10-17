package com.johnai.apkmaker;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private Button generateButton;
    private TextView outputText;
    private ProgressBar progressBar;
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.prompt_edittext);
        generateButton = findViewById(R.id.generate_button);
        outputText = findViewById(R.id.output_text);
        progressBar = findViewById(R.id.progress_bar);

        generateButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Please describe the app or game you want.", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPromptToBackend(prompt);
        });
    }

    private void sendPromptToBackend(String prompt) {
        String backendUrl = BuildConfig.BACKEND_URL;
        if (backendUrl == null || backendUrl.trim().isEmpty() || "https://example.com".equals(backendUrl)) {
            Toast.makeText(this, "Secure backend not configured. Set BACKEND_URL to your hosted backend.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        outputText.setText("");

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("prompt", prompt);

        RequestBody body = RequestBody.create(bodyJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(backendUrl + "/generate")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    outputText.setText("Failed to reach backend: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (!response.isSuccessful()) {
                        outputText.setText("Backend error (" + response.code() + "): " + responseBody);
                        return;
                    }

                    try {
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        if (json.has("projectZipUrl")) {
                            outputText.setText("Project generated. Download: " + json.get("projectZipUrl").getAsString());
                        } else if (json.has("code")) {
                            outputText.setText(json.get("code").getAsString());
                        } else {
                            outputText.setText(responseBody);
                        }
                    } catch (Exception ex) {
                        outputText.setText(responseBody);
                    }
                });
            }
        });
    }
}

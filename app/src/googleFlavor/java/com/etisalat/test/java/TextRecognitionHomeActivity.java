package com.etisalat.test.java;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.etisalat.test.R;


public class TextRecognitionHomeActivity extends AppCompatActivity {

    Button button;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_text_recognition_home);
        button = findViewById(R.id.detect_btn);
        textView = findViewById(R.id.detected_text);
        button.setOnClickListener(v -> {
            // Text recognition
            startActivity(new Intent(TextRecognitionHomeActivity.this,
                    TextRecognitionActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String scratchIDNumber = extras.getString("EXTRA_Scratch_ID");
            //The key argument here must match that used in the other activity
            textView.setText(scratchIDNumber);
        }
    }
}

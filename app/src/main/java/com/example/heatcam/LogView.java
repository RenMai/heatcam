package com.example.heatcam;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LogView extends AppCompatActivity {

    private static final int MAX_READ_LINES = 300;

    private TextView log;
    private List<String> logs;
    private EditText filter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logcat_layout);

        log = findViewById(R.id.log_text);
        filter = findViewById(R.id.log_filter);
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {filter(s.toString());}
        });
        logs = fetchLog();
        findViewById(R.id.logs_clear).setOnClickListener(v -> clearLog());
    }

    @Override
    protected void onStart() {
        StringBuilder sb = new StringBuilder();
        logs.forEach(e -> sb.append(e).append("\n"));
        updateLog(sb.toString());
        super.onStart();
    }

    private void updateLog(String text) {
        log.setText(text);
    }

    private void filter(final String filter) {
        StringBuilder sb = new StringBuilder();
        logs.forEach(e -> {
            boolean contains = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE) // better than using toLowerCase performance-wise
                    .matcher(e)
                    .find();
            if(contains) {
                sb.append(e).append("\n");
            }
        });
        updateLog(sb.toString());
    }

    private List<String> fetchLog() {
        String filePath = getFilesDir() + "/logcat.txt";
        List<String> list = new ArrayList<>();
        try {
            ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(filePath), StandardCharsets.UTF_8);
            String st;
            while((st = reader.readLine()) != null && list.size() < MAX_READ_LINES) {
                list.add(st);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void clearLog() {
        File f = new File(getFilesDir() + "/logcat.txt");
        f.delete();
        try {
            f.createNewFile();
            updateLog("Cleared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.example.exchangerateapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.exchangerateapp.model.ConversionRecord;
import com.example.exchangerateapp.utils.FirebaseHelper;
import com.example.exchangerateapp.utils.PreferenceHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;
    private ProgressBar progressBar;
    private Button btnRefresh, btnClear;
    private TextView tvEmpty;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> recordList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        PreferenceHelper prefs = new PreferenceHelper(this);
        firebaseHelper = new FirebaseHelper(prefs.getUserId());

        initViews();
        loadHistory();
    }

    private void initViews() {
        listViewHistory = findViewById(R.id.listViewHistory);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnClear = findViewById(R.id.btnClear);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recordList);
        listViewHistory.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> loadHistory());
        btnClear.setOnClickListener(v -> {
            // 暫不實作完整清除，可自行擴充
            Toast.makeText(this, "清除功能可自行擴充 Firebase 批次刪除", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        firebaseHelper.loadRecentRecords(new FirebaseHelper.OnRecordsLoadedListener() {
            @Override
            public void onLoaded(List<ConversionRecord> records) {
                recordList.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN);

                for (ConversionRecord r : records) {
                    String item = sdf.format(r.getTimestamp().toDate()) + "\n" +
                            String.format("%.2f %s → %.2f %s\n匯率：%.4f",
                                    r.getInputAmount(), r.getFromCurrency(),
                                    r.getOutputAmount(), r.getToCurrency(), r.getRate());
                    recordList.add(item);
                }

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (recordList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "載入失敗: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
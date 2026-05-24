package com.example.exchangerateapp.utils;

import com.example.exchangerateapp.model.ConversionRecord;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId;

    public interface OnRecordsLoadedListener {
        void onLoaded(List<ConversionRecord> records);
        void onError(String error);
    }

    public FirebaseHelper(String userId) {
        this.userId = userId;
    }

    public void saveConversionRecord(ConversionRecord record) {
        db.collection("users")
                .document(userId)
                .collection("conversions")
                .add(record);
    }

    public void loadRecentRecords(OnRecordsLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .collection("conversions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ConversionRecord> records = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> records.add(doc.toObject(ConversionRecord.class)));
                    listener.onLoaded(records);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void clearAllRecords() {
        // 批次刪除較複雜，此處簡化為不實作完整批次（可後續擴充）
    }
}
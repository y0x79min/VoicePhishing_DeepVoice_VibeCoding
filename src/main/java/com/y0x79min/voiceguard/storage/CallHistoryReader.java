package com.y0x79min.voiceguard.storage;

import com.y0x79min.voiceguard.call.CallLogRecord;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/** 통화 기록 화면에 최신 통화부터 표시할 데이터를 제공합니다. */
public final class CallHistoryReader {
    private final JsonCallLogStore store;

    public CallHistoryReader(JsonCallLogStore store) {
        this.store = store;
    }

    public List<CallLogRecord> loadNewestFirst() throws IOException {
        List<CallLogRecord> records = store.readAll();
        records.sort(Comparator.comparingLong(CallLogRecord::startedAt).reversed());
        return records;
    }
}

package com.y0x79min.voiceguard.storage;

import com.y0x79min.voiceguard.call.CallAnalysisCoordinator;
import com.y0x79min.voiceguard.call.CallLogRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** 통화별 STT·탐지 확률·녹음 경로를 JSON 파일에 저장합니다. */
public final class JsonCallLogStore implements CallAnalysisCoordinator.CallLogStore {
    private final Path logFile;

    public JsonCallLogStore(Path logFile) {
        this.logFile = logFile;
    }

    @Override
    public synchronized void loadOrCreate() throws IOException {
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(logFile)) {
            Files.writeString(logFile, "[]", StandardCharsets.UTF_8);
        }
    }

    @Override
    public synchronized void save(CallLogRecord record) throws IOException {
        loadOrCreate();
        JSONArray records = readArray();
        boolean replaced = false;

        for (int index = 0; index < records.length(); index++) {
            JSONObject saved = records.getJSONObject(index);
            if (saved.optLong("startedAt", -1L) == record.startedAt()) {
                records.put(index, record.toJson());
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            records.put(record.toJson());
        }
        writeAtomically(records);
    }

    public synchronized List<CallLogRecord> readAll() throws IOException {
        loadOrCreate();
        JSONArray records = readArray();
        List<CallLogRecord> result = new ArrayList<>(records.length());
        for (int index = 0; index < records.length(); index++) {
            result.add(CallLogRecord.fromJson(records.getJSONObject(index)));
        }
        return result;
    }

    private JSONArray readArray() throws IOException {
        String json = Files.readString(logFile, StandardCharsets.UTF_8).trim();
        return json.isEmpty() ? new JSONArray() : new JSONArray(json);
    }

    private void writeAtomically(JSONArray records) throws IOException {
        Path temporary = logFile.resolveSibling(logFile.getFileName() + ".tmp");
        Files.writeString(temporary, records.toString(2), StandardCharsets.UTF_8);

        try {
            Files.move(
                    temporary,
                    logFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, logFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.TraceResponse;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TraceService {
    private final AtomicReference<TraceResponse> latest = new AtomicReference<>();

    public void setLatest(TraceResponse trace) {
        latest.set(trace);
    }

    public TraceResponse getLatest() {
        return latest.get();
    }
}

package com.smartchat.memorymanager.controller;

import com.smartchat.memorymanager.dto.TraceResponse;
import com.smartchat.memorymanager.service.TraceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace")
public class TraceController {
    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/latest")
    public ResponseEntity<TraceResponse> latest() {
        TraceResponse trace = traceService.getLatest();
        return trace == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(trace);
    }
}

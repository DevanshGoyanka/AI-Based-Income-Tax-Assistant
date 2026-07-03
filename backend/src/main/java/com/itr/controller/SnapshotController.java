package com.itr.controller;

import com.itr.domain.computation.ComputedReturn;
import com.itr.entity.ITRFiling;
import com.itr.service.FilingSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SnapshotController - records immutable filing snapshots.
 * Document 1 §8 - triggered by PDF export, JSON export, draft save.
 */
@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
public class SnapshotController {
    
    private final FilingSnapshotService snapshotService;
    
    @PostMapping
    public ResponseEntity<Long> recordSnapshot(@RequestBody ComputedReturn computedReturn) {
        ITRFiling snapshot = snapshotService.recordSnapshot(computedReturn);
        return ResponseEntity.ok(snapshot.getId());
    }
}

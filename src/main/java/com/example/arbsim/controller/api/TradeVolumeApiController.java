package com.example.arbsim.controller.api;

import com.example.arbsim.service.VolumeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TradeVolumeApiController {
    private final VolumeService volume;

    public TradeVolumeApiController(VolumeService volume) {
        this.volume = volume;
    }

    @GetMapping("/api/volumes")
    public Map<Long, java.util.Map<Long, Integer>> getVolumes() {
        return volume.snapshot();
    }
}

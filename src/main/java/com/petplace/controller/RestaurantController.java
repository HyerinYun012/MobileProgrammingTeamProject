package com.petplace.controller;
import com.petplace.dto.request.*;
import com.petplace.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/restaurants") @RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService service;

    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue="3.0") double radius) { return ResponseEntity.ok(service.findNearby(lat, lng, radius)); }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String keyword) { return ResponseEntity.ok(service.search(keyword)); }

    @GetMapping("/filter")
    public ResponseEntity<?> filter(@ModelAttribute RestaurantFilterRequest req) { return ResponseEntity.ok(service.filter(req)); }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) { return ResponseEntity.ok(service.getDetail(id)); }

    @PostMapping
    public ResponseEntity<?> register(@RequestParam Long ownerId, @RequestBody RestaurantRequest req) { return ResponseEntity.ok(service.register(ownerId, req)); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RestaurantRequest req) { return ResponseEntity.ok(service.update(id, req)); }
}

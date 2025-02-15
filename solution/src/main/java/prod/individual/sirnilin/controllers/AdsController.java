package prod.individual.sirnilin.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.sirnilin.services.AdsService;

import java.util.UUID;

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdsController {

    final AdsService adsService;

    @GetMapping()
    public ResponseEntity<?> getAds(@RequestParam("client_id") String clientId) {
        try {
            UUID clientUUID = UUID.fromString(clientId);

            return ResponseEntity.ok(adsService.getAds(clientUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get ads failed: " + e.getMessage());
        }
    }
}

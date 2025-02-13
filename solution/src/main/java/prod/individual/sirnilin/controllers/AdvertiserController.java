package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.models.request.MlScoreRequest;
import prod.individual.sirnilin.services.AdvertiserService;
import prod.individual.sirnilin.services.MlScoreService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/advertisers")
@RequiredArgsConstructor
public class AdvertiserController {

    final private AdvertiserService advertiserService;
    final private MlScoreService mlScoreService;

    @PostMapping("/bulk")
    public ResponseEntity<?> bulkInsert(@Valid @RequestBody List<AdvertiserModel> advertiserModels) {
        try {
            advertiserService.bulkInsert(advertiserModels);
            return ResponseEntity.status(HttpStatus.CREATED).body(advertiserModels);
        } catch (Exception e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Bulk insert/update failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{advertiserId}")
    public ResponseEntity<?> getAdvertiserById(@PathVariable String advertiserId) {
        try {
            UUID uuid = UUID.fromString(advertiserId);
            AdvertiserModel advertiser = advertiserService.getAdvertiserById(uuid);

            if (advertiser == null) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Advertiser not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            return ResponseEntity.ok(advertiser);
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid UUID format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/ml-scores")
    public ResponseEntity<?> saveMlScore(@Valid @RequestBody MlScoreRequest mlScoreRequest) {
        try {
            if (mlScoreService.saveScore(mlScoreRequest.getAdvertiserId(), mlScoreRequest.getClientId(), mlScoreRequest.getScore()) == null) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Save ML score failed: advertiser or client not found");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(mlScoreRequest);
        } catch (Exception e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Save ML score failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}

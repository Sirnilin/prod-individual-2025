package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.sirnilin.models.request.MlScoreRequest;
import prod.individual.sirnilin.services.MlScoreService;

import java.util.HashMap;

@RestController
@RequiredArgsConstructor
public class MlScoreController {

    final private MlScoreService mlScoreService;

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

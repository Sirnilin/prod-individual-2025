package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prod.individual.sirnilin.models.AdvertisersModel;
import prod.individual.sirnilin.services.AdvertisersService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/advertisers")
@RequiredArgsConstructor
public class AdvertisersController {

    final private AdvertisersService advertisersService;

    @PostMapping("/bulk")
    public ResponseEntity<?> bulkInsert(@Valid @RequestBody List<AdvertisersModel> advertisersModels) {
        if (advertisersService.bulkInsert(advertisersModels)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(advertisersModels);
        } else {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Bulk insert failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{advertiserId}")
    public ResponseEntity<?> getAdvertiserById(@PathVariable String advertiserId) {
        try {
            UUID uuid = UUID.fromString(advertiserId);
            AdvertisersModel advertiser = advertisersService.getAdvertiserById(uuid);

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
}

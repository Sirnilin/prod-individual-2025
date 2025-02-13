package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.request.CampaignCreateRequest;
import prod.individual.sirnilin.models.request.CampaignUpdateRequest;
import prod.individual.sirnilin.models.request.MlScoreRequest;
import prod.individual.sirnilin.services.AdvertiserService;
import prod.individual.sirnilin.services.CampaignService;
import prod.individual.sirnilin.services.MlScoreService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/advertisers")
@RequiredArgsConstructor
public class AdvertiserController {

    final private AdvertiserService advertiserService;
    final private CampaignService campaignService;

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

    @PostMapping("/{advertiserId}/campaigns")
    public ResponseEntity<?> createCampaign(@Valid @RequestBody CampaignCreateRequest request, @PathVariable String advertiserId) {
        try {
            UUID uuid = UUID.fromString(advertiserId);
            if (advertiserService.getAdvertiserById(uuid) == null) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Advertiser not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createCampaign(request, uuid));
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Create campaign failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{advertiserId}/campaigns/{campaignId}")
    public ResponseEntity<?> getCampaign(@PathVariable String advertiserId, @PathVariable String campaignId) {
        try {
            UUID advertiserUuid = UUID.fromString(advertiserId);
            UUID campaignUuid = UUID.fromString(campaignId);

            CampaignModel campaign = campaignService.getCampaign(campaignUuid);

            if (!campaign.getAdvertiserId().equals(advertiserUuid)) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Advertiser does not have access to this campaign");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            return ResponseEntity.ok(campaign);
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Get campaign failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{advertiserId}/campaigns/{campaignId}")
    public ResponseEntity<?> updateCampaign(@Valid @RequestBody CampaignUpdateRequest request, @PathVariable String advertiserId, @PathVariable String campaignId) {
        try {
            UUID advertiserUuid = UUID.fromString(advertiserId);
            UUID campaignUuid = UUID.fromString(campaignId);

            return ResponseEntity.ok(campaignService.updateCampaign(request, campaignUuid, advertiserUuid));
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Update campaign failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}

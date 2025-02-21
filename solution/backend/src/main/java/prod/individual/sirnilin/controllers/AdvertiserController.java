package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.request.CampaignCreateRequest;
import prod.individual.sirnilin.models.request.CampaignUpdateRequest;
import prod.individual.sirnilin.models.request.MlScoreRequest;
import prod.individual.sirnilin.services.AdvertiserService;
import prod.individual.sirnilin.services.CampaignService;
import prod.individual.sirnilin.services.MlScoreService;
import prod.individual.sirnilin.services.ProfanityFilterService;

import java.util.*;

@RestController
@RequestMapping("/advertisers")
@RequiredArgsConstructor
public class AdvertiserController {

    private final AdvertiserService advertiserService;
    private final CampaignService campaignService;
    private final ProfanityFilterService profanityFilterService;

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

    @PostMapping("/{advertiserId}/campaigns/V2")
    public ResponseEntity<?> createCampaignV2(@Valid @RequestBody CampaignCreateRequest request, @PathVariable String advertiserId) {
        try {
            UUID uuid = UUID.fromString(advertiserId);

            if (profanityFilterService.containsProfanity(request.getAdText()) ||
                    profanityFilterService.containsProfanity(request.getAdTitle())) {
                throw new IllegalArgumentException("Campaign contains profanity");
            }

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
            if (e.getMessage().equals("Campaign not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", e.getMessage()));
            }
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

    @PutMapping("/{advertiserId}/campaigns/{campaignId}/V2")
    public ResponseEntity<?> updateCampaignV2(@Valid @RequestBody CampaignUpdateRequest request, @PathVariable String advertiserId, @PathVariable String campaignId) {
        try {
            UUID advertiserUuid = UUID.fromString(advertiserId);
            UUID campaignUuid = UUID.fromString(campaignId);

            if (profanityFilterService.containsProfanity(request.getAdText()) ||
                    profanityFilterService.containsProfanity(request.getAdTitle())) {
                throw new IllegalArgumentException("Campaign contains profanity");
            }

            return ResponseEntity.ok(campaignService.updateCampaign(request, campaignUuid, advertiserUuid));
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Update campaign failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{advertiserId}/campaigns/{campaignId}")
    public ResponseEntity<?> deleteCampaign(@PathVariable String advertiserId, @PathVariable String campaignId) {
        try {
            UUID advertiserUuid = UUID.fromString(advertiserId);
            UUID campaignUuid = UUID.fromString(campaignId);

            campaignService.deleteCampaign(campaignUuid, advertiserUuid);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Delete campaign failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{advertiserId}/campaigns")
    public ResponseEntity<?> getAllCampaignsByAdvertiser(@PathVariable String advertiserId,
                                                         @RequestParam(value = "size", defaultValue = "10") String size,
                                                         @RequestParam(value = "page", defaultValue = "0") String page
    ) {
        try {
            UUID advertiserUuid = UUID.fromString(advertiserId);
            List<CampaignModel> allCampaigns = campaignService.getCampaignsByAdvertiserId(advertiserUuid);

            int s = Integer.parseInt(size);
            int p = Integer.parseInt(page);

            int offset = p * s;
            if (offset > allCampaigns.size()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            int end = Math.min(offset + s, allCampaigns.size());
            List<CampaignModel> pagedCampaigns = allCampaigns.subList(offset, end);
            return ResponseEntity.ok(pagedCampaigns);
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Get all campaigns failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{advertiserId}/campaigns/{campaignId}/uploadImage")
    public ResponseEntity<?> uploadImage(@PathVariable String advertiserId, @PathVariable String campaignId, @RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            UUID advertiserUuid = UUID.fromString(advertiserId);
            UUID campaignUuid = UUID.fromString(campaignId);

            return ResponseEntity.ok(campaignService.addImage(campaignUuid, file, advertiserUuid));
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Upload image failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{advertiserId}/campaigns/{campaignId}/removeImage")
    public ResponseEntity<?> removeImage(@PathVariable String advertiserId, @PathVariable String campaignId) {
        try {
            UUID advertiserUuid = UUID.fromString(advertiserId);
            UUID campaignUuid = UUID.fromString(campaignId);

            return ResponseEntity.ok(campaignService.removeImage(campaignUuid, advertiserUuid));
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Remove image failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > 15 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds limit of 15MB");
        }

        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IllegalArgumentException("Invalid file format");
        }
    }
}

package prod.individual.sirnilin.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.sirnilin.services.AdsService;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticController {

    final private AdsService adsService;

    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<?> getCampaignsStatistic(@PathVariable String campaignId) {
        try {
            UUID campaignUUID = UUID.fromString(campaignId);
            return ResponseEntity.ok(adsService.getCampaignStatistic(campaignUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get campaign statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/advertisers/{advertiserId}/campaigns")
    public ResponseEntity<?> getAdvertisersStatistic(@PathVariable String advertiserId) {
        try {
            UUID advertiserUUID = UUID.fromString(advertiserId);
            return ResponseEntity.ok(adsService.getAdvertiserStatistic(advertiserUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get advertiser statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/clients/{clientId}/daily")
    public ResponseEntity<?> getClientsDailyStatistic(@PathVariable String clientId) {
        try {
            UUID clientUUID = UUID.fromString(clientId);
            return ResponseEntity.ok(adsService.getCampaignStatisticDaily(clientUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get client daily statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/advertisers/{advertiserId}/campaigns/daily")
    public ResponseEntity<?> getAdvertisersDailyStatistic(@PathVariable String advertiserId) {
        try {
            UUID advertiserUUID = UUID.fromString(advertiserId);
            return ResponseEntity.ok(adsService.getAdvertiserStatisticDaily(advertiserUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get advertiser daily statistic failed: " + e.getMessage());
        }
    }
}

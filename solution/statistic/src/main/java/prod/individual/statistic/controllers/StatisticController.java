package prod.individual.statistic.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.statistic.services.StatisticService;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticController {

    final private StatisticService statisticService;

    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<?> getCampaignsStatistic(@PathVariable String campaignId) {
        try {
            UUID campaignUUID = UUID.fromString(campaignId);
            return ResponseEntity.ok(statisticService.getCampaignStatistic(campaignUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get campaign statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/advertisers/{advertiserId}/campaigns")
    public ResponseEntity<?> getAdvertisersStatistic(@PathVariable String advertiserId) {
        try {
            UUID advertiserUUID = UUID.fromString(advertiserId);
            return ResponseEntity.ok(statisticService.getAdvertiserStatistic(advertiserUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get advertiser statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/campaigns/{campaignsId}/daily")
    public ResponseEntity<?> getCampaignStatisticDaily(@PathVariable String campaignsId) {
        try {
            UUID campaignUUID = UUID.fromString(campaignsId);
            return ResponseEntity.ok(statisticService.getCampaignStatisticDaily(campaignUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get campaign daily statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/advertisers/{advertiserId}/campaigns/daily")
    public ResponseEntity<?> getAdvertisersDailyStatistic(@PathVariable String advertiserId) {
        try {
            UUID advertiserUUID = UUID.fromString(advertiserId);
            return ResponseEntity.ok(statisticService.getAdvertiserStatisticDaily(advertiserUUID));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get advertiser daily statistic failed: " + e.getMessage());
        }
    }
}

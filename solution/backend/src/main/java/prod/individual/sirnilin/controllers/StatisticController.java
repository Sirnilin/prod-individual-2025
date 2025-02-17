package prod.individual.sirnilin.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import prod.individual.sirnilin.services.AdsService;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticController {

    @Value("${service.url}")
    private String URL;

    private final RestTemplate restTemplate = new RestTemplate();


    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<?> getCampaignsStatistic(@PathVariable String campaignId) {
        try {
            UUID campaignUUID = UUID.fromString(campaignId);
            String url = URL+ "/stats/campaigns/" + campaignUUID;
            ResponseEntity<?> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.ok(response.getBody());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get campaign statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/advertisers/{advertiserId}/campaigns")
    public ResponseEntity<?> getAdvertisersStatistic(@PathVariable String advertiserId) {
        try {
            UUID advertiserUUID = UUID.fromString(advertiserId);
            String url = URL + "/stats/advertisers/" + advertiserUUID + "/campaigns";
            ResponseEntity<?> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.ok(response.getBody());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get advertiser statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/campaigns/{campaignsId}/daily")
    public ResponseEntity<?> getCampaignStatisticDaily(@PathVariable String campaignsId) {
        try {
            UUID campaignUUID = UUID.fromString(campaignsId);
            String url = URL + "/stats/campaigns/" + campaignUUID + "/daily";
            ResponseEntity<?> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.ok(response.getBody());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get campaign daily statistic failed: " + e.getMessage());
        }
    }

    @GetMapping("/advertisers/{advertiserId}/campaigns/daily")
    public ResponseEntity<?> getAdvertisersDailyStatistic(@PathVariable String advertiserId) {
        try {
            UUID advertiserUUID = UUID.fromString(advertiserId);
            String url = URL + "/stats/advertisers/" + advertiserUUID + "/campaigns/daily";
            ResponseEntity<?> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.ok(response.getBody());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get advertiser daily statistic failed: " + e.getMessage());
        }
    }
}

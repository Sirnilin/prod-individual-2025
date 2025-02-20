package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.request.AdsClickRequest;
import prod.individual.sirnilin.models.resonse.AdsResponse;
import prod.individual.sirnilin.services.AdsService;

import java.util.UUID;

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdsController {

    private final AdsService adsService;

    @GetMapping()
    public ResponseEntity<?> getAds(@RequestParam("client_id") String clientId) {
        try {
            UUID clientUUID = UUID.fromString(clientId);
            CampaignModel campaign = adsService.getAds(clientUUID);

            if (campaign == null) {
                return ResponseEntity.notFound().build();
            }

            AdsResponse ads = new AdsResponse(
                    campaign.getCampaignId(),
                    campaign.getAdTitle(),
                    campaign.getAdText(),
                    campaign.getAdvertiserId()
            );

            return ResponseEntity.ok(ads);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Get ads failed: " + e.getMessage());
        }
    }

    @PostMapping("{adId}/click")
    public ResponseEntity<?> adsClick(@Valid @RequestBody AdsClickRequest adsClickRequest, @PathVariable String adId) {
        try {
            UUID adUUID = UUID.fromString(adId);
            adsService.adsClick(adsClickRequest.getClientId(), adUUID);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Click failed: " + e.getMessage());
        }
    }
}

package prod.individual.sirnilin.services;

import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.ClientModel;
import prod.individual.sirnilin.repositories.CampaignRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingAdsService {

    private final CampaignRepository campaignRepository;

    @Cacheable(value = "matchingAdsCache", key = "#client.clientId")
    public List<CampaignModel> getMatchingAds(ClientModel client, Integer finalCurrentDate) {
        System.out.println("Fetching matching ads from DB...");
        return campaignRepository.findMatchingCampaigns(finalCurrentDate,
                client.getGender(),
                client.getAge(),
                client.getLocation());
    }
}

package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.TargetModel;
import prod.individual.sirnilin.models.request.CampaignCreateRequest;
import prod.individual.sirnilin.repositories.AdvertiserRepository;
import prod.individual.sirnilin.repositories.CampaignRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    final private CampaignRepository campaignRepository;
    final private AdvertiserRepository advertiserRepository;

    public CampaignModel createCampaign(CampaignCreateRequest request, UUID advertiserId) {
        CampaignModel campaign = new CampaignModel();

        if (advertiserRepository.findByAdvertiserId(advertiserId).isEmpty()) {
            throw new IllegalArgumentException("Advertiser not found");
        }

        if (request.getStartDate() > request.getEndDate()) {
            throw new IllegalArgumentException("Start date cannot be greater than end date");
        }

        TargetModel target = request.getTargeting();

        if (target != null) {
            if (target.getAgeFrom() != null &&
                    target.getAgeTo() != null &&
                    target.getAgeFrom() > target.getAgeTo()
            ) {
                throw new IllegalArgumentException("Age from cannot be greater than age to");
            }
        }

        campaign.setAdvertiserId(advertiserId);
        campaign.setImpressionsLimit(request.getImpressionsLimit());
        campaign.setClicksLimit(request.getClicksLimit());
        campaign.setCostPerImpression(request.getCostPerImpression());
        campaign.setCostPerClick(request.getCostPerClick());
        campaign.setAdTitle(request.getAdTitle());
        campaign.setAdText(request.getAdText());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setTargeting(target);

        return campaignRepository.save(campaign);
    }
}

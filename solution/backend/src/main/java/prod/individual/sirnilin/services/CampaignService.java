package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.TargetModel;
import prod.individual.sirnilin.models.TimeModel;
import prod.individual.sirnilin.models.request.CampaignCreateRequest;
import prod.individual.sirnilin.models.request.CampaignUpdateRequest;
import prod.individual.sirnilin.repositories.AdvertiserRepository;
import prod.individual.sirnilin.repositories.CampaignRepository;
import prod.individual.sirnilin.repositories.TimeRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    final private CampaignRepository campaignRepository;
    final private AdvertiserRepository advertiserRepository;
    final private RedisTemplate<String, Object> redisTemplate;
    final private TimeRepository timeRepository;

    @CacheEvict(value = "matchingAdsCache", allEntries = true)
    public CampaignModel createCampaign(CampaignCreateRequest request, UUID advertiserId) {
        CampaignModel campaign = new CampaignModel();

        if (advertiserRepository.findByAdvertiserId(advertiserId).isEmpty()) {
            throw new IllegalArgumentException("Advertiser not found");
        }

        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
        }

        if (request.getStartDate() < currentDate || request.getEndDate() < currentDate) {
            throw new IllegalArgumentException("Start date and end date cannot be less than current date");
        }

        if (request.getStartDate() > request.getEndDate()) {
            throw new IllegalArgumentException("Start date cannot be greater than end date");
        }

        if (request.getImpressionsLimit() < request.getClicksLimit()) {
            throw new IllegalArgumentException("Impressions limit cannot be less than clicks limit");
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

    public CampaignModel getCampaign(UUID campaignId) {
        return campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    }

    @CacheEvict(value = "matchingAdsCache", allEntries = true)
    public CampaignModel updateCampaign(CampaignUpdateRequest request, UUID campaignId, UUID advertiserId) {
        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new IllegalArgumentException("Advertiser does not have access to this campaign");
        }

        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }

        if (request.getStartDate() < currentDate || request.getEndDate() < currentDate) {
            throw new IllegalArgumentException("Start date and end date cannot be less than current date");
        }

        if (request.getStartDate() > request.getEndDate()) {
            throw new IllegalArgumentException("Start date cannot be greater than end date");
        }

        if ((campaign.getStartDate() != request.getStartDate() || campaign.getEndDate() != request.getEndDate()) &&
                campaign.getStartDate() <= currentDate && campaign.getEndDate() >= currentDate) {
            throw new IllegalArgumentException("Start date and end date cannot be changed after the campaign has started");
        }


        TargetModel target = request.getTargeting();

        if (target != null) {
            if (target.getAgeFrom() != null &&
                    target.getAgeTo() != null &&
                    target.getAgeFrom() > target.getAgeTo()
            ) {
                throw new IllegalArgumentException("Age from cannot be greater than age to");
            }

            campaign.setTargeting(target);
        }

        if (request.getCostPerImpression() != null) {
            campaign.setCostPerImpression(request.getCostPerImpression());
        }
        if (request.getCostPerClick() != null) {
            campaign.setCostPerClick(request.getCostPerClick());
        }
        if (request.getAdTitle() != null) {
            campaign.setAdTitle(request.getAdTitle());
        }
        if (request.getAdText() != null) {
            campaign.setAdText(request.getAdText());
        }
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());

        return campaignRepository.save(campaign);
    }

    @CacheEvict(value = "matchingAdsCache", allEntries = true)
    public void deleteCampaign(UUID campaignId, UUID advertiserId) {
        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new IllegalArgumentException("Advertiser does not have access to this campaign");
        }

        campaignRepository.delete(campaign);
    }

    public List<CampaignModel> getCampaignsByAdvertiserId(UUID advertiserId) {
        return campaignRepository.findByAdvertiserId(advertiserId);
    }
}

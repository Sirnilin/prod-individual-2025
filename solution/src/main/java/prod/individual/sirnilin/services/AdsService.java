package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.*;
import prod.individual.sirnilin.repositories.CampaignRepository;
import prod.individual.sirnilin.repositories.ClientRepository;
import prod.individual.sirnilin.repositories.MlScoreRepository;
import prod.individual.sirnilin.repositories.TimeRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdsService {

    final private CampaignRepository campaignRepository;
    final private ClientRepository clientRepository;
    final private MlScoreRepository mlScoreRepository;
    final private RedisTemplate<String, Object> redisTemplate;
    final private TimeRepository timeRepository;

    public CampaignModel getAds(UUID clientId) {
        ClientModel client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        List<CampaignModel> matchingAds = getMatchingAds(client);

        if (matchingAds.isEmpty()) {
            return null;
        }

        CampaignModel bestCampaign = getBestCampaign(matchingAds, client);

        bestCampaign.setCountImpressions(bestCampaign.getCountImpressions() + 1);

        return campaignRepository.save(bestCampaign);
    }

    private List<CampaignModel> getMatchingAds(ClientModel client) {
        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel()).getCurrentDate();
        }

        Integer finalCurrentDate = currentDate;
        return campaignRepository.findAll()
                .stream()
                .filter(campaign -> {
                    TargetModel target = campaign.getTargeting();
                    if (target == null) {
                        return true;
                    }
                    if (target.getGender() != null
                            && !target.getGender().equals(client.getGender())
                            && !target.getGender().equals(Gender.ALL)
                    ) {
                        return false;
                    }
                    if (target.getAgeFrom() != null
                            && client.getAge() < target.getAgeFrom()) {
                        return false;
                    }
                    if (target.getAgeTo() != null
                            && client.getAge() > target.getAgeTo()) {
                        return false;
                    }
                    if (target.getLocation() != null
                            && !target.getLocation().equals(client.getLocation())) {
                        return false;
                    }
                    return campaign.getStartDate() <= finalCurrentDate
                            && campaign.getEndDate() >= finalCurrentDate;
                })
                .toList();
    }

    private float computeScore(CampaignModel campaign,
                              int mlScore
    ) {
        float baseCost = 0.5f * (campaign.getCostPerImpression() + campaign.getCostPerClick());
        float mlPart = 0.25f * mlScore;

        float ratiosSum = 0f;
        int count = 0;
        if (campaign.getImpressionsLimit() != 0) {
            ratiosSum += (float) campaign.getCountImpressions() / campaign.getImpressionsLimit();
            count++;
        }
        if (campaign.getClicksLimit() != 0) {
            ratiosSum += (float) campaign.getCountClicks() / campaign.getClicksLimit();
            count++;
        }
        float limitRatio = count > 0 ? ratiosSum / count : 0f;

        return baseCost + mlPart + 0.15f * limitRatio;
    }

    private CampaignModel getBestCampaign(List<CampaignModel> matchingAds,
                                         ClientModel client
    ) {
        CampaignModel bestCampaign = null;
        float maxScore = Float.NEGATIVE_INFINITY;

        for (CampaignModel campaign : matchingAds) {
            float score = computeScore(campaign,
                    getMlScore(campaign.getAdvertiserId(), client.getClientId())
            );
            if (score > maxScore) {
                maxScore = score;
                bestCampaign = campaign;
            }
        }

        return bestCampaign;
    }

    private int getMlScore (UUID advertiserId, UUID clientId) {
        return mlScoreRepository.findByAdvertiserIdAndClientId(advertiserId, clientId)
                .orElseGet(() -> new MlScoreModel(advertiserId, clientId, 0)).getScore();
    }
}

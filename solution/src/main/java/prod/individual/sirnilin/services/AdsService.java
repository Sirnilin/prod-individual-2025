package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.*;
import prod.individual.sirnilin.repositories.*;

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
    final private HistoryImpressionsRepository historyImpressionsRepository;
    final private HistoryClicksRepository historyClicksRepository;

    public CampaignModel getAds(UUID clientId) {
        ClientModel client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel()).getCurrentDate();
        }

        List<CampaignModel> matchingAds = getMatchingAds(client, currentDate);

        if (matchingAds.isEmpty()) {
            return null;
        }

        CampaignModel bestCampaign = getBestCampaign(matchingAds, client);

        bestCampaign.setCountImpressions(bestCampaign.getCountImpressions() + 1);

        HistoryImpressionsModel historyImpressions = new HistoryImpressionsModel(clientId,
                bestCampaign.getCampaignId(),
                currentDate
        );
        historyImpressionsRepository.save(historyImpressions);

        return campaignRepository.save(bestCampaign);
    }

    public void adsClick (UUID clientId, UUID campaignId) {
        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel()).getCurrentDate();
        }

        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        HistoryImpressionsModel historyImpressions = historyImpressionsRepository.findByClientIdAndCampaignId(clientId, campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Impression not found"));

        HistoryClicksModel historyClicks = new HistoryClicksModel(clientId, campaignId, currentDate);
        historyClicksRepository.save(historyClicks);

        campaign.setCountClicks(campaign.getCountClicks() + 1);
        campaignRepository.save(campaign);
    }

    private List<CampaignModel> getMatchingAds(ClientModel client, Integer finalCurrentDate) {

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

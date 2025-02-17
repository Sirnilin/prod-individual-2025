package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import prod.individual.sirnilin.models.*;
import prod.individual.sirnilin.repositories.*;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdsService {

    private final CampaignRepository campaignRepository;
    private final ClientRepository clientRepository;
    private final MlScoreRepository mlScoreRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TimeRepository timeRepository;
    private final HistoryImpressionsRepository historyImpressionsRepository;
    private final HistoryClicksRepository historyClicksRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final KafkaProducer kafkaProducer;
    private final MatchingAdsService matchingAdsService;

    @Value("${service.url}")
    private String serviceUrl;

    private Map<UUID, Integer> fetchImpressionsFromStatisticsService(List<UUID> campaignIds) {
        String url = UriComponentsBuilder.fromHttpUrl(serviceUrl)
                .path("/history/impressions")
                .queryParam("campaignIds", campaignIds.stream().map(UUID::toString).collect(Collectors.joining(",")))
                .toUriString();
        System.out.println(url);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<UUID, Integer>>() {}).getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching impressions: " + e.getMessage());
            return Collections.emptyMap(); // Возвращаем пустую карту в случае ошибки
        }
    }

    private Map<UUID, Integer> fetchClicksFromStatisticsService(List<UUID> campaignIds) {
        String url = UriComponentsBuilder.fromHttpUrl(serviceUrl)
                .path("/history/clicks")
                .queryParam("campaignIds", campaignIds.stream().map(UUID::toString).collect(Collectors.joining(",")))
                .toUriString();
        System.out.println(url);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<UUID, Integer>>() {}).getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching clicks: " + e.getMessage());
            return Collections.emptyMap(); // Возвращаем пустую карту в случае ошибки
        }
    }

    private List<CampaignModel> updateCountImpressionsAndClicks(List<UUID> campaignIds) {
        List<UUID> missingImpressions = new ArrayList<>();
        List<UUID> missingClicks = new ArrayList<>();
        Map<UUID, Integer> impressionsMap = new HashMap<>();
        Map<UUID, Integer> clicksMap = new HashMap<>();

        for (UUID campaignId : campaignIds) {
            String redisKeyImpressions = "impressions:" + campaignId;
            String redisKeyClicks = "clicks:" + campaignId;

            Integer countImpressions = (Integer) redisTemplate.opsForValue().get(redisKeyImpressions);
            Integer countClicks = (Integer) redisTemplate.opsForValue().get(redisKeyClicks);

            if (countImpressions == null) {
                missingImpressions.add(campaignId);
            } else {
                impressionsMap.put(campaignId, countImpressions);
            }

            if (countClicks == null) {
                missingClicks.add(campaignId);
            } else {
                clicksMap.put(campaignId, countClicks);
            }
        }

        if (!missingImpressions.isEmpty()) {
            Map<UUID, Integer> fetchedImpressions = fetchImpressionsFromStatisticsService(missingImpressions);
            impressionsMap.putAll(fetchedImpressions);
            for (Map.Entry<UUID, Integer> entry : fetchedImpressions.entrySet()) {
                String redisKeyImpressions = "impressions:" + entry.getKey();
                redisTemplate.opsForValue().set(redisKeyImpressions, entry.getValue());
            }
        }

        if (!missingClicks.isEmpty()) {
            Map<UUID, Integer> fetchedClicks = fetchClicksFromStatisticsService(missingClicks);
            clicksMap.putAll(fetchedClicks);
            for (Map.Entry<UUID, Integer> entry : fetchedClicks.entrySet()) {
                String redisKeyClicks = "clicks:" + entry.getKey();
                redisTemplate.opsForValue().set(redisKeyClicks, entry.getValue());
            }
        }

        List<CampaignModel> campaigns = campaignRepository.findByCampaignIdIn(campaignIds);

        for (CampaignModel campaign : campaigns) {
            UUID campaignId = campaign.getCampaignId();
            Integer countImpressions = impressionsMap.getOrDefault(campaignId, 0);
            Integer countClicks = clicksMap.getOrDefault(campaignId, 0);

            campaign.setCountImpressions(countImpressions);
            campaign.setCountClicks(countClicks);
        }

        return campaigns;
    }

    public CampaignModel getAds(UUID clientId) {
        ClientModel client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
        }

        List<CampaignModel> matchingAds = matchingAdsService.getMatchingAds(client, currentDate);

        if (matchingAds.isEmpty()) {
            return null;
        }

        matchingAds = updateCountImpressionsAndClicks(matchingAds.stream()
                .map(CampaignModel::getCampaignId)
                .collect(Collectors.toList()));

        CampaignModel bestCampaign = getBestCampaign(matchingAds, client);

        if (bestCampaign == null) {
            return null;
        }

        kafkaProducer.sendImpressionEvent(new HistoryEvent
                (clientId,
                bestCampaign.getCampaignId(),
                bestCampaign.getAdvertiserId(),
                currentDate,
                bestCampaign.getCostPerImpression())
        );

        return bestCampaign;
    }

    public void adsClick (UUID clientId, UUID campaignId) {
        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
        }

        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        Boolean exists = historyImpressionsRepository.existsByClientIdAndCampaignId(clientId, campaignId);

        if (!exists) {
            throw new IllegalArgumentException("Impression not found");
        }

        kafkaProducer.sendClickEvent(new HistoryEvent
                (clientId,
                campaignId,
                campaign.getAdvertiserId(),
                currentDate,
                campaign.getCostPerClick())
        );
    }

    private float computeScore(CampaignModel campaign, int mlScore) {
        float baseCost = 0.5f * (campaign.getCostPerImpression() + campaign.getCostPerClick());
        float normalizedMlScore = (float) Math.log(mlScore + 1);
        float mlPart = 0.25f * normalizedMlScore;
        float impressionRatio = campaign.getImpressionsLimit() != 0
                ? (float) campaign.getCountImpressions() / campaign.getImpressionsLimit()
                : 1;
        float clickRatio = campaign.getClicksLimit() != 0
                ? (float) campaign.getCountClicks() / campaign.getClicksLimit()
                : 1;
        float impressionPenalty = impressionRatio > 1 ? 1 / impressionRatio : 1;
        float clickPenalty = clickRatio > 1 ? 1 / clickRatio : 1;
        float limitPenalty = (impressionPenalty + clickPenalty) / 2;

        float score = (baseCost + mlPart) * limitPenalty;

        if (campaign.getCountImpressions() >= campaign.getImpressionsLimit() &&
                campaign.getCountClicks() >= campaign.getClicksLimit()) {
            score *= 0;
        }

        if (campaign.getCountImpressions() >= campaign.getImpressionsLimit()) {
            score *= 0.5;
        }

        return score;
    }

    private float computeGlobalImpressionPenaltyFactor() {
        List<HistoryImpressionsModel> historyImpressionsModels = historyImpressionsRepository.findAll();
        List<UUID> campaignIds = historyImpressionsModels.stream()
                .map(HistoryImpressionsModel::getCampaignId)
                .distinct()
                .collect(Collectors.toList());

        List<CampaignModel> campaigns = campaignRepository.findByCampaignIdIn(campaignIds);
        Map<UUID, CampaignModel> campaignMap = campaigns.stream()
                .collect(Collectors.toMap(CampaignModel::getCampaignId, Function.identity()));

        float allCount = historyImpressionsModels.size();
        float errorCount = 0;

        for (HistoryImpressionsModel historyImpressionsModel : historyImpressionsModels) {
            CampaignModel campaign = campaignMap.get(historyImpressionsModel.getCampaignId());
            if (campaign == null) {
                continue;
            }
            if (campaign.getImpressionsLimit() < campaign.getCountImpressions()) {
                errorCount++;
            }
        }
        if (errorCount == 0) {
            return 1.0f;
        }
        float overshootFraction = errorCount / allCount;
        if (overshootFraction <= 0.05f) {
            float penaltyPercent = overshootFraction * 100 * 0.1f;
            float factor = 1.0f - penaltyPercent;
            return Math.max(factor, 0);
        }
        return 0;
    }

    private float computeGlobalClickPenaltyFactor() {
        List<HistoryImpressionsModel> historyImpressionsModels = historyImpressionsRepository.findAll();
        List<UUID> campaignIds = historyImpressionsModels.stream()
                .map(HistoryImpressionsModel::getCampaignId)
                .distinct()
                .collect(Collectors.toList());

        List<CampaignModel> campaigns = campaignRepository.findByCampaignIdIn(campaignIds);
        Map<UUID, CampaignModel> campaignMap = campaigns.stream()
                .collect(Collectors.toMap(CampaignModel::getCampaignId, Function.identity()));

        float allCount = historyImpressionsModels.size();
        float errorCount = 0;

        for (HistoryImpressionsModel historyImpressionsModel : historyImpressionsModels) {
            CampaignModel campaign = campaignMap.get(historyImpressionsModel.getCampaignId());
            if (campaign == null) {
                continue;
            }
            if (campaign.getClicksLimit() < campaign.getCountClicks()) {
                errorCount++;
            }
        }
        if (errorCount == 0) {
            return 1.0f;
        }
        float overshootFraction = errorCount / allCount;
        if (overshootFraction <= 0.05f) {
            float penaltyPercent = overshootFraction * 100 * 0.1f;
            float factor = 1.0f - penaltyPercent;
            return Math.max(factor, 0);
        }
        return 0;
    }

    private CampaignModel getBestCampaign(List<CampaignModel> matchingAds, ClientModel client) {
        Set<UUID> advertiserIds = matchingAds.stream()
                .map(CampaignModel::getAdvertiserId)
                .collect(Collectors.toSet());
        List<MlScoreModel> mlScores = mlScoreRepository.findByAdvertiserIdInAndClientId(advertiserIds, client.getClientId());
        Map<UUID, Integer> mlScoreMap = mlScores.stream()
                .collect(Collectors.toMap(MlScoreModel::getAdvertiserId, MlScoreModel::getScore));

        ForkJoinPool customThreadPool = new ForkJoinPool(8);
        try {
            return customThreadPool.submit(() ->
                    matchingAds.parallelStream()
                            .map(campaign -> {
                                int mlScore = mlScoreMap.getOrDefault(campaign.getAdvertiserId(), 0);
                                float score = computeScore(campaign, mlScore);
                                return new AbstractMap.SimpleEntry<>(campaign, score);
                            })
                            .max(Comparator.comparing(AbstractMap.SimpleEntry::getValue))
                            .filter(entry -> entry.getValue() > 0)
                            .map(AbstractMap.SimpleEntry::getKey)
                            .orElse(null)
            ).join();
        } finally {
            customThreadPool.shutdown();
        }
    }

    private int getMlScore (UUID advertiserId, UUID clientId) {
        return mlScoreRepository.findByAdvertiserIdAndClientId(advertiserId, clientId)
                .orElseGet(() -> new MlScoreModel(advertiserId, clientId, 0)).getScore();
    }
}

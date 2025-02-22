package prod.individual.sirnilin.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import prod.individual.sirnilin.models.*;
import prod.individual.sirnilin.models.request.StatsRequest;
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
    private final RestTemplate restTemplate = new RestTemplate();
    private final KafkaProducer kafkaProducer;
    private final MatchingAdsService matchingAdsService;
    private final MetricsExporter metricsExporter;

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
            return Collections.emptyMap();
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
            return Collections.emptyMap();
        }
    }

    private Map<UUID, Map<String, Integer>> fetchStatsFromService(List<UUID> campaignIds) {
        String url = UriComponentsBuilder.fromHttpUrl(serviceUrl)
                .path("/history/stats")
                .toUriString();

        StatsRequest request = new StatsRequest(campaignIds);

        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<Map<UUID, Map<String, Integer>>>() {}
            ).getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching stats: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<CampaignModel> updateCountImpressionsAndClicks(List<CampaignModel> campaigns) {
        List<String> keys = new ArrayList<>();
        List<UUID> missingIds = new ArrayList<>();
        for (CampaignModel campaign : campaigns) {
            keys.add("impressions:" + campaign.getCampaignId());
            keys.add("clicks:" + campaign.getCampaignId());
        }

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<UUID, Integer> impressionsMap = new HashMap<>();
        Map<UUID, Integer> clicksMap = new HashMap<>();

        for (int i = 0; i < keys.size(); i += 2) {
            UUID campaignId = UUID.fromString(keys.get(i).split(":")[1]);
            Integer imp = values.get(i) != null ? (Integer) values.get(i) : null;
            Integer clk = values.get(i + 1) != null ? (Integer) values.get(i + 1) : null;

            if (imp == null || clk == null) {
                missingIds.add(campaignId);
            } else {
                impressionsMap.put(campaignId, imp);
                clicksMap.put(campaignId, clk);
            }
        }

        if (!missingIds.isEmpty()) {
            Map<UUID, Map<String, Integer>> fetchedStats = fetchStatsFromService(missingIds);
            for (Map.Entry<UUID, Map<String, Integer>> entry : fetchedStats.entrySet()) {
                UUID id = entry.getKey();
                Integer imp = entry.getValue().getOrDefault("impressions", 0);
                Integer clk = entry.getValue().getOrDefault("clicks", 0);

                redisTemplate.opsForValue().set("impressions:" + id, imp);
                redisTemplate.opsForValue().set("clicks:" + id, clk);

                impressionsMap.put(id, imp);
                clicksMap.put(id, clk);
            }
        }

        for (CampaignModel campaign : campaigns) {
            UUID campaignId = campaign.getCampaignId();
            campaign.setCountImpressions(impressionsMap.getOrDefault(campaignId, 0));
            campaign.setCountClicks(clicksMap.getOrDefault(campaignId, 0));
        }

        return campaigns;
    }

    public CampaignModel getAds(UUID clientId) {
        ClientModel client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");
        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
            redisTemplate.opsForValue().set("currentDate", currentDate);
        }

        List<CampaignModel> candidateAds = matchingAdsService.getMatchingAds(client, currentDate);

        String redisViewedKey = "viewed_ads:" + clientId;
        Set<Object> viewedCampaigns = redisTemplate.opsForSet().members(redisViewedKey);
        boolean isViewedGroup = false;

        List<CampaignModel> newAds = candidateAds.stream()
                .filter(campaign -> viewedCampaigns == null || !viewedCampaigns.contains(campaign.getCampaignId().toString()))
                .collect(Collectors.toList());

        List<CampaignModel> adsToConsider;
        if (!newAds.isEmpty()) {
            adsToConsider = newAds;
        } else {
            String redisClickedKey = "clicked_ads:" + clientId;
            Set<Object> clickedCampaigns = redisTemplate.opsForSet().members(redisClickedKey);
            adsToConsider = candidateAds.stream()
                    .filter(campaign -> clickedCampaigns == null || !clickedCampaigns.contains(campaign.getCampaignId().toString()))
                    .collect(Collectors.toList());
            isViewedGroup = true;
        }

        if (adsToConsider.isEmpty()) {
            return null;
        }

        Double errorRate = (Double) redisTemplate.opsForValue().get("error_rate");
        double error = (errorRate != null) ? errorRate : 0.0;


        List<CampaignModel> updatedAds = updateCountImpressionsAndClicks(adsToConsider);
        CampaignModel bestCampaign = getBestCampaign(updatedAds, client, error, isViewedGroup);

        if (bestCampaign == null) {
            return null;
        }

        redisTemplate.opsForSet().add(redisViewedKey, bestCampaign.getCampaignId().toString());

        kafkaProducer.sendImpressionEvent(new HistoryEvent(
                clientId,
                bestCampaign.getCampaignId(),
                bestCampaign.getAdvertiserId(),
                currentDate,
                bestCampaign.getCostPerImpression(),
                bestCampaign.getCountImpressions() >= bestCampaign.getImpressionsLimit()
        ));
        metricsExporter.recordImpression(bestCampaign.getCampaignId(), currentDate);

        return bestCampaign;
    }

    public void adsClick (UUID clientId, UUID campaignId) {
        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
            redisTemplate.opsForValue().set("currentDate", currentDate);
        }

        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        String redisViewedKey = "viewed_ads:" + clientId;
        Boolean alreadyViewed = redisTemplate.opsForSet().isMember(redisViewedKey, campaignId.toString());
        if (alreadyViewed == null || !alreadyViewed) {
            return;
        }

        String redisClickedKey = "clicked_ads:" + clientId;
        redisTemplate.opsForSet().add(redisClickedKey, campaignId.toString());

        kafkaProducer.sendClickEvent(new HistoryEvent
                (clientId,
                        campaignId,
                        campaign.getAdvertiserId(),
                        currentDate,
                        campaign.getCostPerClick(),
                        campaign.getCountClicks() >= campaign.getClicksLimit()
                )
        );

        metricsExporter.recordClick(campaignId, currentDate);
    }

    private float computeScore(CampaignModel campaign, int mlScore, double error, boolean isViewed) {
        float baseCost = 0.5f * (campaign.getCostPerImpression() + campaign.getCostPerClick());
        baseCost = (float) Math.log1p(baseCost);

        float normalizedMlScore = (float) Math.log1p(mlScore);
        float mlPart = 0.25f * normalizedMlScore;

        float limitPenalty;
        if (isViewed) {
            limitPenalty = 1.0f;
        } else if (campaign.getCountImpressions() >= campaign.getImpressionsLimit()) {
            float overLimit = Math.max(1, campaign.getCountImpressions() - campaign.getImpressionsLimit());
            limitPenalty = (float) Math.exp(-5 * overLimit / (float) campaign.getImpressionsLimit());
        } else {
            float remainingRatio = (campaign.getImpressionsLimit() - campaign.getCountImpressions()) / (float) campaign.getImpressionsLimit();
            limitPenalty = 0.5f + 0.5f * remainingRatio;
        }

        float errorFactor = (error > 0.03) ? 0.75f : 1.0f;
        limitPenalty *= errorFactor;

        if (campaign.getCountImpressions() >= campaign.getImpressionsLimit() && error >= 0.05) {
            return 0;
        }

        return (baseCost + mlPart) * limitPenalty;
    }

    private CampaignModel getBestCampaign(List<CampaignModel> matchingAds, ClientModel client, double error, boolean isViewed) {
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
                                float score = computeScore(campaign, mlScore, error, isViewed);
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

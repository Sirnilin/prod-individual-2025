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
        }

        if (adsToConsider.isEmpty()) {
            return null;
        }

        double error = (double) redisTemplate.opsForValue().get("error_rate");


        List<CampaignModel> updatedAds = updateCountImpressionsAndClicks(adsToConsider);
        CampaignModel bestCampaign = getBestCampaign(updatedAds, client, error);

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

    private float computeScore(CampaignModel campaign, int mlScore, double error) {
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
        float impress = 1;
        float click = 1;
        if (campaign.getCountImpressions() >= campaign.getImpressionsLimit()) {
            impress = (float) (1 - error*10);
        }

        if (campaign.getCountImpressions() >= campaign.getImpressionsLimit()) {
            click = (float) (1 - error*10);
        }

        return score * impress * click;
    }

    private CampaignModel getBestCampaign(List<CampaignModel> matchingAds, ClientModel client, double error) {
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
                                float score = computeScore(campaign, mlScore, error);
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
}

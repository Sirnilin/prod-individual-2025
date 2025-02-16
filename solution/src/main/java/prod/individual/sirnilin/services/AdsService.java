package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.*;
import prod.individual.sirnilin.models.resonse.StatisticResponse;
import prod.individual.sirnilin.repositories.*;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
        }

        List<CampaignModel> matchingAds = getMatchingAds(client, currentDate);

        if (matchingAds.isEmpty()) {
            return null;
        }

        CampaignModel bestCampaign = getBestCampaign(matchingAds, client);

        if (bestCampaign == null) {
            return null;
        }

        campaignRepository.incrementImpressions(bestCampaign.getCampaignId());

        HistoryImpressionsModel historyImpressions = new HistoryImpressionsModel(clientId,
                bestCampaign.getCampaignId(),
                bestCampaign.getAdvertiserId(),
                currentDate
        );
        historyImpressionsRepository.save(historyImpressions);

        return bestCampaign;
    }

    public void adsClick (UUID clientId, UUID campaignId) {
        Integer currentDate = (Integer) redisTemplate.opsForValue().get("currentDate");

        if (currentDate == null) {
            currentDate = timeRepository.findById(1l).orElse(new TimeModel(0)).getCurrentDate();
        }

        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        HistoryImpressionsModel historyImpressions = historyImpressionsRepository.findByClientIdAndCampaignId(clientId, campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Impression not found"));

        HistoryClicksModel historyClicks = new HistoryClicksModel(clientId,
                campaignId,
                campaign.getAdvertiserId(),
                currentDate
        );
        historyClicksRepository.save(historyClicks);

        campaignRepository.incrementClicks(campaignId);
    }

    public StatisticResponse getCampaignStatistic(UUID campaignId) {
        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        List<HistoryImpressionsModel> historyImpressions = historyImpressionsRepository.findByCampaignId(campaignId);
        List<HistoryClicksModel> historyClicks = historyClicksRepository.findByCampaignId(campaignId);

        StatisticResponse statistic = new StatisticResponse();

        statistic.setImpressionsCount(historyImpressions.size());
        statistic.setClicksCount(historyClicks.size());
        statistic.setConversion((float) (historyClicks.size() / historyImpressions.size())*100);
        statistic.setSpentImpressions(campaign.getCostPerImpression()*historyImpressions.size());
        statistic.setSpentClicks(campaign.getCostPerClick()*historyClicks.size());
        statistic.setSpentTotal(statistic.getSpentImpressions() + statistic.getSpentClicks());

        return statistic;
    }

    public List<StatisticResponse> getCampaignStatisticDaily(UUID campaignId) {
        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        List<HistoryImpressionsModel> historyImpressions = historyImpressionsRepository.findByCampaignId(campaignId);
        List<HistoryClicksModel> historyClicks = historyClicksRepository.findByCampaignId(campaignId);

        Map<Integer, StatisticResponse> dailyHistoryMap = new HashMap<>();

        for (HistoryImpressionsModel impression : historyImpressions) {
            int date = impression.getDate();
            dailyHistoryMap.putIfAbsent(date, new StatisticResponse());
            StatisticResponse history = dailyHistoryMap.get(date);
            history.setImpressionsCount(history.getImpressionsCount() == null ? 1 : history.getImpressionsCount() + 1);
            history.setSpentImpressions(campaign.getCostPerImpression() * history.getImpressionsCount());
        }

        for (HistoryClicksModel click : historyClicks) {
            int date = click.getDate();
            dailyHistoryMap.putIfAbsent(date, new StatisticResponse());
            StatisticResponse history = dailyHistoryMap.get(date);
            history.setClicksCount(history.getClicksCount() == null ? 1 : history.getClicksCount() + 1);
            history.setSpentClicks(campaign.getCostPerClick() * history.getClicksCount());
        }

        for (StatisticResponse history : dailyHistoryMap.values()) {
            history.setConversion((float) (history.getClicksCount() / history.getImpressionsCount()) * 100);
            history.setSpentTotal(history.getSpentImpressions() + history.getSpentClicks());
        }

        return dailyHistoryMap.entrySet().stream()
                .map(entry -> {
                    StatisticResponse history = entry.getValue();
                    history.setDate(entry.getKey());
                    return history;
                })
                .collect(Collectors.toList());
    }

    public StatisticResponse getAdvertiserStatistic(UUID advertiserId) {
        List<CampaignModel> campaigns = campaignRepository.findByAdvertiserId(advertiserId);

        List<HistoryImpressionsModel> historyImpressions = historyImpressionsRepository.findByAdvertiserId(advertiserId);
        List<HistoryClicksModel> historyClicks = historyClicksRepository.findByAdvertiserId(advertiserId);

        StatisticResponse statistic = new StatisticResponse();

        int totalImpressions = historyImpressions.size();
        int totalClicks = historyClicks.size();
        float totalSpentImpressions = 0;
        float totalSpentClicks = 0;

        for (CampaignModel campaign : campaigns) {
            totalSpentImpressions += campaign.getCostPerImpression() * historyImpressions.stream()
                    .filter(impression -> impression.getCampaignId().equals(campaign.getCampaignId()))
                    .count();
            totalSpentClicks += campaign.getCostPerClick() * historyClicks.stream()
                    .filter(click -> click.getCampaignId().equals(campaign.getCampaignId()))
                    .count();
        }

        statistic.setImpressionsCount(totalImpressions);
        statistic.setClicksCount(totalClicks);
        statistic.setConversion((float) totalClicks / totalImpressions * 100);
        statistic.setSpentImpressions(totalSpentImpressions);
        statistic.setSpentClicks(totalSpentClicks);
        statistic.setSpentTotal(totalSpentImpressions + totalSpentClicks);

        return statistic;
    }

    public List<StatisticResponse> getAdvertiserStatisticDaily(UUID advertiserId) {
        List<CampaignModel> campaigns = campaignRepository.findByAdvertiserId(advertiserId);

        List<HistoryImpressionsModel> historyImpressions = historyImpressionsRepository.findByAdvertiserId(advertiserId);
        List<HistoryClicksModel> historyClicks = historyClicksRepository.findByAdvertiserId(advertiserId);

        Map<Integer, StatisticResponse> dailyHistoryMap = new HashMap<>();

        for (HistoryImpressionsModel impression : historyImpressions) {
            int date = impression.getDate();
            dailyHistoryMap.putIfAbsent(date, new StatisticResponse());
            StatisticResponse history = dailyHistoryMap.get(date);
            history.setImpressionsCount(history.getImpressionsCount() == null ? 1 : history.getImpressionsCount() + 1);
            history.setSpentImpressions(history.getSpentImpressions() == null ? 0 : history.getSpentImpressions());
            for (CampaignModel campaign : campaigns) {
                if (campaign.getCampaignId().equals(impression.getCampaignId())) {
                    history.setSpentImpressions(history.getSpentImpressions() + campaign.getCostPerImpression());
                }
            }
        }

        for (HistoryClicksModel click : historyClicks) {
            int date = click.getDate();
            dailyHistoryMap.putIfAbsent(date, new StatisticResponse());
            StatisticResponse history = dailyHistoryMap.get(date);
            history.setClicksCount(history.getClicksCount() == null ? 1 : history.getClicksCount() + 1);
            history.setSpentClicks(history.getSpentClicks() == null ? 0 : history.getSpentClicks());
            for (CampaignModel campaign : campaigns) {
                if (campaign.getCampaignId().equals(click.getCampaignId())) {
                    history.setSpentClicks(history.getSpentClicks() + campaign.getCostPerClick());
                }
            }
        }

        for (StatisticResponse history : dailyHistoryMap.values()) {
            history.setConversion((float) (history.getClicksCount() / history.getImpressionsCount()) * 100);
            history.setSpentTotal(history.getSpentImpressions() + history.getSpentClicks());
        }

        return dailyHistoryMap.entrySet().stream()
                .map(entry -> {
                    StatisticResponse history = entry.getValue();
                    history.setDate(entry.getKey());
                    return history;
                })
                .collect(Collectors.toList());
    }

    @Cacheable(value = "matchingAdsCache", key = "#client.clientId")
    public List<CampaignModel> getMatchingAds(ClientModel client, Integer finalCurrentDate) {
        return campaignRepository.findMatchingCampaigns(finalCurrentDate,
                client.getGender(),
                client.getAge(),
                client.getLocation());
    }

    private float computeScore(CampaignModel campaign, int mlScore,
                               float globalImpressionPenaltyFactor, float globalClickPenaltyFactor) {
        float baseCost = 0.5f * (campaign.getCostPerImpression() + campaign.getCostPerClick());
        float mlPart = 0.25f * mlScore;
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

        if (campaign.getCountClicks() > campaign.getClicksLimit() || campaign.getCountImpressions() > campaign.getImpressionsLimit()) {
            float globalLimitPenaltyFactor = globalImpressionPenaltyFactor * globalClickPenaltyFactor;
            score = score * globalLimitPenaltyFactor;
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

        float globalImpressionPenaltyFactor = computeGlobalImpressionPenaltyFactor();
        float globalClickPenaltyFactor = computeGlobalClickPenaltyFactor();

        ForkJoinPool customThreadPool = new ForkJoinPool(8);
        try {
            return customThreadPool.submit(() ->
                    matchingAds.parallelStream()
                            .map(campaign -> {
                                int mlScore = mlScoreMap.getOrDefault(campaign.getAdvertiserId(), 0);
                                float score = computeScore(campaign, mlScore, globalImpressionPenaltyFactor, globalClickPenaltyFactor);
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

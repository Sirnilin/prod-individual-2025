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

        long impressionsCount = historyImpressionsRepository.countByCampaignId(campaignId);
        long clicksCount = historyClicksRepository.countByCampaignId(campaignId);

        StatisticResponse statistic = new StatisticResponse();
        statistic.setImpressionsCount((int) impressionsCount);
        statistic.setClicksCount((int) clicksCount);
        statistic.setConversion(impressionsCount == 0 ? 0.0f
                : (float) clicksCount / impressionsCount * 100.0f);
        statistic.setSpentImpressions(campaign.getCostPerImpression() * impressionsCount);
        statistic.setSpentClicks(campaign.getCostPerClick() * clicksCount);
        statistic.setSpentTotal(statistic.getSpentImpressions() + statistic.getSpentClicks());
        return statistic;
    }

    public List<StatisticResponse> getCampaignStatisticDaily(UUID campaignId) {
        CampaignModel campaign = campaignRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        Map<Integer, StatisticResponse> dailyStats = new HashMap<>();

        List<Object[]> impressionsData = historyImpressionsRepository.countDailyImpressions(campaignId);
        for (Object[] row : impressionsData) {
            Integer date = (Integer) row[0];
            Long count = (Long) row[1];
            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            stat.setImpressionsCount(count.intValue());
            stat.setSpentImpressions(campaign.getCostPerImpression() * count);
        }

        List<Object[]> clicksData = historyClicksRepository.countDailyClicks(campaignId);
        for (Object[] row : clicksData) {
            Integer date = (Integer) row[0];
            Long count = (Long) row[1];
            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            stat.setClicksCount(count.intValue());
            stat.setSpentClicks(campaign.getCostPerClick() * count);
        }

        dailyStats.values().forEach(stat -> {
            int impressions = stat.getImpressionsCount() == null ? 0 : stat.getImpressionsCount();
            int clicks = stat.getClicksCount() == null ? 0 : stat.getClicksCount();
            float spentImpressions = stat.getSpentImpressions() == null ? 0.0f : stat.getSpentImpressions();
            float spentClicks = stat.getSpentClicks() == null ? 0.0f : stat.getSpentClicks();
            float conversion = impressions == 0 ? 0.0f : (float) clicks / impressions * 100.0f;
            stat.setConversion(conversion);
            stat.setSpentTotal(spentImpressions + spentClicks);
            stat.setClicksCount(clicks);
            stat.setSpentClicks(spentClicks);
        });

        return dailyStats.entrySet().stream()
                .map(entry -> {
                    StatisticResponse stat = entry.getValue();
                    stat.setDate(entry.getKey());
                    return stat;
                })
                .collect(Collectors.toList());
    }

    public StatisticResponse getAdvertiserStatistic(UUID advertiserId) {
        List<CampaignModel> campaigns = campaignRepository.findByAdvertiserId(advertiserId);
        Map<UUID, CampaignModel> campaignMap = campaigns.stream()
                .collect(Collectors.toMap(CampaignModel::getCampaignId, Function.identity()));

        long totalImpressions = 0;
        long totalClicks = 0;
        float totalSpentImpressions = 0;
        float totalSpentClicks = 0;

        List<Object[]> impressionsData = historyImpressionsRepository.countImpressionsByAdvertiser(advertiserId);
        for (Object[] row : impressionsData) {
            UUID campaignId = (UUID) row[0];
            Long count = (Long) row[1];
            totalImpressions += count;
            CampaignModel campaign = campaignMap.get(campaignId);
            if (campaign != null) {
                totalSpentImpressions += campaign.getCostPerImpression() * count;
            }
        }

        List<Object[]> clicksData = historyClicksRepository.countClicksByAdvertiser(advertiserId);
        for (Object[] row : clicksData) {
            UUID campaignId = (UUID) row[0];
            Long count = (Long) row[1];
            totalClicks += count;
            CampaignModel campaign = campaignMap.get(campaignId);
            if (campaign != null) {
                totalSpentClicks += campaign.getCostPerClick() * count;
            }
        }

        StatisticResponse statistic = new StatisticResponse();
        statistic.setImpressionsCount((int) totalImpressions);
        statistic.setClicksCount((int) totalClicks);
        statistic.setConversion(totalImpressions == 0 ? 0.0f
                : (float) totalClicks / totalImpressions * 100.0f);
        statistic.setSpentImpressions(totalSpentImpressions);
        statistic.setSpentClicks(totalSpentClicks);
        statistic.setSpentTotal(totalSpentImpressions + totalSpentClicks);
        return statistic;
    }

    public List<StatisticResponse> getAdvertiserStatisticDaily(UUID advertiserId) {
        List<CampaignModel> campaigns = campaignRepository.findByAdvertiserId(advertiserId);
        Map<UUID, CampaignModel> campaignMap = campaigns.stream()
                .collect(Collectors.toMap(CampaignModel::getCampaignId, Function.identity()));

        Map<Integer, StatisticResponse> dailyStats = new HashMap<>();

        List<Object[]> impressionsData = historyImpressionsRepository.countDailyImpressionsByAdvertiser(advertiserId);
        for (Object[] row : impressionsData) {
            Integer date = (Integer) row[0];
            UUID campaignId = (UUID) row[1];
            Long count = (Long) row[2];
            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            int currentImpressions = stat.getImpressionsCount() == null ? 0 : stat.getImpressionsCount();
            stat.setImpressionsCount(currentImpressions + count.intValue());
            CampaignModel campaign = campaignMap.get(campaignId);
            if (campaign != null) {
                float currentSpent = stat.getSpentImpressions() == null ? 0 : stat.getSpentImpressions();
                stat.setSpentImpressions(currentSpent + campaign.getCostPerImpression() * count);
            }
        }

        List<Object[]> clicksData = historyClicksRepository.countDailyClicksByAdvertiser(advertiserId);
        for (Object[] row : clicksData) {
            Integer date = (Integer) row[0];
            UUID campaignId = (UUID) row[1];
            Long count = (Long) row[2];
            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            int currentClicks = stat.getClicksCount() == null ? 0 : stat.getClicksCount();
            stat.setClicksCount(currentClicks + count.intValue());
            CampaignModel campaign = campaignMap.get(campaignId);
            if (campaign != null) {
                float currentSpent = stat.getSpentClicks() == null ? 0 : stat.getSpentClicks();
                stat.setSpentClicks(currentSpent + campaign.getCostPerClick() * count);
            }
        }

        dailyStats.values().forEach(stat -> {
            int impressions = stat.getImpressionsCount() == null ? 0 : stat.getImpressionsCount();
            int clicks = stat.getClicksCount() == null ? 0 : stat.getClicksCount();
            float spentImpressions = stat.getSpentImpressions() == null ? 0.0f : stat.getSpentImpressions();
            float spentClicks = stat.getSpentClicks() == null ? 0.0f : stat.getSpentClicks();
            float conversion = impressions == 0 ? 0.0f : (float) clicks / impressions * 100.0f;
            stat.setConversion(conversion);
            stat.setSpentTotal(spentImpressions + spentClicks);
            stat.setClicksCount(clicks);
            stat.setSpentClicks(spentClicks);
        });

        return dailyStats.entrySet().stream()
                .map(entry -> {
                    StatisticResponse stat = entry.getValue();
                    stat.setDate(entry.getKey());
                    return stat;
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

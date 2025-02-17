package prod.individual.statistic.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.statistic.models.HistoryClicksModel;
import prod.individual.statistic.models.HistoryImpressionsModel;
import prod.individual.statistic.models.response.StatisticResponse;
import prod.individual.statistic.repositories.HistoryClicksRepository;
import prod.individual.statistic.repositories.HistoryImpressionsRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticService {

    final private HistoryClicksRepository historyClicksRepository;
    final private HistoryImpressionsRepository historyImpressionsRepository;

    public StatisticResponse getCampaignStatistic(UUID campaignId) {
        long impressionsCount = historyImpressionsRepository.countByCampaignId(campaignId);
        long clicksCount = historyClicksRepository.countByCampaignId(campaignId);

        float spentImpressions = historyImpressionsRepository.findByCampaignId(campaignId).stream()
                .map(HistoryImpressionsModel::getCost)
                .reduce(0.0f, Float::sum);

        float spentClicks = historyClicksRepository.findByCampaignId(campaignId).stream()
                .map(HistoryClicksModel::getCost)
                .reduce(0.0f, Float::sum);

        StatisticResponse statistic = new StatisticResponse();
        statistic.setImpressionsCount((int) impressionsCount);
        statistic.setClicksCount((int) clicksCount);
        statistic.setConversion(impressionsCount == 0 ? 0.0f
                : (float) clicksCount / impressionsCount * 100.0f);
        statistic.setSpentImpressions(spentImpressions);
        statistic.setSpentClicks(spentClicks);
        statistic.setSpentTotal(spentImpressions + spentClicks);
        return statistic;
    }

    public List<StatisticResponse> getCampaignStatisticDaily(UUID campaignId) {
        Map<Integer, StatisticResponse> dailyStats = new HashMap<>();

        List<Object[]> impressionsData = historyImpressionsRepository.countDailyImpressions(campaignId);
        for (Object[] row : impressionsData) {
            Integer date = (Integer) row[0];
            Long count = (Long) row[1];
            float spentImpressions = historyImpressionsRepository.findByCampaignIdAndDate(campaignId, date).stream()
                    .map(HistoryImpressionsModel::getCost)
                    .reduce(0.0f, Float::sum);

            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            stat.setImpressionsCount(count.intValue());
            stat.setSpentImpressions(spentImpressions);
        }

        List<Object[]> clicksData = historyClicksRepository.countDailyClicks(campaignId);
        for (Object[] row : clicksData) {
            Integer date = (Integer) row[0];
            Long count = (Long) row[1];
            float spentClicks = historyClicksRepository.findByCampaignIdAndDate(campaignId, date).stream()
                    .map(HistoryClicksModel::getCost)
                    .reduce(0.0f, Float::sum);

            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            stat.setClicksCount(count.intValue());
            stat.setSpentClicks(spentClicks);
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
        long totalImpressions = 0;
        long totalClicks = 0;
        float totalSpentImpressions = 0;
        float totalSpentClicks = 0;

        List<Object[]> impressionsData = historyImpressionsRepository.countImpressionsByAdvertiser(advertiserId);
        for (Object[] row : impressionsData) {
            Long count = (Long) row[1];
            totalImpressions += count;
            totalSpentImpressions += historyImpressionsRepository.findByAdvertiserId(advertiserId).stream()
                    .map(HistoryImpressionsModel::getCost)
                    .reduce(0.0f, Float::sum);
        }

        List<Object[]> clicksData = historyClicksRepository.countClicksByAdvertiser(advertiserId);
        for (Object[] row : clicksData) {
            Long count = (Long) row[1];
            totalClicks += count;
            totalSpentClicks += historyClicksRepository.findByAdvertiserId(advertiserId).stream()
                    .map(HistoryClicksModel::getCost)
                    .reduce(0.0f, Float::sum);
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
        Map<Integer, StatisticResponse> dailyStats = new HashMap<>();

        List<Object[]> impressionsData = historyImpressionsRepository.countDailyImpressionsByAdvertiser(advertiserId);
        for (Object[] row : impressionsData) {
            Integer date = (Integer) row[0];
            Long count = (Long) row[2];
            float spentImpressions = historyImpressionsRepository.findByAdvertiserIdAndDate(advertiserId, date).stream()
                    .map(HistoryImpressionsModel::getCost)
                    .reduce(0.0f, Float::sum);

            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            int currentImpressions = stat.getImpressionsCount() == null ? 0 : stat.getImpressionsCount();
            stat.setImpressionsCount(currentImpressions + count.intValue());
            float currentSpent = stat.getSpentImpressions() == null ? 0 : stat.getSpentImpressions();
            stat.setSpentImpressions(currentSpent + spentImpressions);
        }

        List<Object[]> clicksData = historyClicksRepository.countDailyClicksByAdvertiser(advertiserId);
        for (Object[] row : clicksData) {
            Integer date = (Integer) row[0];
            Long count = (Long) row[2];
            float spentClicks = historyClicksRepository.findByAdvertiserIdAndDate(advertiserId, date).stream()
                    .map(HistoryClicksModel::getCost)
                    .reduce(0.0f, Float::sum);

            StatisticResponse stat = dailyStats.computeIfAbsent(date, d -> new StatisticResponse());
            int currentClicks = stat.getClicksCount() == null ? 0 : stat.getClicksCount();
            stat.setClicksCount(currentClicks + count.intValue());
            float currentSpent = stat.getSpentClicks() == null ? 0 : stat.getSpentClicks();
            stat.setSpentClicks(currentSpent + spentClicks);
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
}
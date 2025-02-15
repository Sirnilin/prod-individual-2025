package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.*;
import prod.individual.sirnilin.models.resonse.StatisticResponse;
import prod.individual.sirnilin.repositories.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                bestCampaign.getAdvertiserId(),
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

        HistoryClicksModel historyClicks = new HistoryClicksModel(clientId,
                campaignId,
                campaign.getAdvertiserId(),
                currentDate
        );
        historyClicksRepository.save(historyClicks);

        campaign.setCountClicks(campaign.getCountClicks() + 1);
        campaignRepository.save(campaign);
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

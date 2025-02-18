package prod.individual.statistic.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import prod.individual.statistic.models.StatsRequest;
import prod.individual.statistic.repositories.HistoryClicksRepository;
import prod.individual.statistic.repositories.HistoryImpressionsRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryImpressionsRepository historyImpressionsRepository;
    private final HistoryClicksRepository historyClicksRepository;

    @GetMapping("/impressions")
    public Map<UUID, Integer> getImpressionsCount(@RequestParam List<UUID> campaignIds) {
        Map<UUID, Integer> impressionsMap = historyImpressionsRepository.countByCampaignIds(campaignIds);
        for (UUID campaignId : campaignIds) {
            impressionsMap.putIfAbsent(campaignId, 0);
        }
        return impressionsMap;
    }

    @GetMapping("/clicks")
    public Map<UUID, Integer> getClicksCount(@RequestParam List<UUID> campaignIds) {
        Map<UUID, Integer> clicksMap = historyClicksRepository.countByCampaignIds(campaignIds);
        for (UUID campaignId : campaignIds) {
            clicksMap.putIfAbsent(campaignId, 0);
        }
        return clicksMap;
    }

    @PostMapping("/stats")
    public Map<UUID, Map<String, Integer>> getStats(@RequestBody StatsRequest request) {
        List<UUID> campaignIds = request.getCampaignIds();
        Map<UUID, Integer> impressionsMap = historyImpressionsRepository.countByCampaignIds(campaignIds);
        Map<UUID, Integer> clicksMap = historyClicksRepository.countByCampaignIds(campaignIds);

        Map<UUID, Map<String, Integer>> result = new HashMap<>();
        for (UUID id : campaignIds) {
            int impressions = impressionsMap.getOrDefault(id, 0);
            int clicks = clicksMap.getOrDefault(id, 0);

            Map<String, Integer> metrics = new HashMap<>();
            metrics.put("impressions", impressions);
            metrics.put("clicks", clicks);

            result.put(id, metrics);
        }
        return result;
    }

}

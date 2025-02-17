package prod.individual.statistic.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.statistic.repositories.HistoryClicksRepository;
import prod.individual.statistic.repositories.HistoryImpressionsRepository;

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

}

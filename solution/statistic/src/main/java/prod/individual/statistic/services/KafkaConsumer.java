package prod.individual.statistic.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import prod.individual.statistic.models.HistoryClicksModel;
import prod.individual.statistic.models.HistoryImpressionsModel;
import prod.individual.statistic.models.HistoryEvent;
import prod.individual.statistic.repositories.HistoryClicksRepository;
import prod.individual.statistic.repositories.HistoryImpressionsRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final HistoryImpressionsRepository historyImpressionsRepository;
    private final HistoryClicksRepository historyClicksRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "impression-events", groupId = "ad-statistics")
    public void consumeImpressionEvent(HistoryEvent event) {
        String redisViewedKey = "viewed_ads:" + event.getClientId().toString();
        Boolean alreadyViewed = redisTemplate.opsForSet().isMember(redisViewedKey, event.getCampaignId().toString());

        if (alreadyViewed != null && alreadyViewed) {
            return;
        }
        redisTemplate.opsForSet().add(redisViewedKey, event.getCampaignId().toString());

        if (historyImpressionsRepository.existsByClientIdAndCampaignId(event.getClientId(), event.getCampaignId())) {
            return;
        }
        historyImpressionsRepository.save(new HistoryImpressionsModel(
                event.getClientId(),
                event.getCampaignId(),
                event.getAdvertiserId(),
                event.getCurrentDate(),
                event.getCost()
        ));

        String redisKey = "impressions:" + event.getCampaignId();
        redisTemplate.opsForValue().increment(redisKey);
    }

    @KafkaListener(topics = "click-events", groupId = "ad-statistics")
    public void consumeClickEvent(HistoryEvent event) {
        log.info("Received click event: {}", event);

        Boolean exist = historyClicksRepository.existsByClientIdAndCampaignId(event.getClientId(), event.getCampaignId());

        if (exist) {
            return;
        }

        historyClicksRepository.save(new HistoryClicksModel(
                event.getClientId(),
                event.getCampaignId(),
                event.getAdvertiserId(),
                event.getCurrentDate(),
                event.getCost()
        ));

        String redisKey = "clicks:" + event.getCampaignId();
        redisTemplate.opsForValue().increment(redisKey);
    }
}

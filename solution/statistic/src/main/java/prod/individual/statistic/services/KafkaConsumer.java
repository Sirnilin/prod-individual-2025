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

        redisTemplate.opsForValue().increment("total_impressions");

        if (event.getError()) {
            redisTemplate.opsForValue().increment("error_impressions");
        }


        Long totalImpressions = (Long) redisTemplate.opsForValue().get("total_impressions");
        Long errorImpressions = (Long) redisTemplate.opsForValue().get("error_impressions");

        if (totalImpressions != null && totalImpressions > 0) {
            double errorRate = (double) errorImpressions / totalImpressions * 100;
            redisTemplate.opsForValue().set("error_rate", errorRate);
        }
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

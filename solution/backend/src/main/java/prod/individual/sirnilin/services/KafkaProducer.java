package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.HistoryEvent;

@Service
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, HistoryEvent> kafkaTemplate;

    public void sendImpressionEvent(HistoryEvent event) {
        kafkaTemplate.send("impression-events", event);
    }

    public void sendClickEvent(HistoryEvent event) {
        kafkaTemplate.send("click-events", event);
    }
}

package prod.individual.statistic.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import prod.individual.statistic.models.HistoryEvent;

import java.util.Map;

public class HistoryEventSerializer implements Serializer<HistoryEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, HistoryEvent data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Ошибка сериализации объекта HistoryEvent", e);
        }
    }

    @Override
    public void close() {
    }
}

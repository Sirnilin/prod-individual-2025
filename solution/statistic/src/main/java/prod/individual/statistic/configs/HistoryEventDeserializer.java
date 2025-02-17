package prod.individual.statistic.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import prod.individual.statistic.models.HistoryEvent;

import java.util.Map;

public class HistoryEventDeserializer implements Deserializer<HistoryEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public HistoryEvent deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, HistoryEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Ошибка десериализации объекта HistoryEvent", e);
        }
    }

    @Override
    public void close() {
    }
}
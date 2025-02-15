package prod.individual.sirnilin.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import prod.individual.sirnilin.models.TimeModel;
import prod.individual.sirnilin.models.request.TimeRequest;
import prod.individual.sirnilin.repositories.TimeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TimeControllerTest {

    @Mock
    private TimeRepository timeRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TimeController timeController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testUpdateCurrentDate() {
        TimeRequest timeRequest = new TimeRequest();
        timeRequest.setCurrentDate(10);

        TimeModel timeModel = new TimeModel();
        timeModel.setCurrentDate(10);

        when(timeRepository.findById(1L)).thenReturn(java.util.Optional.of(timeModel));
        when(timeRepository.save(any(TimeModel.class))).thenReturn(timeModel);

        ResponseEntity<?> responseEntity = timeController.updateCurrentDate(timeRequest);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertEquals(timeModel, responseEntity.getBody());

        verify(timeRepository, times(1)).findById(1L);
        verify(timeRepository, times(1)).save(any(TimeModel.class));
        verify(redisTemplate.opsForValue(), times(1)).set("currentDate", 10);
    }

    @Test
    public void testUpdateCurrentDateWithPastTime() {
        TimeRequest timeRequest = new TimeRequest();
        timeRequest.setCurrentDate(5);

        TimeModel timeModel = new TimeModel();
        timeModel.setCurrentDate(10);

        when(timeRepository.findById(1L)).thenReturn(java.util.Optional.of(timeModel));

        ResponseEntity<?> responseEntity = timeController.updateCurrentDate(timeRequest);

        assertEquals(400, responseEntity.getStatusCode().value());
        assertTrue(responseEntity.getBody().toString().contains("Current date cannot be decreased"));

        verify(timeRepository, times(1)).findById(1L);
        verify(timeRepository, times(0)).save(any(TimeModel.class));
        verify(redisTemplate.opsForValue(), times(0)).set(anyString(), any());
    }
}

package prod.individual.sirnilin.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import prod.individual.sirnilin.models.MlScoreModel;
import prod.individual.sirnilin.models.request.MlScoreRequest;
import prod.individual.sirnilin.services.MlScoreService;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MlScoreControllerTest {

    @Mock
    private MlScoreService mlScoreService;

    @InjectMocks
    private MlScoreController mlScoreController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSaveMlScoreSuccess() {
        MlScoreRequest request = new MlScoreRequest();
        request.setAdvertiserId(UUID.randomUUID());
        request.setClientId(UUID.randomUUID());
        request.setScore(85);

        MlScoreModel mlScoreModel = new MlScoreModel();
        mlScoreModel.setAdvertiserId(request.getAdvertiserId());
        mlScoreModel.setClientId(request.getClientId());
        mlScoreModel.setScore(request.getScore());

        when(mlScoreService.saveScore(any(UUID.class), any(UUID.class), any(Integer.class))).thenReturn(mlScoreModel);

        ResponseEntity<?> responseEntity = mlScoreController.saveMlScore(request);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(request, responseEntity.getBody());
    }

    @Test
    public void testSaveMlScoreFailure() {
        MlScoreRequest request = new MlScoreRequest();
        request.setAdvertiserId(UUID.randomUUID());
        request.setClientId(UUID.randomUUID());
        request.setScore(85);

        when(mlScoreService.saveScore(any(UUID.class), any(UUID.class), any(Integer.class))).thenReturn(null);

        ResponseEntity<?> responseEntity = mlScoreController.saveMlScore(request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Save ML score failed: advertiser or client not found", ((HashMap<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testSaveMlScoreException() {
        MlScoreRequest request = new MlScoreRequest();
        request.setAdvertiserId(UUID.randomUUID());
        request.setClientId(UUID.randomUUID());
        request.setScore(85);

        when(mlScoreService.saveScore(any(UUID.class), any(UUID.class), any(Integer.class))).thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> responseEntity = mlScoreController.saveMlScore(request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Save ML score failed: Unexpected error", ((HashMap<String, String>) responseEntity.getBody()).get("message"));
    }
}
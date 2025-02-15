package prod.individual.sirnilin.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.services.AdvertiserService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AdvertiserControllerTest {

    @Mock
    private AdvertiserService advertiserService;

    @InjectMocks
    private AdvertiserController advertiserController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testBulkInsertSuccess() {
        List<AdvertiserModel> advertiserModels = new ArrayList<>();
        AdvertiserModel advertiserModel = new AdvertiserModel();
        advertiserModel.setAdvertiserId(UUID.randomUUID());
        advertiserModel.setName("Test Advertiser");
        advertiserModels.add(advertiserModel);

        doNothing().when(advertiserService).bulkInsert(any(List.class));

        ResponseEntity<?> responseEntity = advertiserController.bulkInsert(advertiserModels);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(advertiserModels, responseEntity.getBody());
    }

    @Test
    public void testBulkInsertFailure() {
        List<AdvertiserModel> advertiserModels = new ArrayList<>();
        AdvertiserModel advertiserModel = new AdvertiserModel();
        advertiserModel.setAdvertiserId(UUID.randomUUID());
        advertiserModel.setName("Test Advertiser");
        advertiserModels.add(advertiserModel);

        doThrow(new RuntimeException("Bulk insert/update failed")).when(advertiserService).bulkInsert(any(List.class));

        ResponseEntity<?> responseEntity = advertiserController.bulkInsert(advertiserModels);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Bulk insert/update failed: Bulk insert/update failed", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testGetAdvertiserByIdSuccess() {
        UUID advertiserId = UUID.randomUUID();
        AdvertiserModel advertiserModel = new AdvertiserModel();
        advertiserModel.setAdvertiserId(advertiserId);
        advertiserModel.setName("Test Advertiser");

        when(advertiserService.getAdvertiserById(advertiserId)).thenReturn(advertiserModel);

        ResponseEntity<?> responseEntity = advertiserController.getAdvertiserById(advertiserId.toString());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(advertiserModel, responseEntity.getBody());
    }

    @Test
    public void testGetAdvertiserByIdNotFound() {
        UUID advertiserId = UUID.randomUUID();

        when(advertiserService.getAdvertiserById(advertiserId)).thenReturn(null);

        ResponseEntity<?> responseEntity = advertiserController.getAdvertiserById(advertiserId.toString());

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals("Advertiser not found", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testGetAdvertiserByIdInvalidUUID() {
        String invalidUUID = "invalid-uuid";

        ResponseEntity<?> responseEntity = advertiserController.getAdvertiserById(invalidUUID);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Invalid UUID format", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }
}
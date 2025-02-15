package prod.individual.sirnilin.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.models.CampaignModel;
import prod.individual.sirnilin.models.request.CampaignCreateRequest;
import prod.individual.sirnilin.models.request.CampaignUpdateRequest;
import prod.individual.sirnilin.services.AdvertiserService;
import prod.individual.sirnilin.services.CampaignService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @Mock
    private AdvertiserService advertiserService;

    @InjectMocks
    private AdvertiserController advertiserController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateCampaignSuccess() {
        UUID advertiserId = UUID.randomUUID();
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setImpressionsLimit(1000);
        request.setClicksLimit(100);
        request.setCostPerImpression(0.5f);
        request.setCostPerClick(1.0f);
        request.setAdTitle("Test Ad");
        request.setAdText("Test Ad Text");
        request.setStartDate(20250101);
        request.setEndDate(20251231);

        CampaignModel campaignModel = new CampaignModel();
        campaignModel.setCampaignId(UUID.randomUUID());
        campaignModel.setAdvertiserId(advertiserId);
        campaignModel.setImpressionsLimit(request.getImpressionsLimit());
        campaignModel.setClicksLimit(request.getClicksLimit());
        campaignModel.setCostPerImpression(request.getCostPerImpression());
        campaignModel.setCostPerClick(request.getCostPerClick());
        campaignModel.setAdTitle(request.getAdTitle());
        campaignModel.setAdText(request.getAdText());
        campaignModel.setStartDate(request.getStartDate());
        campaignModel.setEndDate(request.getEndDate());

        when(campaignService.createCampaign(any(CampaignCreateRequest.class), eq(advertiserId))).thenReturn(campaignModel);
        when(advertiserService.getAdvertiserById(advertiserId)).thenReturn(new AdvertiserModel());

        ResponseEntity<?> responseEntity = advertiserController.createCampaign(request, advertiserId.toString());

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(campaignModel, responseEntity.getBody());
    }

    @Test
    public void testCreateCampaignFailure() {
        UUID advertiserId = UUID.randomUUID();
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setImpressionsLimit(1000);
        request.setClicksLimit(100);
        request.setCostPerImpression(0.5f);
        request.setCostPerClick(1.0f);
        request.setAdTitle("Test Ad");
        request.setAdText("Test Ad Text");
        request.setStartDate(20250101);
        request.setEndDate(20251231);

        doThrow(new IllegalArgumentException("Create campaign failed")).when(campaignService).createCampaign(any(CampaignCreateRequest.class), eq(advertiserId));
        when(advertiserService.getAdvertiserById(advertiserId)).thenReturn(new AdvertiserModel());

        ResponseEntity<?> responseEntity = advertiserController.createCampaign(request, advertiserId.toString());

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Create campaign failed: Create campaign failed", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testGetCampaignSuccess() {
        UUID advertiserId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        CampaignModel campaignModel = new CampaignModel();
        campaignModel.setCampaignId(campaignId);
        campaignModel.setAdvertiserId(advertiserId);
        campaignModel.setImpressionsLimit(1000);
        campaignModel.setClicksLimit(100);
        campaignModel.setCostPerImpression(0.5f);
        campaignModel.setCostPerClick(1.0f);
        campaignModel.setAdTitle("Test Ad");
        campaignModel.setAdText("Test Ad Text");
        campaignModel.setStartDate(20250101);
        campaignModel.setEndDate(20251231);

        when(campaignService.getCampaign(campaignId)).thenReturn(campaignModel);

        ResponseEntity<?> responseEntity = advertiserController.getCampaign(advertiserId.toString(), campaignId.toString());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(campaignModel, responseEntity.getBody());
    }

    @Test
    public void testGetCampaignNotFound() {
        UUID advertiserId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        when(campaignService.getCampaign(campaignId))
                .thenThrow(new IllegalArgumentException("Campaign not found"));

        ResponseEntity<?> responseEntity = advertiserController.getCampaign(advertiserId.toString(), campaignId.toString());

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals("Campaign not found",
                ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testUpdateCampaignSuccess() {
        UUID advertiserId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setCostPerImpression(0.6f);
        request.setCostPerClick(1.1f);
        request.setAdTitle("Updated Ad");
        request.setAdText("Updated Ad Text");

        CampaignModel campaignModel = new CampaignModel();
        campaignModel.setCampaignId(campaignId);
        campaignModel.setAdvertiserId(advertiserId);
        campaignModel.setImpressionsLimit(1000);
        campaignModel.setClicksLimit(100);
        campaignModel.setCostPerImpression(request.getCostPerImpression());
        campaignModel.setCostPerClick(request.getCostPerClick());
        campaignModel.setAdTitle(request.getAdTitle());
        campaignModel.setAdText(request.getAdText());
        campaignModel.setStartDate(20250101);
        campaignModel.setEndDate(20251231);

        when(campaignService.updateCampaign(any(CampaignUpdateRequest.class), eq(campaignId), eq(advertiserId))).thenReturn(campaignModel);

        ResponseEntity<?> responseEntity = advertiserController.updateCampaign(request, advertiserId.toString(), campaignId.toString());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(campaignModel, responseEntity.getBody());
    }

    @Test
    public void testUpdateCampaignFailure() {
        UUID advertiserId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setCostPerImpression(0.6f);
        request.setCostPerClick(1.1f);
        request.setAdTitle("Updated Ad");
        request.setAdText("Updated Ad Text");

        doThrow(new IllegalArgumentException("Update campaign failed")).when(campaignService).updateCampaign(any(CampaignUpdateRequest.class), eq(campaignId), eq(advertiserId));

        ResponseEntity<?> responseEntity = advertiserController.updateCampaign(request, advertiserId.toString(), campaignId.toString());

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Update campaign failed: Update campaign failed", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testDeleteCampaignSuccess() {
        UUID advertiserId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        doNothing().when(campaignService).deleteCampaign(campaignId, advertiserId);

        ResponseEntity<?> responseEntity = advertiserController.deleteCampaign(advertiserId.toString(), campaignId.toString());

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    public void testDeleteCampaignFailure() {
        UUID advertiserId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        doThrow(new IllegalArgumentException("Delete campaign failed")).when(campaignService).deleteCampaign(campaignId, advertiserId);

        ResponseEntity<?> responseEntity = advertiserController.deleteCampaign(advertiserId.toString(), campaignId.toString());

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Delete campaign failed: Delete campaign failed", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }
}
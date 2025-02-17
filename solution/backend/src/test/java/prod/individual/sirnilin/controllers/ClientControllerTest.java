package prod.individual.sirnilin.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import prod.individual.sirnilin.models.ClientModel;
import prod.individual.sirnilin.models.Gender;
import prod.individual.sirnilin.services.ClientService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testBulkInsertSuccess() {
        List<ClientModel> clientModels = new ArrayList<>();
        ClientModel clientModel = new ClientModel();
        clientModel.setClientId(UUID.randomUUID());
        clientModel.setLogin("test_login");
        clientModel.setAge(25);
        clientModel.setLocation("test_location");
        clientModel.setGender(Gender.MALE);
        clientModels.add(clientModel);

        when(clientService.bulkInsert(any(List.class))).thenReturn(clientModels);

        ResponseEntity<?> responseEntity = clientController.bulkInsert(clientModels);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(clientModels, responseEntity.getBody());
    }

    @Test
    public void testBulkInsertFailure() {
        List<ClientModel> clientModels = new ArrayList<>();
        ClientModel clientModel = new ClientModel();
        clientModel.setClientId(UUID.randomUUID());
        clientModel.setLogin("test_login");
        clientModel.setAge(25);
        clientModel.setLocation("test_location");
        clientModel.setGender(Gender.ALL);
        clientModels.add(clientModel);

        doThrow(new IllegalArgumentException("Gender cannot be ALL")).when(clientService).bulkInsert(any(List.class));

        ResponseEntity<?> responseEntity = clientController.bulkInsert(clientModels);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Bulk insert/update failed: Gender cannot be ALL", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testGetClientByIdSuccess() {
        UUID clientId = UUID.randomUUID();
        ClientModel clientModel = new ClientModel();
        clientModel.setClientId(clientId);
        clientModel.setLogin("test_login");
        clientModel.setAge(25);
        clientModel.setLocation("test_location");
        clientModel.setGender(Gender.MALE);

        when(clientService.getClientById(clientId)).thenReturn(clientModel);

        ResponseEntity<?> responseEntity = clientController.getClientById(clientId.toString());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(clientModel, responseEntity.getBody());
    }

    @Test
    public void testGetClientByIdNotFound() {
        UUID clientId = UUID.randomUUID();

        when(clientService.getClientById(clientId)).thenReturn(null);

        ResponseEntity<?> responseEntity = clientController.getClientById(clientId.toString());

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals("Client not found", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }

    @Test
    public void testGetClientByIdInvalidUUID() {
        String invalidUUID = "invalid-uuid";

        ResponseEntity<?> responseEntity = clientController.getClientById(invalidUUID);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Invalid UUID format", ((Map<String, String>) responseEntity.getBody()).get("message"));
    }
}
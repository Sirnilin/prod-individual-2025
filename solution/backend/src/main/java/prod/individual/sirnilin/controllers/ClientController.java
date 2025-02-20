package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prod.individual.sirnilin.models.ClientModel;
import prod.individual.sirnilin.services.ClientService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/bulk")
    public ResponseEntity<?> bulkInsert(@Valid @RequestBody List<ClientModel> clientModels) {
        try {
            clientService.bulkInsert(clientModels);
            return ResponseEntity.status(HttpStatus.OK).body(clientModels);
        } catch (Exception e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Bulk insert/update failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<?> getClientById(@PathVariable String clientId) {
        try {
            UUID uuid = UUID.fromString(clientId);
            ClientModel client = clientService.getClientById(uuid);

            if (client == null) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Client not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            return ResponseEntity.status(HttpStatus.OK).body(client);
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid UUID format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}

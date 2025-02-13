package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.sirnilin.models.ClientModel;
import prod.individual.sirnilin.services.ClientService;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    final private ClientService clientService;

    @PostMapping("/bulk")
    public ResponseEntity<?> bulkInsert(@Valid @RequestBody List<ClientModel> clientModels) {
        List<ClientModel> insertedClients = clientService.bulkInsert(clientModels);
        if (insertedClients == null) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(insertedClients);
    }

}

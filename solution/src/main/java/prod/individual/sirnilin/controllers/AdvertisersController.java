package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.sirnilin.models.AdvertisersModel;
import prod.individual.sirnilin.services.AdvertisersService;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/advertisers")
@RequiredArgsConstructor
public class AdvertisersController {

    final private AdvertisersService advertisersService;

    @PostMapping("/bulk")
    public ResponseEntity<?> bulkInsert(@Valid @RequestBody List<AdvertisersModel> advertisersModels) {
        if (advertisersService.bulkInsert(advertisersModels)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(advertisersModels);
        } else {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Bulk insert failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

}

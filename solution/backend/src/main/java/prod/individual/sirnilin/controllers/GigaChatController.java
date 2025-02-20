package prod.individual.sirnilin.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import prod.individual.sirnilin.models.request.GenerateAdTextRequest;

@RestController
@RequestMapping("/gigachat")
@RequiredArgsConstructor
public class GigaChatController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gigachat.url}")
    private String gigachatUrl;


    @GetMapping("/generateAdText")
    public ResponseEntity<?> generateAdText(@RequestBody GenerateAdTextRequest request) {
        String url = gigachatUrl + "/generate_ad";
        HttpEntity<GenerateAdTextRequest> entity = new HttpEntity<>(request);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}

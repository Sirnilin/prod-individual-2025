package prod.individual.sirnilin.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prod.individual.sirnilin.models.request.BannedWordRequest;
import prod.individual.sirnilin.services.ProfanityFilterService;

@RestController
@RequestMapping("/banned-words")
@RequiredArgsConstructor
public class BannedWordController {

    private final ProfanityFilterService profanityFilterService;

    @PostMapping("/add")
    public ResponseEntity<?> addBannedWord(@RequestBody BannedWordRequest request) {
        try {
            profanityFilterService.addBannedWord(request.getWords());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add banned word: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeBannedWord(@RequestBody BannedWordRequest request) {
        try {
            profanityFilterService.removeBannedWord(request.getWords());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove banned word: " + e.getMessage());
        }
    }
}

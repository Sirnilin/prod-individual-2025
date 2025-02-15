package prod.individual.sirnilin.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prod.individual.sirnilin.models.TimeModel;
import prod.individual.sirnilin.models.request.TimeRequest;
import prod.individual.sirnilin.repositories.TimeRepository;

@RestController
@RequestMapping("/time")
@RequiredArgsConstructor
public class TimeController {

    final private TimeRepository timeRepository;
    final private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/advance")
    public ResponseEntity<?> updateCurrentDate(@Valid @RequestBody TimeRequest timeRequest) {
        TimeModel timeModel = timeRepository.findById(1l).orElse(new TimeModel());
        timeModel.setCurrentDate(timeRequest.getCurrentDate());
        timeRepository.save(timeModel);
        redisTemplate.opsForValue().set("currentDate", timeRequest.getCurrentDate());
        return ResponseEntity.ok().body(timeModel);
    }
}

package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.repositories.AdvertiserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvertiserService {

    final private AdvertiserRepository advertiserRepository;

    public void bulkInsert(List<AdvertiserModel> advertiserModels) {
        advertiserRepository.saveAll(advertiserModels);
    }

    public AdvertiserModel getAdvertiserById(UUID advertiserId) {
        return advertiserRepository.findByAdvertiserId(advertiserId).orElse(null);
    }
}

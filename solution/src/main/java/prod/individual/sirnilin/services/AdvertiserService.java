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

    public boolean bulkInsert(List<AdvertiserModel> advertiserModels) {
        for (AdvertiserModel model : advertiserModels) {
            if (model.getName() == null ||
                    model.getName().isEmpty() ||
                    model.getName().length() > 255 ||
                    model.getAdvertiserId() == null ||
                    advertiserRepository.findByAdvertiserId(model.getAdvertiserId()) != null
            ) {
                return false;
            }
        }

        advertiserRepository.saveAll(advertiserModels);
        return true;
    }

    public AdvertiserModel getAdvertiserById(UUID advertiserId) {
        return advertiserRepository.findByAdvertiserId(advertiserId);
    }
}

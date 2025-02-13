package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.AdvertiserModel;
import prod.individual.sirnilin.repositories.AdvertisersRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvertisersService {

    final private AdvertisersRepository advertisersRepository;

    public boolean bulkInsert(List<AdvertiserModel> advertiserModels) {
        for (AdvertiserModel model : advertiserModels) {
            if (model.getName() == null ||
                    model.getName().isEmpty() ||
                    model.getName().length() > 255 ||
                    model.getAdvertiserId() == null ||
                    advertisersRepository.findByAdvertiserId(model.getAdvertiserId()) != null
            ) {
                return false;
            }
        }

        advertisersRepository.saveAll(advertiserModels);
        return true;
    }

    public AdvertiserModel getAdvertiserById(UUID advertiserId) {
        return advertisersRepository.findByAdvertiserId(advertiserId);
    }
}

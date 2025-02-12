package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.AdvertisersModel;
import prod.individual.sirnilin.repositories.AdvertisersRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisersService {

    final private AdvertisersRepository advertisersRepository;

    public boolean bulkInsert(List<AdvertisersModel> advertisersModels) {
        for (AdvertisersModel model : advertisersModels) {
            if (model.getName() == null ||
                    model.getName().isEmpty() ||
                    model.getName().length() > 255 ||
                    model.getAdvertiserId() == null ||
                    advertisersRepository.findByAdvertiserId(model.getAdvertiserId()) != null
            ) {
                return false;
            }
        }

        advertisersRepository.saveAll(advertisersModels);
        return true;
    }
}

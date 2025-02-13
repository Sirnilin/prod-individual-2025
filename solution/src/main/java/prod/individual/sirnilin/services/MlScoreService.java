package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.MlScoreModel;
import prod.individual.sirnilin.repositories.AdvertiserRepository;
import prod.individual.sirnilin.repositories.ClientRepository;
import prod.individual.sirnilin.repositories.MlScoreRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MlScoreService {

    final private MlScoreRepository mlScoreRepository;
    final private AdvertiserRepository advertiserRepository;
    final private ClientRepository clientRepository;

    public MlScoreModel saveScore(UUID advertiserId, UUID clientId, int score) {
        if (clientRepository.findByClientId(clientId).isEmpty() ||
                advertiserRepository.findByAdvertiserId(advertiserId).isEmpty()
        ) {
            return null;
        }

        MlScoreModel mlScoreModel = mlScoreRepository.findByAdvertiserIdAndClientId(advertiserId, clientId)
                .orElse(new MlScoreModel());
        mlScoreModel.setAdvertiserId(advertiserId);
        mlScoreModel.setClientId(clientId);
        mlScoreModel.setScore(score);
        return mlScoreRepository.save(mlScoreModel);
    }
}

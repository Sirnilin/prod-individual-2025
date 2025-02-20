package prod.individual.sirnilin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prod.individual.sirnilin.models.ClientModel;
import prod.individual.sirnilin.models.Gender;
import prod.individual.sirnilin.repositories.ClientRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public List<ClientModel> bulkInsert(List<ClientModel> clientModels) {
        for (ClientModel clientModel : clientModels) {
            if (clientModel.getGender().equals(Gender.ALL)) {
                throw new IllegalArgumentException("Gender cannot be ALL");
            }
        }
        return clientRepository.saveAll(clientModels);
    }

    public boolean clientExists(UUID clientId) {
        return clientRepository.findByClientId(clientId).isPresent();
    }

    public ClientModel getClientById(UUID clientId) {
        return clientRepository.findByClientId(clientId).orElse(null);
    }
}

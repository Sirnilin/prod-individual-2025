package prod.individual.sirnilin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProdIndividualApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProdIndividualApplication.class, args);
    }

}

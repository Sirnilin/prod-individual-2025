package prod.individual.sirnilin.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import prod.individual.sirnilin.repositories.AdvertiserRepository;
import prod.individual.sirnilin.repositories.CampaignRepository;
import prod.individual.sirnilin.repositories.ClientRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetricsExporter {

    private final ClientRepository clientRepository;
    private final AdvertiserRepository advertiserRepository;
    private final CampaignRepository campaignRepository;
    private final MeterRegistry registry;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${service.url}")
    private String serviceUrl;

    @PostConstruct
    public void initMetrics() {
        registry.gauge("user_count", this, MetricsExporter::getUserCount);
        registry.gauge("advertiser_count", this, MetricsExporter::getAdvertiserCount);
        registry.gauge("campaign_count", this, MetricsExporter::getCampaignCount);
        registry.gauge("view_count", this, MetricsExporter::getViewCount);
        registry.gauge("click_count", this, MetricsExporter::getClickCount);
        registry.gauge("earnings", this, MetricsExporter::getEarnings);
    }

    private int getUserCount() {
        return clientRepository.findAll().size();
    }

    private int getAdvertiserCount() {
        return advertiserRepository.findAll().size();
    }

    private int getCampaignCount() {
        return campaignRepository.findAll().size();
    }

    private int getViewCount() {
        String url = serviceUrl + "/stats/views";
        return restTemplate.getForObject(url, Integer.class);
    }

    private int getClickCount() {
        String url = serviceUrl + "/stats/clicks";
        return restTemplate.getForObject(url, Integer.class);
    }

    private double getEarnings() {
        String url = serviceUrl + "/stats/earnings";
        return restTemplate.getForObject(url, Double.class);
    }

    public void recordImpression(UUID campaignId, int day) {
        String metricName = "ads_impressions";
        registry.counter(metricName, Tags.of("campaign_id", campaignId.toString(), "day", String.valueOf(day)))
                .increment();
    }

    public void recordClick(UUID campaignId, int day) {
        String metricName = "ads_clicks";
        registry.counter(metricName, Tags.of("campaign_id", campaignId.toString(), "day", String.valueOf(day)))
                .increment();
    }

}

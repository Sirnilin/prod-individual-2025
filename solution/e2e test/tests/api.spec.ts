import { test, expect, request } from '@playwright/test';

test('full E2E scenario', async () => {
    // Create an API context using the base URL from configuration
    const apiContext = await request.newContext({
        baseURL: process.env.BASE_URL || 'http://localhost:8080',
    });

    // 1. Create advertiser(s) using POST /advertisers/bulk
    const advertisersPayload = [
        {
            advertiser_id: '11111111-1111-1111-1111-111111111111',
            name: 'Advertiser 1'
        }
    ];
    let response = await apiContext.post('/advertisers/bulk', {
        data: advertisersPayload
    });
    expect(response.ok()).toBeTruthy();

    // 2. Create a campaign for the advertiser using POST /advertisers/{advertiserId}/campaigns
    const advertiserId = advertisersPayload[0].advertiser_id;
    const campaignPayload = {
        impressions_limit: 1000,
        clicks_limit: 100,
        cost_per_impression: 0.5,
        cost_per_click: 1.0,
        ad_title: 'Campaign 1',
        ad_text: 'Great campaign',
        start_date: Math.floor(Date.now() / 1000),
        end_date: Math.floor(Date.now() / 1000) + 3600,
        targeting: {
            gender: 'ALL',
            location: 'USA',
            age_from: 18,
            age_to: 65
        }
    };
    response = await apiContext.post(`/advertisers/${advertiserId}/campaigns`, {
        data: campaignPayload
    });
    expect(response.ok()).toBeTruthy();
    const campaignData = await response.json();
    // Assuming the response includes a field "campaignId"
    const campaignId = campaignData.campaignId || 'campaign-1';

    // 3. Get the created campaign using GET /advertisers/{advertiserId}/campaigns/{campaignId}
    response = await apiContext.get(`/advertisers/${advertiserId}/campaigns/${campaignId}`);
    expect(response.ok()).toBeTruthy();

    // 4. Add ML score for a client using POST /ml-scores
    const mlScorePayload = {
        score: 85,
        advertiser_id: advertiserId,
        client_id: '22222222-2222-2222-2222-222222222222'
    };
    response = await apiContext.post('/ml-scores', {
        data: mlScorePayload
    });
    expect(response.ok()).toBeTruthy();

    // 5. Simulate an ad click by the client using POST /ads/{adId}/click
    // Here we assume an ad identifier; using campaignId as a placeholder.
    const adsClickPayload = {
        client_id: mlScorePayload.client_id
    };
    response = await apiContext.post(`/ads/${campaignId}/click`, {
        data: adsClickPayload
    });
    expect(response.ok()).toBeTruthy();

    // 6. Advance the day using POST /time/advance
    const timePayload = {
        current_date: Math.floor(Date.now() / 1000) + 86400 // next day
    };
    response = await apiContext.post('/time/advance', {
        data: timePayload
    });
    expect(response.ok()).toBeTruthy();

    // 7. Retrieve campaign statistics using GET /stats/advertisers/{advertiserId}/campaigns
    response = await apiContext.get(`/stats/advertisers/${advertiserId}/campaigns`);
    expect(response.ok()).toBeTruthy();

    // Additionally, get daily statistics using GET /stats/advertisers/{advertiserId}/campaigns/daily
    response = await apiContext.get(`/stats/advertisers/${advertiserId}/campaigns/daily`);
    expect(response.ok()).toBeTruthy();
});
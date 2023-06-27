package com.mobichord.ftps.service.snclient;

import static com.appchord.util.JsonUtil.fromObjectToString;

import com.appchord.data.tenant.TenantConfig;
import com.mobichord.ftps.service.data.RequestData;
import com.mobichord.servicenow.api.http.ServiceNowHttpClient;
import com.mobichord.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceNowClientImpl implements ServiceNowClient {
    private static final String REQUEST_PATH = "/api/x_mobi_p/processes/queue";

    private final TenantService tenantService;

    private ServiceNowHttpClient client;

    void setClient(ServiceNowHttpClient client) {
        this.client = client;
    }

    @Override
    public void post(RequestData requestData) {
        if (client == null) {
            String tenantCode = tenantService.getCurrentTenantConfig().getTenant().getTenantCode();
            var config = tenantService.createSnClientConfig(tenantCode);
            client = new ServiceNowHttpClient(config);
        }
        var requestBody = RequestBody.create(fromObjectToString(requestData), ServiceNowHttpClient.JSON);

        try (var ignore = client.post(REQUEST_PATH, requestBody)) {
            log.trace("Request sent.");
        } catch (Exception e) {
            log.error("Failed to send data to '{}'.", REQUEST_PATH, e);
            throw e;
        }
    }
}

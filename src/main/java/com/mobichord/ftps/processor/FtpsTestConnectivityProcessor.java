package com.mobichord.ftps.processor;

import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.appchord.messages.Signal;
import com.appchord.messages.SignalSelectionProperty;
import com.appchord.messages.SignalSelector;
import com.appchord.messages.tenant.TestConnectivityRequest;
import com.appchord.messages.tenant.TestConnectivityResponse;
import com.appchord.messages.tenant.TestConnectivityResult;
import com.mobichord.ftps.service.FtpsFileServiceFactory;
import com.mobichord.ftps.service.FtpsService;
import com.mobichord.messaging.*;
import com.mobichord.tenant.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Arrays.asList;

@Service
public final class FtpsTestConnectivityProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(FtpsTestConnectivityProcessor.class);
    private static final String TEST_CONNECTIVITY_FTPS = "FTPS";
    private static final String SELECTOR_GROUP = "UTILS";
    private static final String SELECTOR_TYPE = "SFTP";

    private final TenantService tenantService;

    private final FtpsFileServiceFactory ftpsFileServiceFactory;

    private final Sender sender;

    @Autowired
    public FtpsTestConnectivityProcessor(TenantService tenantService, FtpsFileServiceFactory ftpsFileServiceFactory, Sender sender) {
        this.tenantService = tenantService;
        this.ftpsFileServiceFactory = ftpsFileServiceFactory;
        this.sender = sender;
    }

    @SignalSelector(property = SignalSelectionProperty.TEST_CONNECTIVITY_SERVICE, value = TEST_CONNECTIVITY_FTPS)
    @SignalSelector(property = SignalSelectionProperty.TENANT_CONFIGURATION_GROUP, value = SELECTOR_GROUP)
    @SignalSelector(property = SignalSelectionProperty.TENANT_CONFIGURATION_TYPE, value = SELECTOR_TYPE)
    private class TestConnectivityRequestHandler extends SignalHandler<TestConnectivityRequest> {
        @Override
        public void handle(TestConnectivityRequest request) {
            var response = TestConnectivityResponse.builder()
                    .service(request.getService())
                    .targetTenantCode(request.getTargetTenantCode())
                    .group(request.getGroup())
                    .type(request.getType())
                    .tenantConfigurationId(request.getTenantConfigurationId())
                    .build();

            SftpConfiguration config = tenantService.getConfiguration(request.getTargetTenantCode(),
                    request.getTenantConfigurationId(), SftpConfiguration.class);

            if (config == null) {
                response.setResult(TestConnectivityResult.SKIPPED);
                response.setOk(true);
                sender.send(response);
                return;
            }

            logger.info("Checking FTPS connectivity.");

            var currentTenantCode = tenantService.getCurrentTenantCode();
            try {
                tenantService.setCurrentTenantCode(config.getTenantKey());
                FtpsService ftpsFileService = ftpsFileServiceFactory.createFtpsFileService(config, "");
                ftpsFileService.connectToFtpsServer();
                logger.info("FTPS Test connectivity complete!");
                response.setResult(TestConnectivityResult.SUCCESS);
                response.setOk(true);
                sender.send(response);
            } catch (Exception ex) {
                var errorMessage = String.format("FTPS test connectivity failed: %s", ex.getMessage());
                logger.warn(errorMessage, ex);
                response.setResult(TestConnectivityResult.FAILURE);
                response.setOk(false);
                response.setMessage(errorMessage);
                sender.send(response);
            } finally {
                tenantService.setCurrentTenantCode(currentTenantCode);
            }
        }
    }

    @Override
    public List<SignalHandler<? extends Signal>> getHandlers() {
        return asList(new TestConnectivityRequestHandler());
    }
}

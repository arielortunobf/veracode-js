package com.mobichord.ftps.processor;

import com.mobichord.ftps.message.ftps.FtpsUploadRequest;
import com.mobichord.ftps.message.ftps.FtpsUploadResponse;
import com.mobichord.ftps.service.AttachmentApiFactory;
import com.mobichord.ftps.service.ClientFtpsService;
import com.mobichord.messaging.Sender;
import com.mobichord.servicenow.api.http.ServiceNowClientConfiguration;
import com.mobichord.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtpsProcessorTest {

    @InjectMocks
    FtpsProcessor ftpsProcessor;

    @Mock
    private ClientFtpsService clientFtpsService;

    @Mock
    private TenantService tenantService;

    @Mock
    private Sender sender;

    @Mock
    private AttachmentApiFactory apiFactory;

    @Mock
    private ServiceNowClientConfiguration snClientConfig;

    @Test
    public void testFtpsUploadRequestHandlerHandle() {


//        doNothing().when(snClientConfig).setLoggingOptions(any(LoggingInterceptor.LoggingOptions.class));

//        when(snClientConfig.setLoggingOptions(any(LoggingInterceptor.LoggingOptions.class))).thenReturn(snClientConfig);

        FtpsUploadRequest request = mock(FtpsUploadRequest.class);
        FtpsProcessor.FtpsUploadRequestHandler handler = ftpsProcessor.new FtpsUploadRequestHandler();

//        FtpsConf ftpsConf =  FtpsConf.builder()
//                .host("host")
//                .port(21)
//                .username("user")
//                .password("pass") //might need to decrypt??
//                .path(FtpsUtils.getFinalDirectoryPath("dirPath", "requestPath"))
//                .build();

//        when(request.getConfig()).thenReturn(ftpsConf);
//        when(request.getTenantCode()).thenReturn("tenantCode");
//        when(request.getAttachmentIds()).thenReturn(List.of("1", "2", "3"));

        handler.handle(request);

        verify(sender, times(1)).send(any(FtpsUploadResponse.class));
    }

}
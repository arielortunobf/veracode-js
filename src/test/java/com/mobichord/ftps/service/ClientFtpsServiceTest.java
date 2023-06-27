package com.mobichord.ftps.service;

import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.mobichord.ftps.data.DownloadRequest;
import com.mobichord.ftps.message.ftps.FtpsDownloadResponse;
import com.mobichord.tenant.TenantService;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientFtpsServiceTest {

    @InjectMocks
    private ClientFtpsService clientFtpsService;

    @Mock
    private FtpsFileServiceFactory ftpsFileServiceFactory;

    @Mock
    private TenantService tenantService;

    @Mock
    private FtpsFileService ftpsFileService;

    @Mock
    private FTPSClient ftpsClient;

//    @Test
    void uploadFileForClient() throws IOException {
        SftpConfiguration cfg = new SftpConfiguration();
        InputStream inputStream = mock(InputStream.class);
        String fileNameWithExtension = "test.txt";
        String aId = "id";

        when(ftpsFileServiceFactory.createFtpsFileService(cfg, "/")).thenReturn(ftpsFileService);
        when(ftpsFileService.connectToFtpsServer()).thenReturn(ftpsClient);

        clientFtpsService.uploadFileForClient(cfg, inputStream, fileNameWithExtension, aId,  "/", true);

        verify(ftpsFileService, times(1)).uploadFile(ftpsClient, inputStream, fileNameWithExtension, false);
    }
//
//    @Test
    void downloadFileForClient() throws IOException {
        // Arrange
        SftpConfiguration config = new SftpConfiguration();
        DownloadRequest request = new DownloadRequest();
        request.setPath("/");
        request.setFtpsCfgId("123");

        when(tenantService.getConfiguration(request.getFtpsCfgId(), SftpConfiguration.class)).thenReturn(config);
        when(ftpsFileServiceFactory.createFtpsFileService(config, request.getPath() )).thenReturn(ftpsFileService);
        when(ftpsFileService.connectToFtpsServer()).thenReturn(ftpsClient);
        when(ftpsFileService.downloadFile(ftpsClient, tenantService, request)).thenReturn(new FtpsDownloadResponse());

        FtpsDownloadResponse response = clientFtpsService.downloadFileForClient(request);

        // Assert
        verify(ftpsFileServiceFactory).createFtpsFileService(config,request.getPath() );
        verify(ftpsFileService).connectToFtpsServer();
        verify(ftpsFileService).downloadFile(ftpsClient, tenantService, request);
        assertNotNull(response);

    }
}
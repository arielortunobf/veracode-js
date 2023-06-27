package com.mobichord.ftps.service;

import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FtpsFileServiceTest {

    @InjectMocks
    private FtpsFileService ftpsFileService;
    @Mock
    private FTPSClient ftpsClient;

    @Mock
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        ftpsFileService = spy(new FtpsFileService(encryptionService));
    }


    @Test
    void uploadServiceTest() throws IOException {
        String localFilePath = "path/to/local/file";
        String remoteFilePath = "path/to/remote/file";

        FileInputStream fileInputStream = mock(FileInputStream.class);

        doCallRealMethod().when(ftpsFileService).uploadFile(localFilePath, remoteFilePath);
        doReturn(ftpsClient).when(ftpsFileService).createFtpsClient();
        doReturn(fileInputStream).when(ftpsFileService).createFileInputStream(localFilePath);

        ftpsFileService.uploadFile(localFilePath, remoteFilePath);

        verify(ftpsFileService).createFtpsClient();
        verify(ftpsFileService).createFileInputStream(localFilePath);
        verify(ftpsClient).storeFile(remoteFilePath, fileInputStream);
        verify(ftpsClient).logout();
        verify(ftpsClient).disconnect();
        verify(fileInputStream).close();
    }

    @Test
    void downloadFile() throws IOException {
        String remoteFilePath = "path/to/remote/file";
        String localFilePath = "path/to/local/file";

        FileOutputStream fileOutputStream = mock(FileOutputStream.class);

        doCallRealMethod().when(ftpsFileService).downloadFile(remoteFilePath, localFilePath);
        doReturn(ftpsClient).when(ftpsFileService).createFtpsClient();
        doReturn(fileOutputStream).when(ftpsFileService).createFileOutputStream(localFilePath);

        ftpsFileService.downloadFile(remoteFilePath, localFilePath);

        verify(ftpsFileService).createFtpsClient();
        verify(ftpsFileService).createFileOutputStream(localFilePath);
        verify(ftpsClient).retrieveFile(remoteFilePath, fileOutputStream);
        verify(ftpsClient).logout();
        verify(ftpsClient).disconnect();
        verify(fileOutputStream).close();
    }

}
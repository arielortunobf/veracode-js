package com.mobichord.ftps.service;

import com.appchord.data.tenant.configurations.SftpConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FtpsServiceFactoryTest {

    @Mock
    private SftpConfiguration config;

    @InjectMocks
    private FtpsFileServiceFactory factory;

    @Test
    public void testCreateFtpsFileService() {
//        // Arrange
//        when(config.getHost()).thenReturn("testHost");
//        when(config.getPort()).thenReturn(21);
//        when(config.getUsername()).thenReturn("testUser");
//        when(config.getPassword()).thenReturn("testPass");
//        when(config.getPath()).thenReturn("testPath");
//
//        // Act
//        FtpsService ftpsService = factory.createFtpsFileService(config, "/test");
//
//        // Assert
////        assertEquals("testHost", ftpsService.getFtpsHost());
////        assertEquals(21, ftpsService.getFtpsPort());
////        assertEquals("testUser", ftpsService.getFtpsUsername());
////        assertEquals("testPass", ftpsService.getFtpsPassword());
////        assertEquals("testPath", ftpsService.getPath());
    }

}
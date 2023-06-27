package com.mobichord.ftps.rest;

import com.appchord.data.tenant.TenantConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobichord.ftps.TestConfiguration;
import com.mobichord.ftps.data.Options;
import com.mobichord.ftps.data.UploadRequest;
import com.mobichord.ftps.service.ClientFtpsService;
import com.mobichord.tenant.TenantService;
import com.mobichord.tracking.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class})
//@ActiveProfiles("test")
//@SpringBootTest(classes = App.class)
@WebMvcTest(FtpsController.class)
class FtpsControllerTest {

    @Mock
    private ClientFtpsService clientFtpsService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private TenantService tenantService;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private FtpsController ftpsController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
//        ftpsController = new FtpsController(null, null, clientFtpsService, trackingService, tenantService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(ftpsController).build();
    }

    @Test
    public void testCheckIfUps() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(content().string("yes"));
    }

    @Test
    public void testCheckIfUp() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("yes"));
    }

//    @Test
    public void testUploadFile() throws Exception {

        // Arrange
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setPath("testpaht");
        uploadRequest.setEncrypt(false);
        uploadRequest.setAttachments(Arrays.asList("test", "attacj,emt"));
        Options options = new Options();
        options.setOverwrite(false);
        options.setEncryptedFileExt("test");
        options.setAfterAction("after");
        uploadRequest.setOptions(options);
        uploadRequest.setFtpsCfgId("test");
        uploadRequest.setCryptoCfgId("test");
        uploadRequest.setProcessId("test");
        TenantConfig tc = mock(TenantConfig.class);
        when(tenantService.getCurrentTenantConfig()).thenReturn(tc);
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println(objectMapper.writeValueAsString(uploadRequest));

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }


    @Test
    void downloadFile() {
    }
}
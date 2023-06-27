package com.mobichord.ftps;

import com.mobichord.ftps.service.ClientFtpsService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Profile("test")
@Configuration
public class TestConfiguration  {

//    static {
//        System.setProperty("ftps.host", "ftps.payclearly.com");
//        System.setProperty("ftps.port", "21");
//    }

    @Bean
    public ClientFtpsService clientFtpsService() {
        return mock(ClientFtpsService.class);
    }

}

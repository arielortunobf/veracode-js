package com.mobichord.ftps.app;

import com.appchord.data.tenant.configurations.AwsConfiguration;
import com.mobichord.boot.RestAppConfiguration;
import com.mobichord.docker.DockerConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan(basePackages = {
        "com.mobichord.ftps.rest",
        "com.mobichord.ftps.saga",
        "com.mobichord.ftps.processor",
        "com.mobichord.ftps.service"
})
@Import({AwsConfiguration.class, DockerConfiguration.class})
public class FtpsAppConfiguration extends RestAppConfiguration {
}

package com.mobichord.ftps.app;

import com.mobichord.boot.AppModule;
import com.mobichord.boot.Entrypoint;

import static com.mobichord.boot.AppModule.AppType.REST;

public class App {

    public static void main(String[] args) {
        Entrypoint.start("integration-ftps", args,
                         new AppModule(REST, "ftps", "/ftps/*", FtpsAppConfiguration.class)
        );
    }
}

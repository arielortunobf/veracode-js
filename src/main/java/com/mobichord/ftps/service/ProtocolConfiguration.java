package com.mobichord.ftps.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtocolConfiguration {

    private String type;
    private String host;
    private int port;
    private String username;
    private String password;
    private String passwordSalt;
    private String path;
    private String basePath;
    private String requestPath;
}

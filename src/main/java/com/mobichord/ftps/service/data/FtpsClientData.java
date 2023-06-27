package com.mobichord.ftps.service.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FtpsClientData {

    private String ftpsHost;
    private String ftpsUsername;
    private String ftpsPassword;

}

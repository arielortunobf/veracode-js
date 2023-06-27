package com.mobichord.ftps.data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public abstract class FtpsDataRequest {
    @NotNull
    @NotBlank
    private String processId;
    @NotNull @NotBlank
    private String ftpsCfgId;
    private String cryptoCfgId;

    @NotNull @NotBlank
    private String path;
    private Options options;
}

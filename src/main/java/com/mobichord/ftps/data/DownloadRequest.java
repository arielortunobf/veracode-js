package com.mobichord.ftps.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DecryptValidationAnnotation
public class DownloadRequest {

    @NotNull @NotBlank
    private String processId;
    @NotNull @NotBlank
    private String ftpsCfgId;
    private String cryptoCfgId;
//    @NotNull
    private boolean decrypt;
    private String path;
    private String fileMask;
    @NotNull @NotBlank
    private String targetSysId;
    @NotNull @NotBlank
    private String targetTable;
    private String encryptionContext;
    private Options options;
    private String protocol;
    private String protocolCfgId;

}

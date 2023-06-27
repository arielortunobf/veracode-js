package com.mobichord.ftps.message;


import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UploadCommand extends AbstractProtocolCommand {

    private List<String> attachmentIds;
    private String cryptoCfgId;
    private boolean encrypt;
    private String path;
    private boolean overwrite;
    private String encryptedFileExt;

    //add configurations to be passed from the initial request
    private String requestPath;
    private String type;
    private String protocol;
    private String configId;
//    private String activityId;
//    private String ftpsCfgId;
//    private String sourceTenantCode;
}

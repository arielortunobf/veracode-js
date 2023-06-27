package com.mobichord.ftps.message;


import com.appchord.messages.Command;
import com.mobichord.ftps.message.ftps.AbstractFtpsCommand;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolUploadCommand extends Command {

    private List<String> attachmentIds;
    private String cryptoCfgId;
    private boolean encrypt;
    private String path;
    private boolean overwrite;
    private String encryptedFileExt;
    //add configurations to be passed from the initial request
    private String activityId;
    private String protocolCfgId;
    private String sourceTenantCode;
    private String protocol;
}

package com.mobichord.ftps.message;

import com.appchord.messages.Request;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProtocolDownloadRequest extends Request {

    private String cryptoCfgId;
    private boolean encrypt;
    private String requestPath;
    private String targetSysId;
    private String targetTable;
    private String fileMask;
    private String activityId;
    private String protocolCfgId;
    private String protocol;

    //added for downloading single file
    private String adjustedFileName;
    private String fileName;
    private boolean decrypt;
    private String encryptionContext;
    private String afterAction;

}

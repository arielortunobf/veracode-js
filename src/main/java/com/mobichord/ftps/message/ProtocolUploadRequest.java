package com.mobichord.ftps.message;

import com.appchord.messages.Request;
import com.mobichord.ftps.message.ftps.AbstractFtpsRequest;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProtocolUploadRequest extends Request {

    private List<String> attachmentIds;
    private String cryptoCfgId;
    private boolean encrypt;
    private String path;
    private boolean overwrite;
    private String encryptedFileExt;
    private String activityId;
    private String protocolCfgId;
    private String protocol;
}

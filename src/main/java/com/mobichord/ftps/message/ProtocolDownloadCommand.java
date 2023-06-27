package com.mobichord.ftps.message;


import com.appchord.messages.Command;
import com.mobichord.ftps.data.Options;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolDownloadCommand extends Command {

    private String activityId;
    private String processId;
    private String protocol;
    private String protocolCfgId;
    private String cryptoCfgId;
    private boolean decrypt;
    private String path;
    private String fileMask;
    private String targetSysId;
    private String targetTable;
    private String encryptionContext;
    private Options options;
    private String afterAction;

}

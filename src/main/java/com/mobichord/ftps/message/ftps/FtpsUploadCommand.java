package com.mobichord.ftps.message.ftps;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FtpsUploadCommand extends AbstractFtpsCommand {
    private List<String> attachmentIds;
    private String cryptoCfgId;
    private boolean encrypt;
    private String path;
    private boolean overwrite;
    private String encryptedFileExt;
    //add configurations to be passed from the initial request
}

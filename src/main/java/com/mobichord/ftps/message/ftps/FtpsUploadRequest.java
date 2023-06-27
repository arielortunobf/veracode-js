package com.mobichord.ftps.message.ftps;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FtpsUploadRequest extends AbstractFtpsRequest {
    private List<String> attachmentIds;
    private String cryptoCfgId;
    private boolean encrypt;
    private String path;
    private boolean overwrite;
    private String encryptedFileExt;
}

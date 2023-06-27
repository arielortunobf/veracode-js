package com.mobichord.ftps.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EncryptValidationAnnotation
public class UploadRequest {

    // where to get?
    @NotNull @NotBlank
    private String processId;
    private String ftpsCfgId;
    private String cryptoCfgId;
    private String protocol;
    @NotNull
    private boolean encrypt;
//    @NotNull @NotBlank
    private String protocolCfgId;
    //directory?
    private String path;
    @NotEmpty
    private List<@NotBlank String> attachments;
    private Options options;

}

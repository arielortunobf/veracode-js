package com.mobichord.ftps.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Options {

    private boolean overwrite;
    private String encryptedFileExt;
    private String afterAction;
}

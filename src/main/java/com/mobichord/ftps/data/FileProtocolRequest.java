package com.mobichord.ftps.data;

import com.mobichord.servicenow.api.http.AttachmentDownloadResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FileProtocolRequest {

//    private AttachmentDownloadResponse attachment;
    private boolean isOverWrite;
    private InputStream stream;
    private String fileName;

    //downloads
    private String fileMask;
    private String adjustedFileName;
    private String oldFileName;
    private String newFileName;

}

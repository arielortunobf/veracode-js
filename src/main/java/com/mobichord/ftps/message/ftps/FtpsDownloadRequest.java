package com.mobichord.ftps.message.ftps;

import com.mobichord.ftps.data.DownloadRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FtpsDownloadRequest extends AbstractFtpsRequest {
    private DownloadRequest request;
}

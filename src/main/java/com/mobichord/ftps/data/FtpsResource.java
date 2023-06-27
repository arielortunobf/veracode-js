package com.mobichord.ftps.data;

import com.appchord.data.PulledResource;
import lombok.Data;

@Data
public abstract class FtpsResource extends PulledResource {
    private FtpsStatus ftpsStatus;
    private String errorCode;
    private String errorDescription;
}

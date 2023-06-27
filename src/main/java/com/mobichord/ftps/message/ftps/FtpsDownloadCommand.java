package com.mobichord.ftps.message.ftps;


import com.mobichord.ftps.data.DownloadRequest;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FtpsDownloadCommand extends AbstractFtpsCommand {
    private DownloadRequest request;
}

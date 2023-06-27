package com.mobichord.ftps.message.ftps;

import com.appchord.messages.Command;
import lombok.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FtpsCommand extends Command {

    String syncTaskId;
    String uploadActivityId;
    List<OutputStream> outputStreamList;
    List<InputStream> inputStreamList;
    //todo : additional fields needed for the service
}

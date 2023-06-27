package com.mobichord.ftps.message;


import com.appchord.messages.Request;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AbstractProtocolRequest extends Request {
    private String activityId;
    private String protocolCfgId;
}

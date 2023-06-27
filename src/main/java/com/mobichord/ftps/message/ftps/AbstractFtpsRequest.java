package com.mobichord.ftps.message.ftps;


import com.appchord.messages.Request;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AbstractFtpsRequest extends Request {
    private String activityId;
    private String ftpsCfgId;
}

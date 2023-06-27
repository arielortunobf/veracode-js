package com.mobichord.ftps.message.ftps;

import com.appchord.messages.Command;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractFtpsCommand extends Command {
    String activityId;
    String ftpsCfgId;
    String sourceTenantCode;
}

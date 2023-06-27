package com.mobichord.ftps.message;

import com.appchord.messages.Command;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractProtocolCommand extends Command {
    private String activityId;
    private String protocolCfgId;
    private String sourceTenantCode;
}

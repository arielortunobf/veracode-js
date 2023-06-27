package com.mobichord.ftps.message;

import com.appchord.messages.Response;
import lombok.Data;

@Data
public class AbstractProtocolResponse extends Response {
    private String message;

    public void appendMessage(String message) {
        if (this.message == null || this.message.isBlank()) {
            this.message = message;
        } else {
            this.message += " \n " + message;
        }
    }
}

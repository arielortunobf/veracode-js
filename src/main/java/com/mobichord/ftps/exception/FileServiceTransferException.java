package com.mobichord.ftps.exception;

import org.springframework.http.HttpStatus;

import java.io.IOException;

public class FileServiceTransferException extends IOException {

    public FileServiceTransferException() {
        super();
    }

    public FileServiceTransferException(String message) {
        super(message);
    }

    public FileServiceTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileServiceTransferException(HttpStatus status, String message){
        super();
    }
}

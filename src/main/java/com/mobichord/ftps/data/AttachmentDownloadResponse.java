package com.mobichord.ftps.data;

import lombok.Getter;
import okhttp3.Response;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.lang3.StringUtils.isBlank;


public class AttachmentDownloadResponse implements Closeable {
    private final Response response;

    @Getter
    private String fileName;

    @Getter
    private long contentLength;

    public AttachmentDownloadResponse(Response response) {
        this.response = response;

        var contentDisposition = response.header("Content-Disposition");
        if (isBlank(contentDisposition)) {
            throw new RuntimeException("Could not read attachment, no disposition");
        }

        String[] split = contentDisposition.split("filename=");
        fileName = split.length >= 1 ? split[1] : null;

        contentLength = response.body().contentLength();
    }

    public InputStream stream() {
        return this.response.body().byteStream();
    }

    @Override
    public void close() throws IOException {
        this.response.close();
    }
}

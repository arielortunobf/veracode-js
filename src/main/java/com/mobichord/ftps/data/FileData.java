package com.mobichord.ftps.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileData {

    private String fileName;
    private InputStream inputStream;
}

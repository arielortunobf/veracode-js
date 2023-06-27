package com.mobichord.ftps.utility;

import com.mobichord.ftps.data.AttachmentDownloadResponse;
import com.mobichord.security.chipher.AesChipher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FtpsUtilsTest {

    @Test
    public void testGetNewFileName() {
        String originalFileName = "test.txt";
        String remoteFilePath = "/remote/path/";

        String newFileName = FtpsUtils.getNewFileName(originalFileName, remoteFilePath);

        assertTrue(newFileName.startsWith(remoteFilePath + "test"));
        assertTrue(newFileName.endsWith(".txt"));
    }

    @Test
    public void testGetAttachmentById() {
        String id = "testId";

        AttachmentDownloadResponse response = FtpsUtils.getAttachmentById(id);

        assertNotNull(response);
    }

    @Test
    public void testAdjustAttachmentFileNames() {
        List<String> files = new ArrayList<>();
        files.add("/remote/path/file1.txt");
        files.add("/remote/path/file2.txt");
        files.add("/remote/path/file3.txt");

        Map<String, String> result = FtpsUtils.adjustAttachmentFileNames(files, "/");

        assertEquals("file1.txt", result.get("/remote/path/file1.txt"));
        assertEquals("file2.txt", result.get("/remote/path/file2.txt"));
        assertEquals("file3.txt", result.get("/remote/path/file3.txt"));
    }

    @Test
    public void testDecryptPassword(){
        //FaSQBW@Mt9LtbZH

//        String secret = "74e9d450";
        String salt = "74e9d450";
        String secret="ZPNt3TXq6HnZnVBP";
        String text = "0IHA+QU8VpIBs+CPI1sEqG+FLHMqp6/QnGVitkKAitjSq0C9FUzwYcb1H2McR6dw";
        String decryptedText = AesChipher.decrypt(text, salt+secret);

        System.out.println(decryptedText);


//        assertEquals(decryptedText, "@MobiCh0rd");
    }
}

package com.mobichord.ftps.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.util.TrustManagerUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

class RealFileServiceTest {


    @Test
    void realTimeCreateDirTest() {
        String server = "ftps.payclearly.com";
        int port = 21;
        String username = "brightfin_admin";
        String password = "MomXyYcmCB@7#88a";
        String localFilePath = "src/test/resources/Paymentfile-.csv";
        String basedir = "/uploads";
        String remoteFilePath = "/gp";

        FTPSClient ftpsClient = new FTPSClient("TLS", false);
        ftpsClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        ftpsClient.setEnabledProtocols(new String[]{"TLSv1.2"});

        try {
            ftpsClient.connect(server, port);
            ftpsClient.login(username, password);

//            ftpsClient.execPBSZ(0);  // Set protection buffer size
//            ftpsClient.execPROT("P"); // Set data channel protection to private
            ftpsClient.enterLocalPassiveMode();

            boolean isDirExisting = ftpsClient.changeWorkingDirectory(basedir+remoteFilePath);

            if (isDirExisting) {
                System.out.println("Successfully changed working directory.");
            } else {
                System.out.println("Failed to change working directory. Check if the path is correct.");
                //change to base dir
                ftpsClient.changeWorkingDirectory(basedir);
                System.out.println("Successfully changed working directory to base directory.");
            }

            String[] directories = ftpsClient.listNames();

            boolean exists = Arrays.asList(directories).contains(remoteFilePath);

            if (exists) {
                System.out.println("Directory exists");
            } else {
                System.out.println("Directory does not exist");
                //create directory now
                boolean created = ftpsClient.makeDirectory(basedir+remoteFilePath);
                if (created) {
                    System.out.println("Directory created successfully");
                } else {
                    System.out.println("Failed to create directory");
                }
            }

            ftpsClient.logout();
            ftpsClient.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Test
    void realTimeUploadTest() {

        String server = "ftps.payclearly.com";
        int port = 21;
        String username = "brightfin_admin";
        String password = "MomXyYcmCB@7#88a";
        String localFilePath = "src/test/resources/Paymentfile-.csv";
        String remoteFilePath = "/uploads/java/";

        FTPSClient ftp = new FTPSClient("TLS", false);
        ftp.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        ftp.setEnabledProtocols(new String[]{"TLSv1.2"});

        try {
            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return;
            }

            if (!ftp.login(username, password)) {
                ftp.logout();
                System.err.println("FTP server login failed.");
                return;
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.LOCAL_FILE_TYPE);
//            ftp.setFileType(FTP.BINARY_FILE_TYPE);
//            ftp.execPBSZ(0);
//            ftp.execPROT("P");

            File localFile = new File(localFilePath);
            String fileName = localFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String fileNameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String fileExtension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String fileNameWithTimestamp = remoteFilePath + fileNameWithoutExtension + timestamp + "." + fileExtension;
            System.out.println(fileNameWithTimestamp);

            int bufferSize = 1024;

            InputStream inputStream = new BufferedInputStream(new FileInputStream(localFile), bufferSize);
            ftp.storeFile(fileNameWithTimestamp, inputStream);


            System.out.println("File uploaded successfully.");

            ftp.logout();

        } catch (IOException e) {
            System.err.println("FTP client error: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    System.err.println("Failed to disconnect from FTP server: " + e.getMessage());
                }
            }
        }
    }

    @Test
    void realTimeUploadInputStreamTest() {

        String server = "ftps.payclearly.com";
        int port = 21;
        String username = "brightfin_admin";
        String password = "MomXyYcmCB@7#88a";
        String localFilePath = "src/test/resources/Paymentfile-.csv";
        String remoteFilePath = "/uploads/testdev01/";

        FTPSClient ftp = new FTPSClient("TLS", false);
        ftp.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        ftp.setEnabledProtocols(new String[]{"TLSv1.2"});

        try {
            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return;
            }

            if (!ftp.login(username, password)) {
                ftp.logout();
                System.err.println("FTP server login failed.");
                return;
            }

            ftp.enterLocalPassiveMode();
//            ftp.setFileType(FTP.LOCAL_FILE_TYPE);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            File localFile = new File(localFilePath);
            String fileName = localFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String fileNameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String fileExtension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String fileNameWithTimestamp = remoteFilePath + fileNameWithoutExtension + timestamp + "." + fileExtension;
            System.out.println(fileNameWithTimestamp);

            int bufferSize = 1024;

            InputStream inputStream = new BufferedInputStream(new FileInputStream(localFile), bufferSize);
            ftp.storeFile(fileNameWithTimestamp, inputStream);


            System.out.println("File uploaded successfully.");

            ftp.logout();

        } catch (IOException e) {
            System.err.println("FTP client error: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    System.err.println("Failed to disconnect from FTP server: " + e.getMessage());
                }
            }
        }


    }

    @Test
    void realTimeDownloadTest() {

        String server = "ftps.payclearly.com";
        int port = 21;
        String username = "brightfin_admin";
        String password = "MomXyYcmCB@7#88a";
        String remoteFilePath = "/uploads/java/";
        String localDownloadDir = System.getProperty("user.home") + "/Downloads/ftps/";

        FTPSClient ftp = new FTPSClient("TLS", false);
        ftp.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        ftp.setEnabledProtocols(new String[]{"TLSv1.2"});

        try {
            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return;
            }

            if (!ftp.login(username, password)) {
                ftp.logout();
                System.err.println("FTP server login failed.");
                return;
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            String remoteFileName = "Paymentfile-20230508_201429.csv";
            String localFilePath = localDownloadDir + remoteFileName;

            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePath));

            if (ftp.retrieveFile(remoteFilePath + remoteFileName, outputStream)) {
                System.out.println("File downloaded successfully to: " + localFilePath);
            } else {
                System.err.println("File download failed.");
            }

            ftp.logout();

        } catch (IOException e) {
            System.err.println("FTP client error: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    System.err.println("Failed to disconnect from FTP server: " + e.getMessage());
                }
            }
        }
    }

//    @Test
    void realTimeMaskDownloadTest() {

        String server = "ftps.payclearly.com";
        int port = 21;
        String username = "brightfin_admin";
        String password = "MomXyYcmCB@7#88a";
        String remoteFilePath = "/uploads/testdev01/";
        String fileMAsk = "*";
        String localDownloadDir = System.getProperty("user.home") + "/Downloads/ftps/";

        FTPSClient ftp = new FTPSClient("TLS", false);
        ftp.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        ftp.setEnabledProtocols(new String[]{"TLSv1.2"});

        try {
            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return;
            }

            if (!ftp.login(username, password)) {
                ftp.logout();
                System.err.println("FTP server login failed.");
                return;
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            String remoteFileName = "Paymentfile-20230508_201429.csv";
            String localFilePath = localDownloadDir + remoteFileName;

//            FTPFile[] files;
//            FTPFile[] files = ftp.listFiles(remoteFilePath);

            String patter = "sn_certservice*.xlsx";
            String patt = cleanPattern(patter);
            System.out.println(wildcardToRegex(patt));
            System.out.println(isValidRegex(patt));
            System.out.println(isValidRegex(wildcardToRegex(patt)));
            List<String> matchingFiles = new ArrayList<>();

            FTPFile[] files = ftp.listFiles(remoteFilePath, new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile file) {
                    Pattern compiledPattern = Pattern.compile(wildcardToRegex(patter));
                    if (compiledPattern.matcher(file.getName()).matches()) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

//            String patt = "*file-2023*.csv";


            for (FTPFile file : files) {
               //dl files
                System.out.println(file.getName());
                InputStream is = ftp.retrieveFileStream(remoteFilePath + file.getName());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }



            //download files


//            List<String> filePaths = Arrays.stream(files)
//                    .map(file -> FilenameUtils.concat(remoteFilePath, file.getName()))
//                    .collect(Collectors.toList());
//
//           filePaths.forEach(System.out::println);
//
//            List<String> list = Arrays.asList("/uploads/testdev01/Paymentfile-20230526_235540.csv", "/uploads/testdev01/Paymentfile-20230529_181622.csv", "/uploads/testdev01/Paymentfile-20230526_171733.csv");

//            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePath));

//            if (ftp.retrieveFile(remoteFilePath + remoteFileName, outputStream)) {
//                System.out.println("File downloaded successfully to: " + localFilePath);
//            } else {
//                System.err.println("File download failed.");
//            }

            ftp.logout();

        } catch (IOException e) {
            System.err.println("FTP client error: " + e.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    System.err.println("Failed to disconnect from FTP server: " + e.getMessage());
                }
            }
        }
    }

    public static boolean isValidRegex(String regex) {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }

    public String wildcardToRegex(String wildcard) {
        return wildcard.replace("*", ".*");
    }

    public static final List<Character> META_CHARACTERS = List.of('.', '^', '$', '*', '+', '-', '?', '(', ')', '[', ']', '{', '}', '\\', '|');

    public static String cleanPattern(String pattern) {
        char[] chars = pattern.toCharArray();
        int index = 0;
        for (char c : chars) {
            if (META_CHARACTERS.indexOf(c) >= 0) {
                pattern = pattern.substring(0, index) + "\\\\" + pattern.substring(index);
                index += 2;
            }
            index++;
        }
        return pattern;
    }
}
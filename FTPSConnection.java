
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import org.apache.commons.net.ftp.FTPSClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.net.ftp.FTPFile;

public class FTPSConnection {

    public static void main(String[] args) {
        Collection<String> message = new ArrayList<>();
        String server = "";
        int port = 990;
        String user = "";
        String pass = "";
        String path = "";

        FTPSClient ftpsClient = new FTPSClient(true);  // true = Implicit FTPS (port 990)
        ftpsClient.addProtocolCommandListener(new org.apache.commons.net.PrintCommandListener(new java.io.PrintWriter(System.out), true));
        try {
            ftpsClient.setConnectTimeout(30000);  // 30 seconds timeout
            ftpsClient.setDataTimeout(30000);
            System.out.println("Establish connection to the FTP server...");
            System.out.println("Connecting to server...");
            ftpsClient.connect(server, port);
            System.out.println("Perform SSL/TLS handshake...");
            if (ftpsClient.login(user, pass)) {
                System.out.println("Connected and logged in.");
                ftpsClient.enterLocalPassiveMode();
                try {
                    String ftpFile = "test.txt";
                    StringBuilder localpath = new StringBuilder();
                    localpath.append("//d://test").append("/");
                    File f = new File(localpath.toString());
                    if (!f.exists()) {
                        f.mkdir();
                    }
                    localpath.append("/").append(ftpFile);
                    String fileData = "test file";
                    try ( FileWriter writer = new FileWriter(localpath.toString())) {
                        writer.write(fileData);
                    }
                    System.out.println("Uploading file...");
                    try ( FileInputStream inputStream = new FileInputStream(localpath.toString())) {
                        boolean done = ftpsClient.storeFile(ftpFile, inputStream);
                        if (done) {
                            System.out.println("File uploaded successfully.");
                        } else {
                            System.out.println("Failed to upload file.");
                        }
                    }
                    Thread.sleep(40000);  // Wait for 10 seconds
                    String errorDir = "/ERROR/";      // FTP folder for error files
                    String processedDir = "/PROCESSED/";  // FTP folder for processed files
                    createDirectoryIfNotExists(ftpsClient, errorDir);
                    createDirectoryIfNotExists(ftpsClient, processedDir);
                    String nameWithoutExtension = ftpFile.split("\\.")[0];
                    FTPFile[] files = ftpsClient.listFiles();
                    for (FTPFile file : files) {
                        String fileType = file.isDirectory() ? "Directory" : "File";
                        System.out.println(fileType + ": " + file.getName());
                        String remoteFilePath = file.getName();
                        String fileExtension = getFileExtension(remoteFilePath);
                        if (file.isFile() && remoteFilePath.contains(nameWithoutExtension)) {
                            System.out.println("Found file: " + remoteFilePath);
                            try ( InputStream inpStream = ftpsClient.retrieveFileStream(remoteFilePath);  BufferedReader reader = new BufferedReader(new InputStreamReader(inpStream, StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if ("man".equalsIgnoreCase(fileExtension)) {
                                        message.add("File processed successfully....  " + line);
                                    } else {
                                        message.add(line);
                                    }
                                }
                            } catch (Exception e) {

                            }
                        }
                        if ("err".equalsIgnoreCase(fileExtension)) {
                            moveFile(ftpsClient, remoteFilePath, errorDir);
                        } else if ("man".equalsIgnoreCase(fileExtension)) {
                            moveFile(ftpsClient, remoteFilePath, processedDir);
                        }
                    }
                } catch (IOException | InterruptedException ex) {
                }
                ftpsClient.setFileType(FTPSClient.BINARY_FILE_TYPE);
                ftpsClient.logout();
                System.out.println("Logged out successfully.");
            } else {
                System.out.println("Login failed.");
            }
        } catch (IOException ex) {
        } finally {
            try {
                if (ftpsClient.isConnected()) {
                    ftpsClient.disconnect();
                    System.out.println("Disconnected.");
                }
            } catch (IOException ex) {
            }
        }
    }

    private static String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        return (lastIndexOfDot == -1) ? "" : fileName.substring(lastIndexOfDot + 1);
    }

    private static void moveFile(FTPSClient ftpsClient, String fileName, String targetDir) throws IOException {
        String sourcePath = "/" + fileName;
        String destinationPath = targetDir + fileName;
        boolean success = ftpsClient.rename(sourcePath, destinationPath);
        if (success) {
            System.out.println("Moved file: " + fileName + " to " + targetDir);
        } else {
            System.out.println("Failed to move file: " + fileName);
        }
    }

    private static void createDirectoryIfNotExists(FTPSClient ftpsClient, String dirPath) throws IOException {
        boolean dirExists = ftpsClient.changeWorkingDirectory(dirPath);
        if (!dirExists) {
            boolean created = ftpsClient.makeDirectory(dirPath);
            if (created) {
                System.out.println("Created directory: " + dirPath);
            } else {
                System.out.println("Failed to create directory: " + dirPath);
            }
        }
    }
}

package com.httplib;

import com.beust.jcommander.internal.Lists;
import com.httplib.util.HttpWebDAVClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author : pgajjar
 * @since  : 7/13/16
 */
public class HttpFileUploader {
    private static Logger log = org.apache.log4j.Logger.getLogger(HttpFileUploader.class.getName());

    public enum HttpWebDavHost {
        RW05("RW05", "http://localhost/uploads/"),
        RW15("RW15", "http://localhost/uploads/"),
        LOCAL("LOCAL", "http://localhost/uploads/"),
        UNKNOWN("UNKNOWN", "http://unknown");

        private final @Nonnull String env;
        private final @Nonnull String httpHostWebDAVBaseUrl;

        HttpWebDavHost(@Nonnull final String _env, @Nonnull final String _httpHostWebDAVBaseUrl) {
            env = _env;
            httpHostWebDAVBaseUrl = _httpHostWebDAVBaseUrl;
        }


        @Nonnull
        public String getEnv() {
            return env;
        }

        @Nonnull
        public String getHttpHostWebDAVBaseUrl() {
            return httpHostWebDAVBaseUrl;
        }

        @Nonnull
        public static HttpWebDavHost host(@Nullable final String queueName) {
            return (queueName != null) ? HttpWebDavHost.valueOf(queueName.trim().toUpperCase()) : UNKNOWN;
        }
    }

    @Nonnull private final Collection<File> inputDataFiles;
    @Nonnull private final HttpWebDavHost httpWebDAVHost;

    @Nonnull private final HttpWebDAVClient httpWebDAVClient;

    private HttpFileUploader(@Nonnull final String[] files, @Nonnull final HttpWebDavHost httpWebDAVHost) {
        this.httpWebDAVClient = HttpWebDAVClient.newInstance(httpWebDAVHost.getHttpHostWebDAVBaseUrl());
        this.httpWebDAVHost = httpWebDAVHost;

        inputDataFiles = Lists.newArrayList();

        for (String file : files) {
            final File inputFile = new File(file);
            if (inputFile.exists()) {
                inputDataFiles.add(inputFile);
            } else {
                log.info("File: " + file + " doesn't exist, ignoring it.");
            }
        }
    }

    @Nonnull
    public static HttpFileUploader newInstance(@Nonnull final String[] files, @Nonnull final HttpWebDavHost httpWebDAVHost) {
        return new HttpFileUploader(files, httpWebDAVHost);
    }

    public void close() throws IOException {
        httpWebDAVClient.close();
    }


    private void uploadFilesToHttpWebDAVHost() throws IOException, URISyntaxException {
        @Nonnull final String remoteFetchSpaceDir = remoteFetchSpaceDirectory();
        @Nonnull final String remoteDiagnosticDir = remoteDiagnosticDirectory();

        @Nonnull final HttpWebDAVClient httpWebDAVClient = HttpWebDAVClient.newInstance(httpWebDAVHost.getHttpHostWebDAVBaseUrl());
        try {
            // upload files to temp - fetch space
            boolean result = uploadFilesToRemoteFetchSpace(httpWebDAVClient, remoteFetchSpaceDir);
            if (!result) {
                throw new RuntimeException("Failed uploading some file(s) to FetchSpace path: " + remoteFetchSpaceDir);
            }

            // now move the files from fetch space to Diagnostic Path.
            result = moveRemoteFilesFromFetchSpaceToDiagnosticPath(httpWebDAVClient, remoteFetchSpaceDir, remoteDiagnosticDir);

            if (!result) {
                throw new RuntimeException("Failed moving some file(s) from FetchSpace path: " + remoteFetchSpaceDir + " to Diagnostic path:" + remoteDiagnosticDir);
            }

            // now cleanup fetch space
            httpWebDAVClient.rmRecursive(remoteFetchSpaceDir);
        } finally {
            httpWebDAVClient.close();
        }
    }

    private boolean uploadFilesToRemoteFetchSpace(@Nonnull final HttpWebDAVClient httpWebDAVClient, @Nonnull final String remoteFetchSpaceDir) throws IOException, URISyntaxException {
        // push all of the files to the respective HTTP WebDAV Host (RW05 / RW15) in FetchSpace
        for (Iterator filesIter = inputDataFiles.iterator(); filesIter.hasNext(); ) {
            final File currentFile = (File) filesIter.next();

            boolean result = httpWebDAVClient.upload(currentFile, remoteFetchSpaceDir, currentFile.getName());
            log.info("Push " + (result ? "succeed" : "failed") + " for: " + currentFile + ", upload path: " + httpWebDAVClient.remoteDirPath(remoteFetchSpaceDir, currentFile.getName()));

            // if failed upload - return false.
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private boolean moveRemoteFilesFromFetchSpaceToDiagnosticPath(@Nonnull final HttpWebDAVClient httpWebDAVClient, @Nonnull final String remoteFetchSpaceDir, @Nonnull final String remoteDiagnosticDir) throws IOException, URISyntaxException {
        // move all the files from fetchspace to diagnostic path
        for (Iterator filesIter = inputDataFiles.iterator(); filesIter.hasNext(); ) {
            final File currentFile = (File) filesIter.next();

            boolean result = httpWebDAVClient.move(remoteFetchSpaceDir, currentFile.getName(), remoteDiagnosticDir, currentFile.getName());
            log.info("Move " + (result ? "succeed" : "failed") + " for: " + currentFile + ", from Path: " + httpWebDAVClient.remoteDirPath(remoteFetchSpaceDir, currentFile.getName()) + " to Path: " + httpWebDAVClient.remoteDirPath(remoteDiagnosticDir, currentFile.getName()));

            // if failed move - return false.
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private String safeName(String name) {
        return StringUtils.replaceChars(name, "!@#$%^&*()_- ", "");
    }

    @Nonnull
    private String remoteFetchSpaceDirectory() throws UnknownHostException {
        return ("FetchSpace" + File.separator + InetAddress.getLocalHost().getHostName() + File.separator + "1010101010101" + "." + "1234566789" + "-" + safeName(httpWebDAVHost.env) + "-" +
                StringUtils.replaceChars("usertestdata", '#', '_'));
    }

    @Nonnull
    private String remoteDiagnosticDirectory() {
        String userName = (System.getProperty("user.name") != null ? System.getProperty("user.name") : "_system");
        return ("Diagnostic" + File.separator + userName + File.separator + "testdata" + File.separator + "usertestdata" + File.separator);
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        if (args.length < 1) {
            System.out.println("usage: HttpFileUploader <list of files to upload>");
            System.exit(0);
        }

        // Get the queue name to determine the HTTP WebDAV Host (either RW05 or RW15)
        HttpWebDavHost httpWebDavHost = HttpWebDavHost.host("LOCAL");
        HttpFileUploader uploader = HttpFileUploader.newInstance(args, httpWebDavHost);
        uploader.uploadFilesToHttpWebDAVHost();
        uploader.close();

//        System.out.println("Uploaded: " + uploader.upload("/Users/pgajjar/Data/Movies/PK.mp4", "pgajjar/example/test/firsttest/package/provider/vfcs/source/Components/files/", "PK.mp4"));
//        System.out.println("Moved: " + uploader.move("pgajjar/example/test/firsttest/package/provider/vfcs/source/Components/files/", "PK.mp4", "move_worked/example/test/firsttest/package/provider/vfcs/source/Components/files/", "PK.mp4"));
    }
}

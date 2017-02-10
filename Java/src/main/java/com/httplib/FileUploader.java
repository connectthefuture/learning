package com.httplib;

import com.beust.jcommander.internal.Lists;
import com.httplib.util.HttpFileUploader;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : pgajjar
 * @since  : 7/13/16
 */
public class FileUploader {
    private static Logger logger = org.apache.log4j.Logger.getLogger(FileUploader.class.getName());

    public enum HttpWebDavHost {
        RW05("RW05", "http://localhost/Gossamer/"),
        RW15("RW15", "http://localhost/Gossamer/"),
        LOCAL("LOCAL", "http://localhost/Gossamer/"),
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
    @Nonnull private final HttpFileUploader worker;

    public FileUploader(@Nonnull final Collection<File> files, @Nonnull final HttpWebDavHost httpWebDAVHost) {
        inputDataFiles = Lists.newArrayList();

        for (File inputFile : files) {
            if (inputFile.exists()) {
                inputDataFiles.add(inputFile);
            } else {
                logger.info("File: " + inputFile + " doesn't exist, ignoring it.");
            }
        }

        worker = new HttpFileUploader(inputDataFiles, "123456789012323", httpWebDAVHost);
    }

    private void moveFilesToDiagnosticPath() throws IOException, URISyntaxException {
        worker.uploadFiles();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        if (args.length < 2) {
            System.out.println("usage: FileUploader <Directory to upload> <webdavhost>");
            System.exit(0);
        }

        final File dir = new File(args[0]);
        if (!dir.exists()) {
            System.out.println("Looks like you provided a directory " + args[0] + " that doesn't exist.");
            System.out.println("usage: FileUploader <Directory to upload> <webdavhost>");
            System.exit(0);
        }

        // final File[] filesExceptHiddenFiles = dir.listFiles(file -> !file.isHidden());
        List<File> filesExceptHiddenFiles = FileUtils.listFiles(dir, null, true).stream().filter(f -> !f.isHidden()).collect(Collectors.toList());

        // Get the queue name to determine the HTTP WebDAV Host
        HttpWebDavHost httpWebDavHost = HttpWebDavHost.host(args[1]);
        FileUploader uploader = new FileUploader(filesExceptHiddenFiles, httpWebDavHost);
        uploader.moveFilesToDiagnosticPath();
    }
}

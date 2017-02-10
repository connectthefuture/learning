package com.httplib;

import com.beust.jcommander.internal.Lists;
import com.httplib.util.HttpFileUploader;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * @author : pgajjar
 * @since  : 7/13/16
 */
public class FileUploader {
    private static Logger logger = org.apache.log4j.Logger.getLogger(FileUploader.class.getName());

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
    @Nonnull private final HttpFileUploader worker;

    public FileUploader(@Nonnull final String[] files, @Nonnull final HttpWebDavHost httpWebDAVHost) {
        inputDataFiles = Lists.newArrayList();

        for (String file : files) {
            final File inputFile = new File(file);
            if (inputFile.exists()) {
                inputDataFiles.add(inputFile);
            } else {
                logger.info("File: " + file + " doesn't exist, ignoring it.");
            }
        }

        worker = new HttpFileUploader(inputDataFiles, "123456789012323", httpWebDAVHost);
    }

    private void moveFilesToDiagnosticPath() throws IOException, URISyntaxException {
        worker.uploadFiles();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        if (args.length < 1) {
            System.out.println("usage: FileUploader <list of files to upload>");
            System.exit(0);
        }

        // Get the queue name to determine the HTTP WebDAV Host
        HttpWebDavHost httpWebDavHost = HttpWebDavHost.host("LOCAL");
        FileUploader uploader = new FileUploader(args, httpWebDavHost);
        uploader.moveFilesToDiagnosticPath();
    }
}

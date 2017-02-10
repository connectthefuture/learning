package com.httplib.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static com.httplib.FileUploader.HttpWebDavHost;

/**
 * @author  : pgajjar
 * @since   : 02/09/2017
 */
public class HttpFileUploader {
    private static final Logger logger = Logger.getLogger(HttpFileUploader.class);

    protected @Nonnull final String workId;
    protected @Nullable HttpWebDavHost httpWebDAVHost;

    private static final String userName = (System.getProperty("user.name") != null ? System.getProperty("user.name") : "_system");

    protected Collection<File> inputDataFiles = Collections.EMPTY_SET;

    public HttpFileUploader(@Nonnull final Collection<File> inputDataFiles, @Nonnull String id, HttpWebDavHost httpWebDAVHost) {
        this.inputDataFiles = inputDataFiles;
        this.workId = id;
        this.httpWebDAVHost = httpWebDAVHost;
    }

    public void uploadFiles() throws IOException, URISyntaxException {
        @Nonnull final String remoteTempPath = remoteTempPath();
        @Nonnull final String remotePath = remotePath();

        @Nonnull final HttpWebDAVClient httpWebDAVClient = HttpWebDAVClient.newInstance(httpWebDAVHost.getHttpHostWebDAVBaseUrl());
        try {
            // upload files to temp
            boolean result = uploadFilesToRemoteTempPath(httpWebDAVClient, remoteTempPath);
            if (!result) {
                throw new RuntimeException("Failed uploading some file(s) to Temp path: " + remoteTempPath);
            }

            // now move the files from temp path to actual Path.
            result = moveRemoteFilesFromTempPathToActualPath(httpWebDAVClient, remoteTempPath, remotePath);

            if (!result) {
                throw new RuntimeException("Failed moving some file(s) from Temp path: " + remoteTempPath + " to Actual path:" + remotePath);
            }

            // now cleanup fetch space
            result = httpWebDAVClient.delete(remoteTempPath);
            if (!result) {
                logger.debug("Failed cleaning up Temp Path:" + remoteTempPath);
            }
        } finally {
            httpWebDAVClient.close();
        }
    }

    private boolean uploadFilesToRemoteTempPath(@Nonnull final HttpWebDAVClient httpWebDAVClient, @Nonnull final String remoteTempPath) throws IOException, URISyntaxException {
        for (Iterator filesIter = inputDataFiles.iterator(); filesIter.hasNext(); ) {
            final File currentFile = (File) filesIter.next();

            final String urlEncodedFileName = URLEncoder.encode(currentFile.getName(), "UTF-8");

            boolean result = httpWebDAVClient.upload(currentFile, remoteTempPath, urlEncodedFileName);
            logger.info("Push " + (result ? "succeed" : "failed") + " for: " + currentFile + ", upload path: " + httpWebDAVClient.remoteFilePath(remoteTempPath, currentFile.getName()));

            // if failed upload - return false.
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private boolean moveRemoteFilesFromTempPathToActualPath(@Nonnull final HttpWebDAVClient httpWebDAVClient, @Nonnull final String remoteTempPath, @Nonnull final String remotePath) throws IOException, URISyntaxException {
        for (Iterator filesIter = inputDataFiles.iterator(); filesIter.hasNext(); ) {
            final File currentFile = (File) filesIter.next();

            final String urlEncodedFileName = URLEncoder.encode(currentFile.getName(), "UTF-8");

            boolean result = httpWebDAVClient.move(remoteTempPath, urlEncodedFileName, remotePath, urlEncodedFileName);
            logger.info("Move " + (result ? "succeed" : "failed") + " for: " + currentFile +
                    ", from Path: " + httpWebDAVClient.remoteFilePath(remoteTempPath, currentFile.getName()) +
                    " to Path: " + httpWebDAVClient.remoteFilePath(remotePath, currentFile.getName()));

            // if failed move - return false.
            if (!result) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    private String remoteTempPath() throws UnsupportedEncodingException, UnknownHostException {
        return ("TempPath" + File.separator +
                URLEncoder.encode(hostnameWithId(), "UTF-8") + File.separator +
                URLEncoder.encode("1234567890" + "." + "9876543210" + "-" + safeName(httpWebDAVHost.getEnv()) + "-" +
                        StringUtils.replaceChars("testdata#verynice@file_test", '#', '_'), "UTF-8") + File.separator);
    }

    @Nonnull
    private String remotePath() throws UnsupportedEncodingException {
        return ("ActualPath" + File.separator +
                URLEncoder.encode(userName, "UTF-8") + File.separator +
                URLEncoder.encode("testmovieprovider#full#video", "UTF-8") + File.separator +
                URLEncoder.encode(StringUtils.replaceChars("testdata#verynice@file_test", '#', '_'), "UTF-8") + File.separator);
    }

    @Nonnull
    private String safeName(String name) {
        return StringUtils.replaceChars(name, "!@#$%^&*()_- ", "");
    }

    @Nonnull
    private String hostnameWithId() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
}


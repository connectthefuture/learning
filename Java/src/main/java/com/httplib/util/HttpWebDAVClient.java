package com.httplib.util;

import com.httplib.method.HttpMkCol;
import com.httplib.method.HttpMove;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * @author  : pgajjar
 * @since   : 7/13/16
 *
 * HttpComponents 4.5.1 version - old version working library
 * <p>
 * Implements following HTTP methods,
 * (1) PUT
 * (2) MKCOL
 * (3) DELETE
 * (4) HEAD
 * (5) MOVE
 */
public final class HttpWebDAVClient {
    private static final Logger logger = Logger.getLogger(HttpWebDAVClient.class);
    private final String httpHostWebDAVBaseUrl;

    private static final int DIAGNOSTIC_PUSH_SOLUTION_RETRIES = 3;

    @Nonnull
    private final CloseableHttpClient httpClient;

    private HttpWebDAVClient(@Nonnull final String _httpHostWebDAVBaseUrl) {
        httpHostWebDAVBaseUrl = _httpHostWebDAVBaseUrl;
        httpClient = HttpClients.createDefault();
    }

    public static HttpWebDAVClient newInstance(@Nonnull final String _httpHostUrl) {
        return new HttpWebDAVClient(_httpHostUrl);
    }

    private static boolean successfulResponse(final int responseCode) {
        // Accept both 200, 201 and 204 for backwards-compatibility reasons
        return responseCode == HttpStatus.SC_CREATED || responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_NO_CONTENT;
    }

    /**
     * It gives path of dir and file both
     */
    @Nonnull
    private String remoteFilePath(@Nonnull final String fileName) {
        return httpHostWebDAVBaseUrl + fileName;
    }

    @Nonnull
    private static String remoteFilePathWithoutHostName(@Nonnull final String dirName, @Nonnull final String fileName) {
        return dirName + File.separator + fileName;
    }

    private boolean executeMethod(@Nonnull HttpRequestBase httpRequest) {
        boolean retry = true;
        int count = 0;
        int responseStatusCode = -1;
        while (retry && count < DIAGNOSTIC_PUSH_SOLUTION_RETRIES) {
            try {
                CloseableHttpResponse response = httpClient.execute(httpRequest);
                responseStatusCode = response.getStatusLine().getStatusCode();
                response.close();
                httpRequest.completed();
                retry = false;
                return successfulResponse(responseStatusCode);
            } catch (Exception e) {
                logger.debug("[ Try: " + count + " ] - " + (retry ? "failed" : "succeed") +
                        " HTTP Request: " + httpRequest +
                        ", received status code: " + responseStatusCode +
                        "Failure Reason: " + e);
                retry = true;
            } finally {
                logger.debug("[ Try: " + count + " ] - " + (retry ? "failed" : "succeed") +
                        " HTTP Request: " + httpRequest +
                        ", received status code: " + responseStatusCode);
                count++;
            }
        }
        return !retry;
    }

    private boolean exists(@Nonnull final String dirName) {
        return executeMethod(new HttpHead(remoteFilePath(dirName)));
    }

    /**
     * HTTP MKCOL - Method implementation.
     */
    private boolean mkdir(@Nonnull final String dirName) throws URISyntaxException {
        if (!exists(dirName)) {
            return executeMethod(new HttpMkCol(remoteFilePath(dirName)));
        } else {
            logger.debug(remoteFilePath(dirName) + " exists.");
        }
        return true;
    }

    /**
     * HTTP DELETE - Method implementation.
     * can remove file or directory on the server, for directory make sure that it ends with File.seperator
     */
    private boolean rm(@Nonnull final String dirName) throws URISyntaxException {
        if (exists(dirName)) {
            return executeMethod(new HttpDelete(remoteFilePath(dirName)));
        } else {
            logger.debug(remoteFilePath(dirName) + " doesn't exist.");
        }
        return true;
    }

    /**
     * HTTP PUT - Method implementation.
     */
    private boolean put(@Nonnull final File localFile, @Nonnull final String targetDirName, @Nonnull final String targetFileName) throws IOException, URISyntaxException {
        if (!localFile.exists()) {
            logger.info("Local File: " + localFile.getAbsolutePath() + " doesn't exist, can't proceed upload to " + httpHostWebDAVBaseUrl);
            return false;
        }

        if (!mkdirRecursive(targetDirName)) {
            logger.info("Failed creating directory: " + targetDirName + " on " + httpHostWebDAVBaseUrl);
            return false;
        }

        final String targetHttpHostFilePath = remoteFilePath(targetDirName, targetFileName);
        logger.info("Received an HTTP Put request for Local File: " + localFile.getAbsolutePath() + ", to Target Path: " + targetHttpHostFilePath);

        if (!exists(remoteFilePathWithoutHostName(targetDirName, targetFileName))) {
            // add the 100 continue directive
            final HttpPut httpPut = new HttpPut(targetHttpHostFilePath);
            httpPut.addHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE);
            FileEntity fileEntity = new FileEntity(localFile, ContentType.APPLICATION_OCTET_STREAM);
            httpPut.setEntity(fileEntity);

            return executeMethod(httpPut);
        } else {
            logger.info("Remote file: " + targetHttpHostFilePath + " exists");
            return true;
        }
    }

    /**
     * HTTP MOVE - Method implementation.
     */
    private boolean mv(@Nonnull String sourceDirName, @Nonnull String sourceFileName, @Nonnull String targetDirName, @Nonnull String targetFileName) throws IOException, URISyntaxException {
        final String sourceRemotePathWithoutHostName = remoteFilePathWithoutHostName(sourceDirName, sourceFileName);
        final String targetRemotePathWithoutHostName = remoteFilePathWithoutHostName(targetDirName, targetFileName);

        final String sourceHttpHostFilePath = remoteFilePath(sourceRemotePathWithoutHostName);
        final String targetHttpHostFilePath = remoteFilePath(targetRemotePathWithoutHostName);

        if (!exists(sourceRemotePathWithoutHostName)) {
            logger.info("Source File: " + sourceRemotePathWithoutHostName + " doesn't exist, can't proceed move to " + targetRemotePathWithoutHostName);
            return false;
        }

        if (!mkdirRecursive(targetDirName)) {
            logger.info("Failed creating directory: " + targetDirName + " on " + httpHostWebDAVBaseUrl);
            return false;
        }

        if (!exists(remoteFilePathWithoutHostName(targetDirName, targetFileName))) {
            return executeMethod(new HttpMove(sourceHttpHostFilePath, targetHttpHostFilePath));
        } else {
            logger.info("Target file: " + targetHttpHostFilePath + " exists");
            return true;
        }
    }

    private boolean mkdirRecursive(@Nonnull final String dirName) throws IOException, URISyntaxException {
        final String[] dirs = dirName.split(File.separator);
        StringBuilder dirToCreate = new StringBuilder();
        for (String dir : dirs) {
            dirToCreate.append(dir).append(File.separator);
            logger.info("Creating directory: " + remoteFilePath(dirToCreate.toString()));
            if (!mkdir(dirToCreate.toString())) {
                logger.info("Failed creating directory: " + remoteFilePath(dirToCreate.toString()));
                return false;
            }
        }
        return true;
    }

    @Nullable
    private String urlEncode(@Nullable final String url) throws UnsupportedEncodingException {
        if (url == null) {
            return null;
        }

        final StringBuilder urlEncodedPayload = new StringBuilder();
        for (String element : url.split(File.separator)) {
            if (!StringUtils.isEmpty(element)) {
                String urlEncodedElement = URLEncoder.encode(element, "UTF-8");
                if (StringUtils.isEmpty(urlEncodedPayload)) {
                    urlEncodedPayload.append(urlEncodedElement);
                } else {
                    urlEncodedPayload.append(File.separator + urlEncodedElement);
                }
            }
        }

        return urlEncodedPayload.toString();
    }



    public boolean delete(@Nonnull final String dirName) throws IOException, URISyntaxException {
        // HTTP delete request for Directory should have trailing path separator.
        final String urlEncodedDirName = urlEncode(dirName) + File.separator;
        String remoteDirPath = remoteFilePath(urlEncodedDirName);
        logger.info("Cleaning directory: " + remoteDirPath);
        if (!rm(urlEncodedDirName)) {
            logger.info("Failed delete directory: " + remoteDirPath);
            return false;
        }
        return true;
    }

    @Nonnull
    public String remoteFilePath(@Nonnull final String dirName, @Nonnull final String fileName) {
        return httpHostWebDAVBaseUrl + remoteFilePathWithoutHostName(dirName, fileName);
    }

    public boolean upload(@Nonnull final String localFilePath, @Nonnull final String targetDirName, @Nonnull final String targetFileName) throws IOException, URISyntaxException {
        return put(new File(localFilePath), urlEncode(targetDirName), urlEncode(targetFileName));
    }

    public boolean upload(@Nonnull final File localFile, @Nonnull final String targetDirName, @Nonnull final String targetFileName) throws IOException, URISyntaxException {
        return put(localFile, urlEncode(targetDirName), urlEncode(targetFileName));
    }

    public boolean move(@Nonnull String sourceDirName, @Nonnull String sourceFileName, @Nonnull String targetDirName, @Nonnull String targetFileName) throws IOException, URISyntaxException {
        return mv(urlEncode(sourceDirName), urlEncode(sourceFileName), urlEncode(targetDirName), urlEncode(targetFileName));
    }

    public void close() throws IOException {
        httpClient.close();
    }
}

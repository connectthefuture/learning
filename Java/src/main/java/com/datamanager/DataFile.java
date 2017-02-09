package com.datamanager;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.StringJoiner;

/**
 * @author : pgajjar
 * @since  : 2/5/17
 */
public class DataFile implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Tika tikaHandle = new Tika();

    private final File file;
    private @NonNull final HashCode md5;
    private @NonNull final String fileType;

    public DataFile(@NonNull final File file) throws IOException {
        this.file = file;
        this.md5 = Files.hash(file, Hashing.md5());
        this.fileType = tikaHandle.detect(file);
    }

    public DataFile(@NonNull final DataFile clone) {
        this.file = clone.file;
        this.md5 = clone.md5;
        this.fileType = clone.fileType;
    }

    public DataFile(@NonNull final String filePath) throws IOException {
        this(new File(filePath));
    }

    private boolean md5Matches(@NonNull final DataFile dataFile) {
        return StringUtils.equals(this.md5.toString(), dataFile.md5.toString());
    }

    @NonNull
    public String fileName() {
        return file.getName();
    }

    @NonNull
    public String md5() {
        return md5.toString();
    }

    @NonNull
    public File fileHandle() {
        return file;
    }

    /**
     * Inclusive range
     */
    public boolean isFileSizeInRange(int start, int end) {
        return file.length() >= start && file.length() <= end;
    }

    @NonNull
    public String filePath() {
        return file.getAbsolutePath();
    }

    public boolean isIdentical(@NonNull final DataFile otherFile) {
        return this.md5Matches(otherFile);
    }

    public boolean fileNameMatches(@NonNull final DataFile otherFile) {
        return this.fileName().equalsIgnoreCase(otherFile.fileName());
    }

    public boolean fileTypeMatches(@NonNull final DataFile otherFile) {
        return StringUtils.equals(this.fileType, otherFile.fileType);
    }

    @NonNull
    public String fileType() {
        return fileType;
    }

    @Override
    public String toString() {
        StringJoiner sb = new StringJoiner("");
        sb.add(md5() + ", ");
        sb.add("\"" + file.getAbsolutePath() + "\", ");
        sb.add(fileType + ", ");
        sb.add(Long.toString(file.length()) + " bytes");

        return sb.toString();
    }

    @NonNull
    public static ComparisionResult compare(@NonNull final DataFile file, @NonNull final DataFile otherFile) {
        return new ComparisionResult(file.isIdentical(otherFile), file.fileNameMatches(otherFile), file.fileTypeMatches(otherFile));
    }

    @NonNull
    public static String fileDeleteCmd() {
        return (FileStore.isWindows()) ? "del" : "rm";
    }

    public static void main(String[] args) throws IOException {
        @NonNull ComparisionResult compareResult = DataFile.compare(new DataFile("/Users/pgajjar/jingle/AssetPool/VideoMpeg2FifteenSeconds.a43a9957516e82e276db14760c2b7187.mpg"),
                new DataFile("/tmp/test.mov"));

        System.out.println("Both files " + (compareResult.md5Matches() ? "are identical" : "differs"));
        System.out.println("Both files name " + (compareResult.fileNameMatches() ? "is identical" : "differs"));
        System.out.println("Both files type " + (compareResult.fileTypeMatches() ? "is identical" : "differs"));
    }
}

package com.datamanager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : pgajjar
 * @since  : 2/6/17
 */
public class FileStore<T extends DataFile> {
    // need to see if this can be serialized, but first think - does it have to be???
    private static final Logger logger = Logger.getLogger(FileStore.class.getName());

    private static @NonNull FileStore handle;
    private static @NonNull FileStore duplicateHandle;

    private static final int fileSizeForDeleteStart = 0;
    private static final int fileSizeForDeleteEnd = 0;

    private static final String OPERATING_SYSTEM = System.getProperty("os.name");

    private final @NonNull Multimap<String, T> fileStore = HashMultimap.create();
    private final @NonNull Multimap<String, String> fileTypeStore = HashMultimap.create();

    private final boolean deleteZeroSizeFiles;
    private boolean isDuplicateStore = false;

    private FileStore(boolean deleteZeroSizeFiles) {
        this.deleteZeroSizeFiles = deleteZeroSizeFiles;
    }

    @NonNull
    public synchronized static FileStore getHandle() {
        return getHandle(false);
    }

    @NonNull
    public synchronized static FileStore getHandle(boolean deleteZeroSizeFiles) {
        return (handle == null) ? (handle = new FileStore(deleteZeroSizeFiles)) : handle;
    }

    @Nullable
    public synchronized static FileStore getDuplicateFileHandle() throws IllegalAccessException {
        if (handle != null) {
            if (duplicateHandle == null) {
                duplicateHandle = new FileStore(false);
                Set<? extends String> keys = handle.fileStore.keySet();
                for (String key : keys) {
                    Collection values = handle.fileStore.get(key);
                    if (values.size() > 1) {
                        // definite duplicates.
                        duplicateHandle.put(key, values);
                    } else {
                        // TODO: other ways of duplication goes here.
                    }
                }

                // from now on duplicate file store can't take new values so setting the flag to block new add
                duplicateHandle.isDuplicateStore = true;
            }
        }
        return duplicateHandle;
    }

    private void deleteFile(@NonNull final T file) {
        file.fileHandle().deleteOnExit();
    }

    /**
     * Keep this as a single point of entry for all the put requests.
     */
    public boolean put(@NonNull final String key, @NonNull final T file) throws IllegalAccessException {
        if (isDuplicateStore) {
            throw new IllegalAccessException("Can't add element to Duplicate File Store.");
        }
        if (deleteZeroSizeFiles && file.isFileSizeInRange(fileSizeForDeleteStart, fileSizeForDeleteEnd) ) {
            deleteFile(file);
            logger.info("Delete request initiated for File size (" + fileSizeForDeleteStart + "-" + fileSizeForDeleteEnd + "): " + file.filePath());
            return false;
        } else {
            fileTypeStore.put(file.fileType(), key);
            return fileStore.put(key, file);
        }
    }

    public void put(@NonNull final String key, @NonNull final Collection<T> files) throws IllegalAccessException {
        for (T value : files) {
            put(key, value);
        }
    }

    public void putAll(Multimap<? extends String, ? extends T> map) throws IllegalAccessException {
        for (Map.Entry<? extends String, ? extends T> entry : map.entries()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean put(@NonNull final T file) throws IllegalAccessException {
        return put(file.md5(), file);
    }

    @NonNull
    public Collection<T> get(@NonNull final String key) {
        return fileStore.get(key);
    }

    @NonNull
    public Collection<T> get(@NonNull final T data) {
        if (data instanceof DataFile) {
            return fileStore.get(((DataFile) data).md5());
        }
        throw new IllegalArgumentException("Only DataFile object is supported for now here.");
    }

    public boolean keyPresent(@NonNull final String key) {
        return fileStore.containsKey(key);
    }

    public boolean isExists(@NonNull final T data) {
        if (data instanceof DataFile) {
            return fileStore.containsKey(((DataFile) data).md5());
        }
        throw new IllegalArgumentException("Only DataFile object is supported for now here.");
    }

    public int getDuplicates(@NonNull final String key) {
        return fileStore.get(key).size();
    }

    public int getDuplicates(@NonNull final T data) {
        if (data instanceof DataFile) {
            return fileStore.get(((DataFile) data).md5()).size();
        }
        throw new IllegalArgumentException("Only DataFile object is supported for now here.");
    }

    public int fileCount() {
        return fileStore.size();
    }

    public int distinctFileCount() {
        return keySet().size();
    }

    public int duplicateFileCount() {
        return fileCount() - distinctFileCount();
    }

    @NonNull
    public Collection<String> keySet() {
        return fileStore.keySet();
    }

    @NonNull
    public Collection<String> fileTypes() {
        return fileTypeStore.keySet();
    }

    @NonNull
    public Collection<T> filesOfType(@NonNull final String key) {
        return fileTypeStore.get(key).stream().map(k -> fileStore.get(k)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static boolean isWindows() {
        return OPERATING_SYSTEM.startsWith("window");
    }
}

package com.datamanager.actions;

import com.datamanager.DataFile;
import com.datamanager.FileStore;
import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * @author : pgajjar
 * @since  : 2/8/17
 */
public class DataManagerScriptAction<T extends DataFile> implements DataManagerAction {
    private static final Logger logger = Logger.getLogger(DataManagerScriptAction.class.getName());
    private final PrintWriter writer;
    private final FileStore<T> fileStore;

    DataManagerScriptAction(@NonNull final FileStore fileStore, @NonNull final String reportFile) throws FileNotFoundException {
        this.fileStore = fileStore;
        this.writer = new PrintWriter(new File(reportFile));
    }

    @Override
    public void action() {
        Collection<String> fileTypes = fileStore.fileTypes();
        for (String fileType : fileTypes) {
            writer.println("# " + fileType);
            String lastMd5 = null;
            for (T dupFile : fileStore.filesOfType(fileType)) {
                if (!dupFile.md5().equals(lastMd5)) {
                    writer.println();
                    lastMd5 = dupFile.md5();
                }
                writer.println("\t" + dupFile.md5() + "\t" + DataFile.fileDeleteCmd() +  " \"" + dupFile.filePath() + "\"");
            }
            writer.println();
        }
        logger.info("Found " + fileStore.duplicateFileCount() + " duplicate files of type(s): " + fileTypes);
    }

    @Override
    public void stop() {
        writer.close();
    }
}

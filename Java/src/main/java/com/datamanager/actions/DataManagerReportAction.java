package com.datamanager.actions;

import com.datamanager.DataFile;
import com.datamanager.FileStore;
import org.apache.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * @author : pgajjar
 * @since  : 2/6/17
 */
public class DataManagerReportAction<T extends DataFile> implements DataManagerAction {
    private static final Logger logger = Logger.getLogger(DataManagerReportAction.class.getName());
    private final PrintWriter writer;
    private final FileStore<T> fileStore;

    DataManagerReportAction(@NonNull final FileStore<T> fileStore, @NonNull final String reportFile) throws FileNotFoundException {
        this.fileStore = fileStore;
        this.writer = new PrintWriter(new File(reportFile));
    }

    @Override
    public void action() {
        for (String md5 : fileStore.keySet()) {
            // writer.println(md5);
            for (T dupFile : fileStore.get(md5)) {
                writer.println(dupFile);
            }
            writer.println();
        }
        logger.info("Found " + fileStore.duplicateFileCount() + " duplicate files.");
    }

    @Override
    public void stop() {
        writer.close();
    }
}

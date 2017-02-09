package com.datamanager.actions;

import com.datamanager.FileStore;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.FileNotFoundException;

/**
 * @author : pgajjar
 * @since  : 2/6/17
 */
public final class DataManagerActionFactory {
    public enum Type {
        WriteReport,
        WriteGroupedReport,
        WriteScript,
    }

    private DataManagerActionFactory() {}

    @Nullable
    public static DataManagerAction getInstance(@NonNull final  Type dataActionType, @NonNull final FileStore fileStore, @NonNull final String reportFile) throws FileNotFoundException {
        switch (dataActionType) {
            case WriteReport:
                return new DataManagerReportAction(fileStore, reportFile);
            case WriteGroupedReport:
                return new DataManagerGroupedReportAction(fileStore, reportFile);
            case WriteScript:
                return new DataManagerScriptAction(fileStore, reportFile);
            default:
                return null;
        }
    }
}

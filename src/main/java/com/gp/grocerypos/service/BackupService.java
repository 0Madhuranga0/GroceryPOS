package com.gp.grocerypos.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic contract for database backup and restore.
 * <p>
 * Uses mysqldump for backup and mysql for restore — the paths to these
 * executables are configurable in the settings table.
 * <p>
 * Only ADMIN users with BACKUP_MANAGE permission may call these operations.
 */
public interface BackupService {

    /**
     * Creates a full database backup using mysqldump.
     * The backup file is written to the configured backup directory with a
     * timestamped filename:  grocery_pos_backup_yyyy-MM-dd_HH-mm-ss.sql
     *
     * @return the backup file that was created
     * @throws com.gp.grocerypos.exception.BackupException if the backup fails
     * @throws com.gp.grocerypos.exception.AuthException   if caller lacks BACKUP_MANAGE
     */
    File createBackup();

    /**
     * Creates a backup to an explicit target file.
     *
     * @param targetFile the destination .sql file
     * @return the backup file written
     */
    File createBackup(File targetFile);

    /**
     * Restores the database from a backup file.
     * <p>
     * <b>WARNING:</b> This drops and recreates all data in the database.
     * The caller must confirm this action in the UI before invoking.
     *
     * @param backupFile the .sql backup file to restore from
     * @throws com.gp.grocerypos.exception.BackupException if the restore fails
     */
    void restoreBackup(File backupFile);

    /**
     * Returns a list of existing backup files in the configured backup directory,
     * newest first.
     */
    List<File> listBackups();

    /**
     * Returns the currently configured backup directory path.
     */
    String getBackupDirectory();
}

package com.gp.grocerypos.service;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.config.DatabaseConfig;
import com.gp.grocerypos.dao.SettingDAO;
import com.gp.grocerypos.dao.SettingDAOImpl;
import com.gp.grocerypos.exception.BackupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BackupServiceImpl implements BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);
    private static final DateTimeFormatter FILE_TS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final SettingDAO settingDAO;

    public BackupServiceImpl() {
        this.settingDAO = new SettingDAOImpl();
    }

    // ── createBackup ──────────────────────────────────────────────────────────

    @Override
    public File createBackup() {
        SessionContext.getInstance().requirePermission("BACKUP_MANAGE");
        String dir = getBackupDirectory();
        File backupDir = new File(dir);
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                throw new BackupException(
                    "Could not create backup directory: " + dir,
                    "Backup directory '" + dir + "' could not be created.");
            }
        }
        String filename = "grocery_pos_backup_"
            + LocalDateTime.now().format(FILE_TS) + ".sql";
        File target = new File(backupDir, filename);
        return createBackup(target);
    }

    @Override
    public File createBackup(File targetFile) {
        SessionContext.getInstance().requirePermission("BACKUP_MANAGE");

        DatabaseConfig db = AppConfig.getInstance().getDatabaseConfig();
        String mysqldump  = settingDAO.getValue("mysqldump_path", "mysqldump");

        // Build the mysqldump command
        // Password is passed via environment variable to avoid it appearing in
        // process listings — MYSQL_PWD is respected by mysqldump
        List<String> cmd = new ArrayList<>(List.of(
            mysqldump,
            "--host=" + db.getHost(),
            "--port=" + db.getPort(),
            "--user=" + db.getUsername(),
            "--single-transaction",
            "--routines",
            "--triggers",
            "--add-drop-table",
            "--result-file=" + targetFile.getAbsolutePath(),
            db.getDbName()
        ));

        log.info("Starting backup to: {}", targetFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("MYSQL_PWD", db.getPassword());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode  = process.waitFor();

            if (exitCode != 0) {
                log.error("mysqldump failed (exit {}): {}", exitCode, output);
                throw new BackupException(
                    "mysqldump exited with code " + exitCode + ": " + output,
                    "Backup failed. Check that mysqldump is installed and accessible.");
            }

            log.info("Backup created successfully: {} ({} bytes)",
                targetFile.getName(), targetFile.length());
            return targetFile;

        } catch (BackupException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Backup process error: {}", e.getMessage(), e);
            throw new BackupException(
                "Backup process failed: " + e.getMessage(),
                "Could not run mysqldump. Check that it is installed and the path is correct.",
                e);
        }
    }

    // ── restoreBackup ─────────────────────────────────────────────────────────

    @Override
    public void restoreBackup(File backupFile) {
        SessionContext.getInstance().requirePermission("BACKUP_MANAGE");

        if (!backupFile.exists() || !backupFile.isFile()) {
            throw new BackupException(
                "Backup file not found: " + backupFile.getAbsolutePath(),
                "The selected backup file does not exist.");
        }

        DatabaseConfig db = AppConfig.getInstance().getDatabaseConfig();

        // Derive mysql executable from mysqldump path
        String mysqldump = settingDAO.getValue("mysqldump_path", "mysqldump");
        String mysql = mysqldump.endsWith("mysqldump")
            ? mysqldump.substring(0, mysqldump.length() - "mysqldump".length()) + "mysql"
            : "mysql";

        List<String> cmd = List.of(
            mysql,
            "--host=" + db.getHost(),
            "--port=" + db.getPort(),
            "--user=" + db.getUsername(),
            db.getDbName()
        );

        log.warn("RESTORE starting from: {}", backupFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("MYSQL_PWD", db.getPassword());
        pb.redirectInput(backupFile);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode  = process.waitFor();

            if (exitCode != 0) {
                log.error("mysql restore failed (exit {}): {}", exitCode, output);
                throw new BackupException(
                    "mysql restore exited with code " + exitCode + ": " + output,
                    "Restore failed. The database may be in an inconsistent state.");
            }

            log.info("Database restored from: {}", backupFile.getName());

        } catch (BackupException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Restore process error: {}", e.getMessage(), e);
            throw new BackupException(
                "Restore process failed: " + e.getMessage(),
                "Could not run the database restore. Check logs for details.", e);
        }
    }

    // ── listBackups ───────────────────────────────────────────────────────────

    @Override
    public List<File> listBackups() {
        File dir = new File(getBackupDirectory());
        if (!dir.exists() || !dir.isDirectory()) return List.of();

        File[] files = dir.listFiles((d, name) ->
            name.startsWith("grocery_pos_backup_") && name.endsWith(".sql"));

        if (files == null) return List.of();

        return Arrays.stream(files)
            .sorted(Comparator.comparingLong(File::lastModified).reversed())
            .toList();
    }

    // ── getBackupDirectory ────────────────────────────────────────────────────

    @Override
    public String getBackupDirectory() {
        return settingDAO.getValue("backup_dir",
            AppConfig.getInstance().getBackupDir());
    }
}

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.downloader;

import com.apollocurrency.aplwallet.apl.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdaterConstants;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;

public class Downloader {
    private UpdaterMediator mediator = UpdaterMediator.getInstance();
    private DownloadExecutor defaultDownloadExecutor = new DefaultDownloadExecutor(TEMP_DIR_PREFIX, DOWNLOADED_FILE_NAME);

    private Downloader() {}

    public static Downloader getInstance() {
        return DownloaderHolder.INSTANCE;
    }

    private boolean checkConsistency(Path file, byte hash[]) {
        try {
            byte[] actualHash = calclulateHash(file);
            if (Arrays.equals(hash, actualHash)) {
                return true;
            }
        }
        catch (Exception e) {
            Logger.logErrorMessage("Cannot calculate checksum for file: " + file, e);
        }
        return false;
    }

    private byte[] calclulateHash(Path file) throws IOException, NoSuchAlgorithmException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return digest.digest();
        }
    }

    /**
     * Download file from uri and return Path to downloaded file
     * @param uri
     * @param hash
     * @param downloadExecutor - configurable download executor
     * @return
     */
    public Path tryDownload(String uri, byte[] hash, DownloadExecutor downloadExecutor) {
        int attemptsCounter = 0;
        mediator.setStatus(UpdateInfo.DownloadStatus.STARTED);
        while (attemptsCounter != UpdaterConstants.DOWNLOAD_ATTEMPTS) {
            try {
                attemptsCounter++;
                mediator.setState(UpdateInfo.DownloadState.IN_PROGRESS);
                Path downloadedFile = downloadExecutor.download(uri);
                if (checkConsistency(downloadedFile, hash)) {
                    mediator.setStatus(UpdateInfo.DownloadStatus.OK);
                    mediator.setState(UpdateInfo.DownloadState.FINISHED);
                    return downloadedFile;
                } else {
                    mediator.setStatus(UpdateInfo.DownloadStatus.INCONSISTENT);
                    Logger.logErrorMessage("Inconsistent file, downloaded from: " + uri);
                }
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                TimeUnit.SECONDS.sleep(NEXT_ATTEMPT_TIMEOUT);
            }
            catch (IOException e) {
                Logger.logErrorMessage("Unable to download update from: " + uri, e);
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                mediator.setStatus(UpdateInfo.DownloadStatus.CONNECTION_FAILURE);
            }
            catch (InterruptedException e) {
                Logger.logInfoMessage("Downloader was awakened", e);
            }
        }
        mediator.setState(UpdateInfo.DownloadState.FINISHED);
        mediator.setStatus(UpdateInfo.DownloadStatus.FAIL);
        return null;
    }

    /**
     * Download file from url and return Path to downloaded file
     * Uses default DownloadExecutor
     * @param uri
     * @param hash
     * @return
     */
    public Path tryDownload(String uri, byte[] hash) {
        return tryDownload(uri, hash, defaultDownloadExecutor);
    }

    private static class DownloaderHolder {
        private static final Downloader INSTANCE = new Downloader();
    }

}

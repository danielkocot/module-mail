package com.reedelk.mail.internal.imap;

import com.sun.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import java.io.Closeable;


public class IMAPIdlListenerThread extends Thread implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(IMAPIdlListenerThread.class);

    private static final int ON_ERROR_SLEEP_TIME = 3000;

    private volatile boolean running = true;

    private final Folder folder;

    private final String username;
    private final String password;
    private final int folderOpenMode;

    public IMAPIdlListenerThread(String username, String password, Folder folder, int folderOpenMode) {
        this.folder = folder;
        this.username = username;
        this.password = password;
        this.folderOpenMode = folderOpenMode;
    }

    @Override
    public void run() {
        while (running) {
            try {
                ensureFolderOpen();
                ((IMAPFolder) folder).idle();
            } catch (Exception exception) {
                // something went wrong wait and try again
                logger.warn(exception.getMessage());// TODO

                exception.printStackTrace(); // TODO: remove this... (should log with debug)
                try {
                    Thread.sleep(ON_ERROR_SLEEP_TIME);
                } catch (InterruptedException interrupted) {
                    // ignore
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        if (running) {
            running = false;
        }
    }

    public void ensureFolderOpen() throws MessagingException {
        Store store = folder.getStore();
        if (store != null && !store.isConnected()) {
            store.connect(username, password);
        }
        if (folder.exists() && !folder.isOpen() && (folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            folder.open(folderOpenMode);
            if (!folder.isOpen()) {
                throw new MessagingException("Unable to open folder " + folder.getFullName());
            }
        }
    }
}
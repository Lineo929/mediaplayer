package com.example.biim3210;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;

/**
 * Manages the list of media files in the playlist.
 */
public class Playlist {

    private final ObservableList<File> files;
    private int currentIndex;

    public Playlist() {
        this.files = FXCollections.observableArrayList();
        this.currentIndex = -1;
    }

    /** Add a media file to the playlist. */
    public void addFile(File file) {
        if (file != null && !files.contains(file)) {
            files.add(file);
            if (currentIndex == -1) {
                currentIndex = 0;
            }
        }
    }

    /** Remove a media file from the playlist. */
    public void removeFile(File file) {
        if (file == null) return;

        int removedIndex = files.indexOf(file);
        files.remove(file);

        if (files.isEmpty()) {
            currentIndex = -1;
        } else if (removedIndex <= currentIndex) {
            if (currentIndex > 0) {
                currentIndex--;
            }
            if (currentIndex >= files.size()) {
                currentIndex = files.size() - 1;
            }
        }
    }

    /** Get the observable list for binding to a ListView. */
    public ObservableList<File> getFiles() {
        return files;
    }

    /** Get the currently selected file, or null if none. */
    public File getCurrentFile() {
        if (currentIndex >= 0 && currentIndex < files.size()) {
            return files.get(currentIndex);
        }
        return null;
    }

    /** Set the current index directly (used by ListView selection). */
    public void setCurrentIndex(int index) {
        if (index >= 0 && index < files.size()) {
            this.currentIndex = index;
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    /** Advance to the next track. Returns the new current file or null. */
    public File next() {
        if (files.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % files.size();
        return getCurrentFile();
    }

    /** Go back to the previous track. Returns the new current file or null. */
    public File previous() {
        if (files.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + files.size()) % files.size();
        return getCurrentFile();
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }
}
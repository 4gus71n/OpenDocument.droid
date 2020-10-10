package at.tomtasche.reader.ui;

import android.net.Uri;

public interface DocumentCallback {
    void loadUri(Uri cacheFileUri, boolean isPersisited);
}

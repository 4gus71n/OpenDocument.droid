package at.tomtasche.reader.background;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import java.io.File;

public class OoxmlLoader extends FileLoader {

    private CoreWrapper lastCore;
    private CoreWrapper.CoreOptions lastCoreOptions;

    public OoxmlLoader(Context context) {
        super(context, LoaderType.OOXML);
    }

    @Override
    public boolean isSupported(Options options) {
        // TODO: enable xlsx and pptx too
        return options.fileType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") /*|| options.fileType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || options.fileType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.presentation")*/;
    }

    @Override
    public void loadSync(Options options) {
        final Result result = new Result();
        result.options = options;
        result.loaderType = type;

        try {
            File cachedFile = AndroidFileCache.getCacheFile(context);

            if (lastCore != null) {
                lastCore.close();
            }

            CoreWrapper core = new CoreWrapper();
            try {
                core.initialize();

                lastCore = core;
            } catch (Throwable e) {
                e.printStackTrace();
            }

            File cacheDirectory = AndroidFileCache.getCacheDirectory(context);

            File fakeHtmlFile = new File(cacheDirectory, "ooxml");

            CoreWrapper.CoreOptions coreOptions = new CoreWrapper.CoreOptions();
            coreOptions.inputPath = cachedFile.getPath();
            coreOptions.outputPath = fakeHtmlFile.getPath();
            coreOptions.password = options.password;
            coreOptions.editable = options.translatable;
            coreOptions.ooxml = true;

            lastCoreOptions = coreOptions;

            CoreWrapper.CoreResult coreResult = lastCore.parse(coreOptions);
            if (coreResult.exception != null) {
                throw coreResult.exception;
            }

            // fileType could potentially change after decrypting DOCX successfully for the first time
            //  (not reported as DOCX prior)
            options.fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(coreResult.extension);

            for (int i = 0; i < coreResult.pageNames.size(); i++) {
                File entryFile = new File(fakeHtmlFile.getPath() + i + ".html");

                result.partTitles.add(coreResult.pageNames.get(i));
                result.partUris.add(Uri.fromFile(entryFile));
            }

            callOnSuccess(result);
        } catch (Throwable e) {
            if (e instanceof CoreWrapper.CoreEncryptedException) {
                e = new EncryptedDocumentException();
            }

            callOnError(result, e);
        }
    }

    @Override
    public File retranslate(String htmlDiff) {
        File cacheDirectory = AndroidFileCache.getCacheDirectory(context);
        File tempFilePrefix = new File(cacheDirectory, "retranslate");

        lastCoreOptions.outputPath = tempFilePrefix.getPath();

        CoreWrapper.CoreResult result = lastCore.backtranslate(lastCoreOptions, htmlDiff);
        if (result.errorCode != 0) {
            return null;
        }

        return new File(result.outputPath);
    }

    @Override
    public void close() {
        super.close();

        if (lastCore != null) {
            lastCore.close();
            lastCore = null;
        }
    }
}

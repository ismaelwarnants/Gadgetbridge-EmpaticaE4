/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Felix Konstantin Maurer, José
    Rebelo, Marc Nause, Petr Vaněk, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.color.DynamicColors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class AndroidUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidUtils.class);

    /**
     * Creates a new {@link ParcelUuid} array with the contents of the given uuids.
     * The given array is expected to contain only {@link ParcelUuid} elements.
     * @param uuids an array of {@link ParcelUuid} elements
     * @return a {@link ParcelUuid} array instance with the same contents
     */
    @Nullable
    public static ParcelUuid[] toParcelUuids(Parcelable[] uuids) {
        if (uuids == null) {
            return null;
        }
        ParcelUuid[] uuids2 = new ParcelUuid[uuids.length];
        System.arraycopy(uuids, 0, uuids2, 0, uuids.length);
        return uuids2;
    }

    /**
     * Unregisters the given receiver from the given context.
     * @param context the context from which to unregister
     * @param receiver the receiver to unregister
     * @return true if it was successfully unregistered, or false if the receiver was not registered
     */
    public static boolean safeUnregisterBroadcastReceiver(Context context, BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Unregisters the given receiver from the given {@link LocalBroadcastManager}.
     * @param manager the manager  from which to unregister
     * @param receiver the receiver to unregister
     * @return true if it was successfully unregistered, or false if the receiver was not registered
     */
    public static boolean safeUnregisterBroadcastReceiver(LocalBroadcastManager manager, BroadcastReceiver receiver) {
        try {
            manager.unregisterReceiver(receiver);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static void setLanguage(Context context, Locale language) {
        Configuration config = new Configuration();
        config.setLocale(language);

        // FIXME: I have no idea what I am doing
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

        Locale.setDefault(language);
    }

    /**
     * Returns the theme dependent text color as a css-style hex string.
     * @param context the context to access the colour
     */
    public static String getTextColorHex(Context context) {
        int color;
        if (DynamicColors.isDynamicColorAvailable() && GBApplication.areDynamicColorsEnabled()) {
            Context dynamicColorContext;
            if (GBApplication.isDarkThemeEnabled()) {
                dynamicColorContext = DynamicColors.wrapContextIfAvailable(context, R.style.GadgetbridgeThemeDynamicDark);
            } else {
                dynamicColorContext = DynamicColors.wrapContextIfAvailable(context, R.style.GadgetbridgeThemeDynamicLight);
            }
            int[] attrsToResolve = {com.google.android.material.R.attr.colorOnSurface};
            @SuppressLint("ResourceType")
            TypedArray ta = dynamicColorContext.obtainStyledAttributes(attrsToResolve);
            color = ta.getColor(0, 0);
            ta.recycle();
        } else if (GBApplication.isDarkThemeEnabled()) {
            color = ContextCompat.getColor(context, R.color.primarytext_dark);
        } else {
            color = ContextCompat.getColor(context, R.color.primarytext_light);
        }
        return colorToHex(color);
    }

    /**
     * Returns the theme dependent background color as a css-style hex string.
     * @param context the context to access the colour
     */
    public static String getBackgroundColorHex(Context context) {
        int color;
        if (DynamicColors.isDynamicColorAvailable() && GBApplication.areDynamicColorsEnabled()) {
            Context dynamicColorContext;
            if (GBApplication.isDarkThemeEnabled()) {
                dynamicColorContext = DynamicColors.wrapContextIfAvailable(context, R.style.GadgetbridgeThemeDynamicDark);
            } else {
                dynamicColorContext = DynamicColors.wrapContextIfAvailable(context, R.style.GadgetbridgeThemeDynamicLight);
            }
            int[] attrsToResolve = {com.google.android.material.R.attr.colorSurface};
            @SuppressLint("ResourceType")
            TypedArray ta = dynamicColorContext.obtainStyledAttributes(attrsToResolve);
            color = ta.getColor(0, 0);
            ta.recycle();
        } else if (GBApplication.isDarkThemeEnabled()) {
            color = ContextCompat.getColor(context, androidx.cardview.R.color.cardview_dark_background);
        } else {
            color = ContextCompat.getColor(context, androidx.cardview.R.color.cardview_light_background);
        }
        return colorToHex(color);
    }

    public static int getBackgroundColor(Context context) {
        int color;
        if (GBApplication.isDarkThemeEnabled()) {
            color = ContextCompat.getColor(context, androidx.cardview.R.color.cardview_dark_background);
        } else {
            color = ContextCompat.getColor(context, androidx.cardview.R.color.cardview_light_background);
        }
        return color;
    }


    private static String colorToHex(int color) {
        return "#"
                + Integer.toHexString(Color.red(color))
                + Integer.toHexString(Color.green(color))
                + Integer.toHexString(Color.blue(color));
    }

    /**
     * As seen on StackOverflow https://stackoverflow.com/a/36714242/1207186
     * Try to find the file path of a document uri
     *
     * @param context the application context
     * @param uri     the Uri for which the path should be resolved
     * @return the path corresponding to the Uri as a String
     * @throws IllegalArgumentException on any problem decoding the uri to a path
     */
    public static @NonNull String getFilePath(@NonNull Context context, @NonNull Uri uri) throws IllegalArgumentException {
        try {
            String path = internalGetFilePath(context, uri);
            if (TextUtils.isEmpty(path)) {
                throw new IllegalArgumentException("Unable to decode the given uri to a file path: " + uri);
            }
            return path;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to decode the given uri to a file path: " + uri, ex);
        }
    }

    /**
     * As seen on StackOverflow https://stackoverflow.com/a/36714242/1207186
     * Try to find the file path of a document uri
     * @param context the application context
     * @param uri the Uri for which the path should be resolved
     * @return the path corresponding to the Uri as a String
     * @throws URISyntaxException
    */
    private static @Nullable String internalGetFilePath(@NonNull Context context, @NonNull Uri uri) throws URISyntaxException {
        String selection = null;
        String[] selectionArgs = null;

        if (DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    uri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                }
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, projection, selection, selectionArgs, null)) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        throw new IllegalArgumentException("Unable to decode the given uri to a file path: " + uri);
    }

    public static void viewFile(String path, String mimeType, Context context) throws IOException {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(path);

        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", file);
        intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(contentUri, mimeType);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_no_app_for_gpx, Toast.LENGTH_LONG).show();
        }
    }

    public static void shareBytesAsFile(final Context context, final String name, final byte[] bytes) throws IOException {
        final File cacheDir = context.getCacheDir();
        final File rawCacheDir = new File(cacheDir, "raw");
        rawCacheDir.mkdir();
        final File file = new File(rawCacheDir, name);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
        } catch (final IOException e) {
            LOG.error("Failed to write bytes to temporary file", e);
            return;
        }

        shareFile(context, file);
    }

    public static void shareFile(final Context context, final File file) throws IOException {
        if (!file.exists()) {
            LOG.warn("File {} does not exist", file.getPath());
            return;
        }

        shareFile(context, file, "*/*");
    }

    public static void shareFile(final Context context, final File file, final String type) throws IOException {
        final Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider",
                file
        );

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(intent, "Share file"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_share_failed, Toast.LENGTH_LONG).show();
        }
    }

    public static void openWebsite(String url){
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        GBApplication.getContext().startActivity(i);
    }

    public static void openApp(String packageName) throws ClassNotFoundException {
        Context context = GBApplication.getContext();

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if(launchIntent == null){
            throw new ClassNotFoundException("App " + packageName + " cannot be found");
        }
        GBApplication.getContext().startActivity(launchIntent);
    }

    @Nullable
    public static PowerManager.WakeLock acquirePartialWakeLock(Context context, String tag, long timeout) {
        try {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Gadgetbridge:" + tag);
            wl.acquire(timeout);
            return wl;
        } catch (final Exception e) {
            LOG.error("Failed to take partial wake lock {}: ", tag, e);
            return null;
        }
    }
}

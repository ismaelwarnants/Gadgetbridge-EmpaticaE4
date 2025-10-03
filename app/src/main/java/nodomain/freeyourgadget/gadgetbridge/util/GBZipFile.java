/*  Copyright (C) 2022-2024 José Rebelo, MPeter

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Utility class for recognition and reading of ZIP archives.
 */
public class GBZipFile {
    private static final Logger LOG = LoggerFactory.getLogger(GBZipFile.class);
    public static final byte[] ZIP_HEADER = new byte[]{
        0x50, 0x4B, 0x03, 0x04
    };

    private final byte[] zipBytes;

    /**
     * Open ZIP file from byte array already in memory.
     * @param zipBytes data to handle as a ZIP file.
     */
    public GBZipFile(byte[] zipBytes) {
        this.zipBytes = zipBytes;
    }

    /**
     * Open ZIP file from InputStream.<br>
     * This will read the entire file into memory at once.
     * @param inputStream data to handle as a ZIP file.
     */
    public GBZipFile(InputStream inputStream) throws IOException {
        this.zipBytes = readAllBytes(inputStream);
    }

    /**
     * Checks if data resembles a ZIP file.<br>
     * The check is not infallible: it may report self-extracting or other exotic ZIP archives as not a ZIP file, and it may report a corrupted ZIP file as a ZIP file.
     * @param data The data to check.
     * @return Whether data resembles a ZIP file.
     */
    public static boolean isZipFile(byte[] data) {
        return ArrayUtils.equals(data, ZIP_HEADER, 0);
    }

    /**
     * Reads the contents of file at path into a byte array.
     * @param path Path of the file in the ZIP file.
     * @return byte array contatining the contents of the requested file.
     * @throws ZipFileException If the specified path does not exist or references a directory, or if some other I/O error occurs. In other words, if return value would otherwise be null.
     */
    public byte[] getFileFromZip(final String path) throws ZipFileException {
        try (InputStream is = new ByteArrayInputStream(zipBytes); ZipInputStream zipInputStream = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(path)) continue; // TODO: is this always a path? The documentation is very vague.

                if (zipEntry.isDirectory()) {
                    throw new ZipFileException(String.format("Path in ZIP file is a directory: %s", path));
                }

                return readAllBytes(zipInputStream);
            }

            throw new ZipFileException(String.format("Path in ZIP file was not found: %s", path));

        } catch (ZipException e) {
            throw new ZipFileException("The ZIP file might be corrupted", e);
        } catch (IOException e) {
            throw new ZipFileException("General IO error", e);
        }
    }

    public List<String> getAllFiles() throws ZipFileException {
        try (InputStream is = new ByteArrayInputStream(zipBytes); ZipInputStream zipInputStream = new ZipInputStream(is)) {
            final List<String> files = new ArrayList<>();
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                files.add(zipEntry.getName());
            }
            return files;
        } catch (ZipException e) {
            throw new ZipFileException("The ZIP file might be corrupted", e);
        } catch (IOException e) {
            throw new ZipFileException("General IO error", e);
        }
    }

    public boolean fileExists(final String path) throws ZipFileException {
        try (InputStream is = new ByteArrayInputStream(zipBytes); ZipInputStream zipInputStream = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(path)) {
                    continue;
                }

                if (zipEntry.isDirectory()) {
                    return false;
                }

                return true;
            }

            return false;
        } catch (final ZipException e) {
            throw new ZipFileException("The ZIP file might be corrupted", e);
        } catch (final IOException e) {
            throw new ZipFileException("General IO error", e);
        }
    }

    public static byte[] readAllBytes(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int n;
        byte[] buf = new byte[16384];

        while ((n = is.read(buf, 0, buf.length)) != -1) {
            buffer.write(buf, 0, n);
        }

        return buffer.toByteArray();
    }
}
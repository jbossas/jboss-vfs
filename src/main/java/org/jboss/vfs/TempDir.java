/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.vfs;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A temporary directory which exists until it is closed, at which time its contents will be removed.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TempDir implements Closeable {

    private final TempFileProvider provider;
    private final File root;
    private final AtomicBoolean open = new AtomicBoolean(true);

    TempDir(TempFileProvider provider, File root) {
        this.provider = provider;
        this.root = root;
    }

    /**
     * Get the {@code File} that represents the root of this temporary directory.  The returned file is only valid as
     * long as the tempdir exists.
     *
     * @return the root file
     * @throws IOException if the directory was closed at the time of this invocation
     */
    public File getRoot() throws IOException {
        if (!open.get()) {
            throw VFSMessages.MESSAGES.tempDirectoryClosed();
        }
        return root;
    }

    /**
     * Get the {@code File} for a relative path.  The returned file is only valid as long as the tempdir exists.
     *
     * @param relativePath the relative path
     * @return the corresponding file
     * @throws IOException if the directory was closed at the time of this invocation
     */
    public File getFile(String relativePath) throws IOException {
        if (!open.get()) {
            throw VFSMessages.MESSAGES.tempDirectoryClosed();
        }
        return new File(root, relativePath);
    }

    /**
     * Create a file within this temporary directory, prepopulating the file from the given input stream.
     *
     * @param relativePath the relative path name
     * @param sourceData   the source input stream to use
     * @return the file
     * @throws IOException if the directory was closed at the time of this invocation or an error occurs
     */
    public File createFile(String relativePath, InputStream sourceData) throws IOException {
        final File tempFile = getFile(relativePath);
        boolean ok = false;
        try {
            final FileOutputStream fos = new FileOutputStream(tempFile);
            try {
                VFSUtils.copyStream(sourceData, fos);
                fos.close();
                sourceData.close();
                ok = true;
                return tempFile;
            } finally {
                VFSUtils.safeClose(fos);
            }
        } finally {
            VFSUtils.safeClose(sourceData);
            if (!ok) {
                tempFile.delete();
            }
        }
    }

    /**
     * Close this directory.  The contents of the directory will be removed.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            provider.delete(root);
        }
    }

    protected void finalize() throws Throwable {
        VFSUtils.safeClose(this);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.vfs.util;

import static org.jboss.vfs.VFSMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

/**
 * Lazy input stream.
 * <p/>
 * Delaying opening stream from underlying virtual file as long as possible.
 * Won't be opened if not used at all.
 * <p/>
 * Synchronization is very simplistic, as it's highly unlikely
 * there will be a lot of concurrent requests.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class LazyInputStream extends InputStream {
    private VirtualFile file;
    private InputStream stream;

    public LazyInputStream(VirtualFile file) {
        if (file == null) {
            throw MESSAGES.nullArgument("file");
        }
        this.file = file;
    }

    /**
     * Open stream.
     *
     * @return file's stream
     * @throws IOException for any IO error
     */
    protected synchronized InputStream openStream() throws IOException {
        if (stream == null) { stream = file.openStream(); }
        return stream;
    }

    @Override
    public int read() throws IOException {
        return openStream().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return openStream().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return openStream().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return openStream().skip(n);
    }

    @Override
    public int available() throws IOException {
        return openStream().available();
    }

    @Override
    public synchronized void close() throws IOException {
        if (stream == null) { return; }

        openStream().close();
        stream = null; // reset the stream
    }

    @Override
    public void mark(int readlimit) {
        try {
            openStream().mark(readlimit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() throws IOException {
        openStream().reset();
    }

    @Override
    public boolean markSupported() {
        try {
            return openStream().markSupported();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

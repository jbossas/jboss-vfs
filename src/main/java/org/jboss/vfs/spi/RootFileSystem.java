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

package org.jboss.vfs.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSigner;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.vfs.VirtualFile;

/**
 * A special FileSystem which supports multiple roots.
 * <p/>
 * This is currently accomplished by requiring that VirtualFile.getPathName()
 * produce output that is consumable by java.io.File as a path.
 */
public final class RootFileSystem implements FileSystem {

    public static final RootFileSystem ROOT_INSTANCE = new RootFileSystem();

    private RootFileSystem() {
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return new FileInputStream(getFile(mountPoint, target));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public File getFile(VirtualFile mountPoint, VirtualFile target) {
        return new File(target.getPathName());
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).delete();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).length();
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).exists();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        return getFile(mountPoint, target).isFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        final String[] names = getFile(mountPoint, target).list();
        return names == null ? Collections.<String>emptyList() : Arrays.asList(names);
    }

    /**
     * {@inheritDoc}
     */
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public File getMountSource() {
        return null;
    }

    public URI getRootURI() throws URISyntaxException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        // no operation - the root FS can't be closed
    }
}

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jboss.vfs.VFSLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileAssembly;

/**
 * FileSystem used to mount an Assembly into the VFS.
 *
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class AssemblyFileSystem implements FileSystem {

    private final VirtualFileAssembly assembly;

    public AssemblyFileSystem(VirtualFileAssembly assembly) {
        this.assembly = assembly;
        VFSLogger.ROOT_LOGGER.tracef("Constructed a new assembly filesystem for %s", assembly);
    }

    /**
     * {@inheritDoc}
     */
    public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return getExistingFile(mountPoint, target).getPhysicalFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile != null && assemblyFile.delete();
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        if (mountPoint.equals(target)) {
            return true;
        }
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile != null) {
            return assemblyFile.exists();
        }
        return assembly.contains(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile != null && assemblyFile.isFile();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile == null) {
            return new ArrayList<String>(assembly.getChildNames(mountPoint, target));
        }
        final List<String> directoryEntries = new LinkedList<String>();
        for (VirtualFile child : assemblyFile.getChildren()) {
            directoryEntries.add(child.getName());
        }
        return directoryEntries;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile == null ? 0L : assemblyFile.getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile == null ? 0L : assemblyFile.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        if (mountPoint.equals(target)) { return true; }
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile != null) { return assemblyFile.isDirectory(); }
        return assembly.contains(mountPoint, target);
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
    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return getExistingFile(mountPoint, target).openStream();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        VFSLogger.ROOT_LOGGER.tracef("Closing assembly filesystem %s", this);
        assembly.close();
    }

    /**
     * {@inheritDoc}
     */
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile == null) {
            return null;
        }
        return assemblyFile.getCodeSigners();
    }

    /**
     * {@inheritDoc}
     */
    public File getMountSource() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public URI getRootURI() throws URISyntaxException {
        return null;
    }

    private VirtualFile getExistingFile(final VirtualFile mountPoint, final VirtualFile target) throws FileNotFoundException {
        final VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile == null) {
            throw new FileNotFoundException(target.getPathName());
        }
        return assemblyFile;
    }
}

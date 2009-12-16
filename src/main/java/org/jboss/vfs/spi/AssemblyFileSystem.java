/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.vfs.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileAssembly;
import org.jboss.logging.Logger;

/**
 * FileSystem used to mount an Assembly into the VFS.
 *
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class AssemblyFileSystem implements FileSystem {

    private static final Logger log = Logger.getLogger("org.jboss.vfs.assembly");

    private final VirtualFileAssembly assembly;

    public AssemblyFileSystem(VirtualFileAssembly assembly) {
        this.assembly = assembly;
        log.tracef("Constructed a new assembly filesystem for %s", assembly);
    }

    /** {@inheritDoc} */
    public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return getExistingFile(mountPoint, target).getPhysicalFile();
    }

    /** {@inheritDoc} */
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile != null && assemblyFile.delete();
    }

    /** {@inheritDoc} */
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        if(mountPoint.equals(target)) 
        {
           return true;
        }
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if(assemblyFile != null) {
           return assemblyFile.exists();
        }
        return assembly.contains(mountPoint, target);
    }

    /** {@inheritDoc} */
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile != null && assemblyFile.isFile();
    }

    /** {@inheritDoc} */
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile == null) {
           return new ArrayList<String>(assembly.getChildNames(mountPoint, target));
        }
        List<String> directoryEntries = new LinkedList<String>();
        for (VirtualFile child : assemblyFile.getChildren()) {
            directoryEntries.add(child.getName());
        }
        return directoryEntries;
    }

    /** {@inheritDoc} */
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile == null ? 0L : assemblyFile.getLastModified();
    }

    /** {@inheritDoc} */
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        return assemblyFile == null ? 0L : assemblyFile.getSize();
    }

    /** {@inheritDoc} */
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        if(mountPoint.equals(target))
           return true;
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if(assemblyFile != null)
           return assemblyFile.isDirectory();
        return assembly.contains(mountPoint, target);
    }

    /** {@inheritDoc} */
    public boolean isReadOnly() {
        return false;
    }

    /** {@inheritDoc} */
    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return getExistingFile(mountPoint, target).openStream();
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        log.tracef("Closing assembly filesystem %s", this);
        assembly.close();
    }
    
    /** {@inheritDoc} */
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
       VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
       if (assemblyFile == null) {
          return null;
       }
       return assemblyFile.getCodeSigners();
    }

    private VirtualFile getExistingFile(final VirtualFile mountPoint, final VirtualFile target) throws FileNotFoundException {
        VirtualFile assemblyFile = assembly.getFile(mountPoint, target);
        if (assemblyFile == null) {
            throw new FileNotFoundException(target.getPathName());
        }
        return assemblyFile;
    }
}

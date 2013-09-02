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

import static java.security.AccessController.doPrivileged;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSigner;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;

/**
 * A real filesystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RealFileSystem implements FileSystem {

    private static final Logger log = Logger.getLogger("org.jboss.vfs.real");

    private static final boolean NEEDS_CONVERSION = File.separatorChar != '/';

    private final File realRoot;
    private final boolean privileged;

    /**
     * Construct a real filesystem with the given real root.
     *
     * @param realRoot the real root
     */
    public RealFileSystem(File realRoot) {
        this(realRoot, true);
    }

    /**
     * Construct a real filesystem with the given real root.
     *
     * @param realRoot   the real root
     * @param privileged {@code true} to check permissions once up front, {@code false} to check at access time
     */
    public RealFileSystem(File realRoot, boolean privileged) {
        if (privileged) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new FilePermission(new File(realRoot, "-").getPath(), "read,delete"));
            }
        }
        this.realRoot = realRoot;
        this.privileged = privileged;
        log.tracef("Constructed real %s filesystem at root %s", privileged ? "privileged" : "unprivileged", realRoot);
    }

    private static <T> T doIoPrivileged(PrivilegedExceptionAction<T> action) throws IOException {
        try {
            return doPrivileged(action);
        } catch (PrivilegedActionException pe) {
            try {
                throw pe.getException();
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openInputStream(final VirtualFile mountPoint, final VirtualFile target) throws IOException {
        return privileged ? doIoPrivileged(new PrivilegedExceptionAction<InputStream>() {
            public InputStream run() throws Exception {
                return new FileInputStream(getFile(mountPoint, target));
            }
        }) : new FileInputStream(getFile(mountPoint, target));
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
        if (mountPoint.equals(target)) {
            return realRoot;
        } else if (NEEDS_CONVERSION) {
            return new File(realRoot, target.getPathNameRelativeTo(mountPoint).replace('/', File.separatorChar));
        } else {
            return new File(realRoot, target.getPathNameRelativeTo(mountPoint));
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFile(mountPoint, target);
        return privileged ? doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(file.delete());
            }
        }).booleanValue() : file.delete();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFile(mountPoint, target);
        return privileged ? doPrivileged(new PrivilegedAction<Long>() {
            public Long run() {
                return Long.valueOf(file.length());
            }
        }).longValue() : file.length();
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFile(mountPoint, target);
        return privileged ? doPrivileged(new PrivilegedAction<Long>() {
            public Long run() {
                return Long.valueOf(file.lastModified());
            }
        }).longValue() : file.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFile(mountPoint, target);
        return privileged ? doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(file.exists());
            }
        }).booleanValue() : file.exists();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        final File file = getFile(mountPoint, target);
        return privileged ? doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(file.isFile());
            }
        }).booleanValue() : file.isFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFile(mountPoint, target);
        return privileged ? doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(file.isDirectory());
            }
        }).booleanValue() : file.isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFile(mountPoint, target);
        final String[] names = privileged ? doPrivileged(new PrivilegedAction<String[]>() {
            public String[] run() {
                return file.list();
            }
        }) : file.list();
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
        return realRoot;
    }

    public URI getRootURI() throws URISyntaxException {
        return realRoot.toURI();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        // no operation - the real FS can't be closed
    }
}

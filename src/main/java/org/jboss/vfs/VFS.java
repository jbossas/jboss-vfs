/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.vfs.spi.AssemblyFileSystem;
import org.jboss.vfs.spi.FileSystem;
import org.jboss.vfs.spi.RealFileSystem;
import org.jboss.vfs.spi.JavaZipFileSystem;
import org.jboss.logging.Logger;
import org.jboss.net.protocol.URLStreamHandlerFactory;

/**
 * Virtual File System
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class VFS {

    private static final Logger log = Logger.getLogger("org.jboss.vfs");

    public static final boolean LEAK_DEBUGGING;

    private static final ConcurrentMap<VirtualFile, Map<String, Mount>> mounts = new ConcurrentHashMap<VirtualFile, Map<String, Mount>>();
    private static final VirtualFile rootVirtualFile = new VirtualFile("", null);
    private static final Mount rootMount = new Mount(RealFileSystem.ROOT_INSTANCE, rootVirtualFile);

    // todo - LRU VirtualFiles?
    // todo - LRU String intern?
    static {
        init();
        LEAK_DEBUGGING = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(System.getProperty("jboss.vfs.leakDebugging", "true"));
            }
        }).booleanValue();
    }

    /**
     * Do not allow construction 
     */
    private VFS() {
    }
    
    /**
     * Initialize VFS protocol handlers package property.
     */
    private static void init() {
        // If this doesn't work, hopefully the existing URLStreamHandlerFactory supports updates to the 'java.protocol.handler.pkgs' property.
        try {
           URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory());
        } catch (Throwable ignored) {}

        String pkgs = System.getProperty("java.protocol.handler.pkgs");
        if (pkgs == null || pkgs.trim().length() == 0) {
            pkgs = "org.jboss.net.protocol|org.jboss.vfs.protocol";
            System.setProperty("java.protocol.handler.pkgs", pkgs);
        } else if (pkgs.contains("org.jboss.vfs.protocol") == false) {
            if(pkgs.contains("org.jboss.net.protocol") == false)
                pkgs += "|org.jboss.net.protocol";
            pkgs += "|org.jboss.vfs.protocol";
            System.setProperty("java.protocol.handler.pkgs", pkgs);
        }
    }

    /**
     * Mount a filesystem on a mount point in the VFS.  The mount point is any valid file name, existant or non-existant.
     * If a relative path is given, it will be treated as relative to the VFS root.
     *
     * @param mountPoint the mount point
     * @param fileSystem the file system to mount
     *
     * @return a handle which can be used to unmount the filesystem
     *
     * @throws IOException if an I/O error occurs, such as a filesystem already being mounted at the given mount point
     */
    public static Closeable mount(VirtualFile mountPoint, FileSystem fileSystem) throws IOException {
        final VirtualFile parent = mountPoint.getParent();
        if (parent == null) {
            throw new IOException("Root filsystem already mounted");
        }
        final String name = mountPoint.getName();
        final Mount mount = new Mount(fileSystem, mountPoint);
        final ConcurrentMap<VirtualFile, Map<String, Mount>> mounts = VFS.mounts;
        for (; ;) {
            Map<String, Mount> childMountMap = mounts.get(parent);
            Map<String, Mount> newMap;
            if (childMountMap == null) {
                childMountMap = mounts.putIfAbsent(parent, Collections.singletonMap(name, mount));
                if (childMountMap == null) {
                    return mount;
                }
            }
            newMap = new HashMap<String, Mount>(childMountMap);
            if (newMap.put(name, mount) != null) {
                throw new IOException("Filsystem already mounted at mount point \"" + mountPoint + "\"");
            }
            if (mounts.replace(parent, childMountMap, newMap)) {
                log.tracef("Mounted filesystem %s on mount point %s", fileSystem, mountPoint);
                return mount;
            }
        }
    }

    /**
     * Find a virtual file.
     *
     * @param url the URL whose path component is the child path
     *
     * @return the child
     *
     * @throws IllegalArgumentException if the path is null
     * @throws java.net.URISyntaxException for any uri error
     */
    public static VirtualFile getChild(URL url) throws URISyntaxException {
        return getChild(url.toURI());
    }

    /**
     * Find a virtual file.
     *
     * @param uri the URI whose path component is the child path
     *
     * @return the child
     *
     * @throws IllegalArgumentException if the path is null
     */
    public static VirtualFile getChild(URI uri) {
        return getChild(uri.getPath());
    }

    /**
     * Find a virtual file.
     *
     * @param path the child path
     *
     * @return the child
     *
     * @throws IllegalArgumentException if the path is null
     */
    public static VirtualFile getChild(String path) {
        if (path == null)
            throw new IllegalArgumentException("Null path");
        return getRootVirtualFile().getChild(path);
    }

    /**
     * Get the root virtual file for this VFS instance.
     *
     * @return the root virtual file
     */
    public static VirtualFile getRootVirtualFile() {
        return rootVirtualFile;
    }

    /**
     * Get the children
     *
     * @return the children
     *
     * @throws IOException for any problem accessing the virtual file system
     */
    public static List<VirtualFile> getChildren() throws IOException {
        return getRootVirtualFile().getChildren(null);
    }

    /**
     * Get the children
     *
     * @param filter to filter the children
     *
     * @return the children
     *
     * @throws IOException for any problem accessing the virtual file system
     */
    public static List<VirtualFile> getChildren(VirtualFileFilter filter) throws IOException {
        return getRootVirtualFile().getChildren(filter);
    }

    /**
     * Get all the children recursively<p>
     * <p/>
     * This always uses {@link VisitorAttributes#RECURSE}
     *
     * @return the children
     *
     * @throws IOException for any problem accessing the virtual file system
     */
    public static List<VirtualFile> getChildrenRecursively() throws IOException {
        return getRootVirtualFile().getChildrenRecursively(null);
    }

    /**
     * Get all the children recursively<p>
     * <p/>
     * This always uses {@link VisitorAttributes#RECURSE}
     *
     * @param filter to filter the children
     *
     * @return the children
     *
     * @throws IOException for any problem accessing the virtual file system
     */
    public static List<VirtualFile> getChildrenRecursively(VirtualFileFilter filter) throws IOException {
        return getRootVirtualFile().getChildrenRecursively(filter);
    }

    /**
     * Visit the virtual file system from the root
     *
     * @param visitor the visitor
     *
     * @throws IOException for any problem accessing the VFS
     * @throws IllegalArgumentException if the visitor is null
     */
    public static void visit(VirtualFileVisitor visitor) throws IOException {
        visitor.visit(getRootVirtualFile());
    }

    /**
     * Visit the virtual file system
     *
     * @param file the file
     * @param visitor the visitor
     *
     * @throws IOException for any problem accessing the VFS
     * @throws IllegalArgumentException if the file or visitor is null
     */
    protected static void visit(VirtualFile file, VirtualFileVisitor visitor) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("Null file");
        visitor.visit(file);
    }

    static Mount getMount(VirtualFile virtualFile) {
        final ConcurrentMap<VirtualFile, Map<String, Mount>> mounts = VFS.mounts;
        for (; ;) {
            final VirtualFile parent = virtualFile.getParent();
            if (parent == null) {
                return rootMount;
            }
            final Map<String, Mount> parentMounts = mounts.get(parent);
            if (parentMounts == null) {
                virtualFile = parent;
            } else {
                final Mount mount = parentMounts.get(virtualFile.getName());
                if (mount == null) {
                    virtualFile = parent;
                } else {
                    return mount;
                }
            }
        }
    }

    /**
     * Get all immediate submounts for a path.
     *
     * @param virtualFile the path
     *
     * @return the collection of present mount (simple) names
     */
    static Set<String> getSubmounts(VirtualFile virtualFile) {
        final ConcurrentMap<VirtualFile, Map<String, Mount>> mounts = VFS.mounts;
        final Map<String, Mount> mountMap = mounts.get(virtualFile);
        if (mountMap == null) {
            return emptyRemovableSet();
        }
        return new HashSet<String>(mountMap.keySet());
    }

    private static Closeable doMount(final FileSystem fileSystem, final VirtualFile mountPoint) throws IOException {
        boolean ok = false;
        try {
            final Closeable mountHandle = mount(mountPoint, fileSystem);
            final Closeable closeable = new Closeable() {
                public void close() throws IOException {
                    VFSUtils.safeClose(mountHandle);
                    VFSUtils.safeClose(fileSystem);
                }
            };
            ok = true;
            return closeable;
        } finally {
            if (!ok) {
                VFSUtils.safeClose(fileSystem);
            }
        }
    }

    /**
     * Create and mount a zip file into the filesystem, returning a single handle which will unmount and close the file
     * system when closed.
     *
     * @param zipFile the zip file to mount
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountZip(File zipFile, VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        boolean ok = false;
        final TempDir tempDir = tempFileProvider.createTempDir(zipFile.getName());
        try {
            final Closeable closeable = doMount(new JavaZipFileSystem(zipFile, tempDir), mountPoint);
            ok = true;
            return closeable;
        } finally {
            if (!ok) {
                VFSUtils.safeClose(tempDir);
            }
        }
    }

    /**
     * Create and mount a zip file into the filesystem, returning a single handle which will unmount and close the file
     * system when closed.
     *
     * @param zipData an input stream containing the zip data
     * @param zipName the name of the archive
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountZip(InputStream zipData, String zipName, VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        boolean ok = false;
        try {
            final TempDir tempDir = tempFileProvider.createTempDir(zipName);
            try {
                final Closeable closeable = doMount(new JavaZipFileSystem(zipName, zipData, tempDir), mountPoint);
                ok = true;
                return closeable;
            } finally {
                if (!ok) {
                    VFSUtils.safeClose(tempDir);
                }
            }
        } finally {
            VFSUtils.safeClose(zipData);
        }
    }

    /**
     * Create and mount a zip file into the filesystem, returning a single handle which will unmount and close the file
     * system when closed.
     *
     * @param zipFile a zip file in the VFS
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountZip(VirtualFile zipFile, VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        return mountZip(zipFile.openStream(), zipFile.getName(), mountPoint, tempFileProvider);
    }

    /**
     * Create and mount a real file system, returning a single handle which will unmount and close the filesystem when
     * closed.
     *
     * @param realRoot the real filesystem root
     * @param mountPoint the point at which the filesystem should be mounted
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountReal(File realRoot, VirtualFile mountPoint) throws IOException {
        return doMount(new RealFileSystem(realRoot), mountPoint);
    }

    /**
     * Create and mount a temporary file system, returning a single handle which will unmount and close the filesystem
     * when closed.
     *
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountTemp(VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        boolean ok = false;
        final TempDir tempDir = tempFileProvider.createTempDir("tmpfs");
        try {
            final Closeable closeable = doMount(new RealFileSystem(tempDir.getRoot()), mountPoint);
            ok = true;
            return new Closeable() {
                public void close() throws IOException {
                    VFSUtils.safeClose(closeable);
                    VFSUtils.safeClose(tempDir);
                }
            };
        } finally {
            if (!ok) {
                VFSUtils.safeClose(tempDir);
            }
        }
    }

    /**
     * Create and mount an expanded zip file in a temporary file system, returning a single handle which will unmount and
     * close the filesystem when closed.
     *
     * @param zipFile the zip file to mount
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountZipExpanded(File zipFile, VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        boolean ok = false;
        final TempDir tempDir = tempFileProvider.createTempDir(zipFile.getName());
        try {
            final File rootFile = tempDir.getRoot();
            VFSUtils.unzip(zipFile, rootFile);
            final Closeable closeable = doMount(new RealFileSystem(rootFile), mountPoint);
            ok = true;
            return new Closeable() {
                public void close() throws IOException {
                    VFSUtils.safeClose(closeable);
                    VFSUtils.safeClose(tempDir);
                }
            };
        } finally {
            if (!ok) {
                VFSUtils.safeClose(tempDir);
            }
        }
    }

    /**
     * Create and mount an expanded zip file in a temporary file system, returning a single handle which will unmount and
     * close the filesystem when closed.  The given zip data stream is closed.
     *
     * @param zipData an input stream containing the zip data
     * @param zipName the name of the archive
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountZipExpanded(InputStream zipData, String zipName, VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        try {
            boolean ok = false;
            final TempDir tempDir = tempFileProvider.createTempDir(zipName);
            try {
                final File zipFile = File.createTempFile(zipName + "-", ".tmp", tempDir.getRoot());
                try {
                    final FileOutputStream os = new FileOutputStream(zipFile);
                    try {
                        // allow an error on close to terminate the unzip
                        VFSUtils.copyStream(zipData, os);
                        zipData.close();
                        os.close();
                    } finally {
                        VFSUtils.safeClose(zipData);
                        VFSUtils.safeClose(os);
                    }
                    final File rootFile = tempDir.getRoot();
                    VFSUtils.unzip(zipFile, rootFile);
                    final Closeable closeable = doMount(new RealFileSystem(rootFile), mountPoint);
                    ok = true;
                    return new Closeable() {
                        public void close() throws IOException {
                            VFSUtils.safeClose(closeable);
                            VFSUtils.safeClose(tempDir);
                        }
                    };
                } finally {
                    zipFile.delete();
                }
            } finally {
                if (!ok) {
                    VFSUtils.safeClose(tempDir);
                }
            }
        } finally {
            VFSUtils.safeClose(zipData);
        }
    }

    /**
     * Create and mount an expanded zip file in a temporary file system, returning a single handle which will unmount and
     * close the filesystem when closed.  The given zip data stream is closed.
     *
     * @param zipFile a zip file in the VFS
     * @param mountPoint the point at which the filesystem should be mounted
     * @param tempFileProvider the temporary file provider
     *
     * @return a handle
     *
     * @throws IOException if an error occurs
     */
    public static Closeable mountZipExpanded(VirtualFile zipFile, VirtualFile mountPoint, TempFileProvider tempFileProvider) throws IOException {
        return mountZipExpanded(zipFile.openStream(), zipFile.getName(), mountPoint, tempFileProvider);
    }
    
    /**
     * Create and mount an assembly file system, returning a single handle which will unmount and
     * close the filesystem when closed. 
     * 
     * @param assembly an {@link VirtualFileAssembly} to mount in the VFS
     * @param mountPoint the point at which the filesystem should be mounted
     * @return a handle
     * 
     * @throws IOException if an error occurs
     */
    public static Closeable mountAssembly(VirtualFileAssembly assembly, VirtualFile mountPoint) throws IOException {
       return doMount(new AssemblyFileSystem(assembly), mountPoint);
    }

    @SuppressWarnings({ "unchecked" })
    private static <E> Set<E> emptyRemovableSet() {
        return EMPTY_REMOVABLE_SET;
    }

    @SuppressWarnings("unchecked")
   private static final Set EMPTY_REMOVABLE_SET = new EmptyRemovableSet();

    private static final class EmptyRemovableSet<E> extends AbstractSet<E> {

        public boolean remove(Object o) {
            return false;
        }

        public boolean retainAll(Collection<?> c) {
            return false;
        }

        public void clear() {
        }

        public Iterator<E> iterator() {
            return Collections.<E>emptySet().iterator();
        }

        public int size() {
            return 0;
        }
    }

    /**
     * The mount representation.  This instance represents a binding between a position in the virtual filesystem and the
     * backing filesystem implementation; the same {@code FileSystem} may be mounted in more than one place, however only
     * one {@code FileSystem} may be bound to a specific path at a time.
     */
    final static class Mount implements Closeable {

        private final FileSystem fileSystem;
        private final VirtualFile mountPoint;
        private final StackTraceElement[] allocationPoint;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        Mount(FileSystem fileSystem, VirtualFile mountPoint) {
            this.fileSystem = fileSystem;
            this.mountPoint = mountPoint;
            allocationPoint = Thread.currentThread().getStackTrace();
        }

        public void close() throws IOException {
            if (closed.getAndSet(true)) {
                return;
            }
            final String name = mountPoint.getName();
            final VirtualFile parent = mountPoint.getParent();
            final ConcurrentMap<VirtualFile, Map<String, Mount>> mounts = VFS.mounts;
            for (; ;) {
                final Map<String, Mount> parentMounts = mounts.get(parent);
                if (parentMounts == null) {
                    return;
                }
                final VFS.Mount mount = parentMounts.get(name);
                if (mount != this) {
                    return;
                }
                final Map<String, Mount> newParentMounts;
                if (parentMounts.size() == 2) {
                    final Iterator<Map.Entry<String, Mount>> ei = parentMounts.entrySet().iterator();
                    final Map.Entry<String, Mount> e1 = ei.next();
                    if (e1.getKey().equals(name)) {
                        final Map.Entry<String, Mount> e2 = ei.next();
                        newParentMounts = Collections.singletonMap(e2.getKey(), e2.getValue());
                    } else {
                        newParentMounts = Collections.singletonMap(e1.getKey(), e1.getValue());
                    }
                    if (mounts.replace(parent, parentMounts, newParentMounts)) {
                        log.tracef("Unmounted filesystem %s on mount point %s", fileSystem, mountPoint);
                        return;
                    }
                } else if (parentMounts.size() == 1) {
                    if (mounts.remove(parent, parentMounts)) {
                        log.tracef("Unmounted filesystem %s on mount point %s", fileSystem, mountPoint);
                        return;
                    }
                } else {
                    newParentMounts = new HashMap<String, Mount>(parentMounts);
                    newParentMounts.remove(name);
                    if (mounts.replace(parent, parentMounts, newParentMounts)) {
                        log.tracef("Unmounted filesystem %s on mount point %s", fileSystem, mountPoint);
                        return;
                    }
                }
            }
        }

        FileSystem getFileSystem() {
            return fileSystem;
        }

        VirtualFile getMountPoint() {
            return mountPoint;
        }

        protected void finalize() throws IOException {
            if (!closed.get()) {
                final StackTraceElement[] allocationPoint = this.allocationPoint;
                if (allocationPoint != null) {
                    final LeakDescriptor t = new LeakDescriptor();
                    t.setStackTrace(allocationPoint);
                    log.warnf(t, "A VFS mount (%s) was leaked!", mountPoint);
                } else {
                    log.warnf("A VFS mount (%s) was leaked!", mountPoint);
                }
                close();
            }
        }
    }

    private static final class LeakDescriptor extends Throwable {

        private static final long serialVersionUID = 6034058126740270584L;

        public String toString() {
            return "Allocation stack trace:";
        }
    }
}

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

import org.jboss.jzipfile.ZipEntry;
import org.jboss.jzipfile.Zip;
import org.jboss.jzipfile.ZipCatalog;
import org.jboss.jzipfile.ZipEntryType;
import org.jboss.vfs.util.PathTokenizer;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

/**
 * {@inheritDoc}
 * <p/>
 * This implementation is backed by a zip file.  The provided file must be owned by this instance; otherwise, if the
 * file disappears unexpectedly, the filesystem will malfunction.
 */
public final class JZipFileSystem implements FileSystem {

    private final File zipFile;
    private final long zipTime;
    private final ZipNode rootNode;
    private final TempDir tempDir;
    private final File contentsDir;

    /**
     * Create a new instance.
     *
     * @param name the name of the source archive
     * @param inputStream an input stream from the source archive
     * @param tempDir the temp dir into which zip information is stored
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public JZipFileSystem(String name, InputStream inputStream, TempDir tempDir) throws IOException {
        this(tempDir.createFile(name, inputStream), tempDir);
    }

    /**
     * Create a new instance.
     *
     * @param zipFile the original archive file
     * @param tempDir the temp dir into which zip information is stored
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public JZipFileSystem(File zipFile, TempDir tempDir) throws IOException {
        zipTime = zipFile.lastModified();
        this.zipFile = zipFile;
        final ZipCatalog catalog = Zip.readCatalog(zipFile);
        final ZipNode rootNode = new ZipNode(new HashMap<String, ZipNode>(), "", null);
        FILES:
        for (ZipEntry entry : catalog.allEntries()) {
            final String name = entry.getName();
            final boolean isDirectory = entry.getEntryType() == ZipEntryType.DIRECTORY;
            final List<String> tokens = PathTokenizer.getTokens(name);
            ZipNode node = rootNode;
            final Iterator<String> it = tokens.iterator();
            while (it.hasNext()) {
                String token = it.next();
                if (PathTokenizer.isCurrentToken(token) || PathTokenizer.isReverseToken(token)) {
                    // invalid file name
                    continue FILES;
                }
                final Map<String, ZipNode> children = node.children;
                if (children == null) {
                    // todo - log bad zip entry
                    continue FILES;
                }
                ZipNode child = children.get(token.toLowerCase());
                if (child == null) {
                    child = it.hasNext() || isDirectory ? new ZipNode(new HashMap<String, ZipNode>(), token, null) : new ZipNode(null, token, entry);
                    children.put(token.toLowerCase(), child);
                }
                node = child;
            }
        }
        this.rootNode = rootNode;
        this.tempDir = tempDir;
        contentsDir = tempDir.getFile("contents");
        contentsDir.mkdir();
    }

    public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = getExistingZipNode(mountPoint, target);
        // check if we have cached one already
        File cachedFile = zipNode.cachedFile;
        if (cachedFile != null) {
            return cachedFile;
        }
        synchronized (zipNode) {
            // double-check
            cachedFile = zipNode.cachedFile;
            if (cachedFile != null) {
                return cachedFile;
            }
            // nope, create a cached temp
            final ZipEntry entry = getNodeEntry(zipNode);
            final String name = entry.getName();
            cachedFile = new File(contentsDir, name);
            VFSUtils.copyStreamAndClose(Zip.openEntry(zipFile, entry), new BufferedOutputStream(new FileOutputStream(cachedFile)));
            zipNode.cachedFile = cachedFile;
            return cachedFile;
        }
    }

    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = getExistingZipNode(mountPoint, target);
        final File cachedFile = zipNode.cachedFile;
        if (cachedFile != null) {
            return new FileInputStream(cachedFile);
        }
        final ZipEntry entry = zipNode.entry;
        if (entry == null) {
            throw new IOException("Not a file: \"" + target.getPathName() + "\"");
        }
        return Zip.openEntry(zipFile, entry);
    }

    public boolean delete(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = getExistingZipNode(mountPoint, target);
        final File cachedFile = zipNode.cachedFile;
        return cachedFile != null && cachedFile.delete();
    }

    public long getSize(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = getExistingZipNode(mountPoint, target);
        final File cachedFile = zipNode.cachedFile;
        final ZipEntry entry = zipNode.entry;
        return cachedFile != null ? cachedFile.length() : entry == null ? 0L : entry.getSize();
    }

    public long getLastModified(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = getExistingZipNode(mountPoint, target);
        final File cachedFile = zipNode.cachedFile;
        final ZipEntry entry = zipNode.entry;
        return cachedFile != null ? cachedFile.lastModified() : entry == null ? zipTime : entry.getModificationTime();
    }

    public boolean exists(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = rootNode.find(mountPoint, target);
        if (zipNode == null) {
            return false;
        } else {
            final File cachedFile = zipNode.cachedFile;
            return cachedFile == null || cachedFile.exists();
        }
    }

    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        final ZipNode zipNode = rootNode.find(mountPoint, target);
        return zipNode != null && zipNode.entry == null;
    }

    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) throws IOException {
        final ZipNode zipNode = getExistingZipNode(mountPoint, target);
        final Map<String, ZipNode> children = zipNode.children;
        if (children == null) {
            return Collections.emptyList();
        }
        final Collection<ZipNode> values = children.values();
        final List<String> names = new ArrayList<String>(values.size());
        for (ZipNode node : values) {
            names.add(node.name);
        }
        return names;
    }

    private ZipEntry getNodeEntry(ZipNode zipNode)
            throws IOException {
        final ZipEntry entry = zipNode.entry;
        if (entry == null) {
            throw new IOException("Cannot call this operation on a directory");
        }
        return entry;
    }

    private ZipNode getExistingZipNode(VirtualFile mountPoint, VirtualFile target)
            throws FileNotFoundException {
        final ZipNode zipNode = rootNode.find(mountPoint, target);
        if (zipNode == null) {
            throw new FileNotFoundException(target.getPathName());
        }
        return zipNode;
    }

    public boolean isReadOnly() {
        return true;
    }

    public void close() throws IOException {
        tempDir.close();
    }

    private static final class ZipNode {

        // immutable child map
        private final Map<String, ZipNode> children;
        private final String name;
        private final ZipEntry entry;
        private volatile File cachedFile;

        private ZipNode(Map<String, ZipNode> children, String name, ZipEntry entry) {
            this.children = children;
            this.name = name;
            this.entry = entry;
        }

        private ZipNode find(VirtualFile mountPoint, VirtualFile target) {
            if (mountPoint.equals(target)) {
                return this;
            } else {
                final ZipNode parent = find(mountPoint, target.getParent());
                if (parent == null) {
                    return null;
                }
                final Map<String, ZipNode> children = parent.children;
                if (children == null) {
                    return null;
                }
                return children.get(target.getLowerCaseName());
            }
        }
    }
}

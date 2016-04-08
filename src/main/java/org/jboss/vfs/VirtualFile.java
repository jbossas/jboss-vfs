/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.vfs.util.FilterVirtualFileVisitor;
import org.jboss.vfs.util.MatchAllVirtualFileFilter;
import org.jboss.vfs.util.PathTokenizer;

/**
 * A virtual file.  This is a symbolic reference to a location in the virtual file system hierarchy.  Holding a {@code
 * VirtualFile} instance gives no guarantees as to the presence or immutability of the referenced file or any of its
 * parent path elements.
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @author Ales.Justin@jboss.org
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @version $Revision: 44334 $
 */
public final class VirtualFile implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;
    private final String lcname;
    private final VirtualFile parent;
    private final int hashCode;
    private String pathName;

    VirtualFile(String name, VirtualFile parent) {
        this.name = name;
        lcname = name.toLowerCase();
        this.parent = parent;
        int result = parent == null ? 1 : parent.hashCode();
        result = 31 * result + name.hashCode();
        hashCode = result;
    }

    /**
     * Get the simple VF name (X.java)
     *
     * @return the simple file name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the simple VF name mapped to lowercase (x.java) (used by case-insensitive filesystems like ZIP).
     *
     * @return the lowercase simple file name
     * @deprecated should not be used anymore, as the code is case-sensitive from JBVFS-170
     */
    public String getLowerCaseName() {
        return lcname;
    }

    /**
     * Get the absolute VFS full path name (/xxx/yyy/foo.ear/baz.jar/org/jboss/X.java)
     *
     * @return the VFS full path name
     */
    public String getPathName() {
        return getPathName(false);
    }

    /**
     * Get the path name relative to a parent virtual file.  If the given virtual file is not a parent of
     * this virtual file, then an {@code IllegalArgumentException} is thrown.
     *
     * @param parent the parent virtual file
     * @return the relative path name as a string
     * @throws IllegalArgumentException if the given virtual file is not a parent of this virtual file
     */
    public String getPathNameRelativeTo(VirtualFile parent) throws IllegalArgumentException {
        final StringBuilder builder = new StringBuilder(160);
        getPathNameRelativeTo(parent, builder);
        return builder.toString();
    }

    private void getPathNameRelativeTo(VirtualFile parent, StringBuilder builder) {
        if (this.parent == null) {
            throw VFSMessages.MESSAGES.parentIsNotAncestor(parent);
        }
        if (this.equals(parent)) {
            return;
        }
        if (!this.parent.equals(parent)) {
            this.parent.getPathNameRelativeTo(parent, builder);
            builder.append('/');
        }
        builder.append(name);
    }

    /**
     * Get the absolute VFS full path name. If this is a URL then directory entries will have a trailing slash.
     *
     * @param url whether or not this path is being used for a URL
     * @return the VFS full path name
     */
    String getPathName(boolean url) {
        if (pathName ==null || pathName.isEmpty()) {
            VirtualFile[] path = getParentFiles();
            final StringBuilder builder = new StringBuilder(path.length*30+50);
            for (int i=path.length-1;i>-1;i--) {
                final VirtualFile parent = path[i].parent;
                if (parent == null){
                    builder.append(path[i].name);
                }else{
                    if (parent.parent != null) {
                        builder.append('/');
                    }
                    builder.append(path[i].name);
                }
            }
            pathName = builder.toString();
        }
        // Perhaps this should be cached to avoid the fs stat call?
        return (url && isDirectory())? pathName.concat("/"): pathName;
    }

    /**
     * When the file was last modified
     *
     * @return the last modified time
     */
    public long getLastModified() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return mount.getFileSystem().getLastModified(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().getLastModified(mount.getMountPoint(), this);
    }

    /**
     * Get the size
     *
     * @return the size
     */
    public long getSize() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return mount.getFileSystem().getSize(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().getSize(mount.getMountPoint(), this);
    }

    /**
     * Tests whether the underlying implementation file still exists.
     *
     * @return true if the file exists, false otherwise.
     */
    public boolean exists() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return mount.getFileSystem().exists(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().exists(mount.getMountPoint(), this);
    }

    /**
     * Determines whether this virtual file represents a true root of a file system.
     * On UNIX, there is only one root "/". Howevever, on Windows there are an infinite
     * number of roots that correspond to drives, or UNC paths.
     *
     * @return {@code true} if this represents a root.
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Whether it is a simple leaf of the VFS, i.e. whether it can contain other files
     *
     * @return {@code true} if a simple file
     * @deprecated use {@link #isDirectory()} or {@link #isFile()} instead
     */
    @Deprecated
    public boolean isLeaf() {
        return isFile();
    }

    /**
     * Determine whether the named virtual file is a plain file.
     *
     * @return {@code true} if it is a plain file, {@code false} otherwise
     */
    public boolean isFile() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return mount.getFileSystem().isFile(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().isFile(mount.getMountPoint(), this);
    }

    /**
     * Determine whether the named virtual file is a directory.
     *
     * @return {@code true} if it is a directory, {@code false} otherwise
     */
    public boolean isDirectory() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return mount.getFileSystem().isDirectory(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().isDirectory(mount.getMountPoint(), this);
    }

    /**
     * Access the file contents.
     *
     * @return an InputStream for the file contents.
     * @throws IOException for any error accessing the file system
     */
    public InputStream openStream() throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        if (isDirectory()) {
            return new VirtualJarInputStream(this);
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return doIoPrivileged(new PrivilegedExceptionAction<InputStream>() {
                @Override
                public InputStream run() throws Exception {
                    return mount.getFileSystem().openInputStream(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().openInputStream(mount.getMountPoint(), this);
    }

    /**
     * Delete this virtual file
     *
     * @return {@code true} if file was deleted
     */
    public boolean delete() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "delete"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return mount.getFileSystem().delete(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().delete(mount.getMountPoint(), this);
    }

    /**
     * Get a physical file for this virtual file.  Depending on the underlying file system type, this may simply return
     * an already-existing file; it may create a copy of a file; or it may reuse a preexisting copy of the file.
     * Furthermore, the returned file may or may not have any relationship to other files from the same or any other
     * virtual directory.
     *
     * @return the physical file
     * @throws IOException if an I/O error occurs while producing the physical file
     */
    public File getPhysicalFile() throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "getfile"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        if (sm != null) {
            final VirtualFile target = this;
            return doIoPrivileged(new PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws Exception {
                    return mount.getFileSystem().getFile(mount.getMountPoint(), target);
                }
            });
        }
        return mount.getFileSystem().getFile(mount.getMountPoint(), this);
    }

    private static <T> T doIoPrivileged(PrivilegedExceptionAction<T> action) throws IOException {
        try {
            return AccessController.doPrivileged(action);
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
     * Get a {@code VirtualFile} which represents the parent of this instance.
     *
     * @return the parent or {@code null} if there is no parent
     */
    public VirtualFile getParent() {
        return parent;
    }

    /**
     * Get the all the parent files of this virtual file from this file to the root.  The leafmost file will be at the
     * start of the array, and the rootmost will be at the end.
     *
     * @return the array of parent files
     */
    public VirtualFile[] getParentFiles() {
        return getParentFiles(0);
    }

    /**
     * Get the all the parent files of this virtual file from this file to the root as a list.  The leafmost file will be
     * at the start of the list, and the rootmost will be at the end.
     *
     * @return the list of parent files
     */
    public List<VirtualFile> getParentFileList() {
        return Arrays.asList(getParentFiles());
    }

    private VirtualFile[] getParentFiles(int idx) {
        final VirtualFile[] array;
        if (parent == null) {
            array = new VirtualFile[idx + 1];
        } else {
            array = parent.getParentFiles(idx + 1);
        }
        array[idx] = this;
        return array;
    }

    /**
     * Get the children.  This is the combined list of real children within this directory, as well as virtual children
     * created by submounts.
     *
     * @return the children
     */
    public List<VirtualFile> getChildren() {
        // isDirectory does the read security check
        if (!isDirectory()) { return Collections.emptyList(); }
        final VFS.Mount mount = VFS.getMount(this);
        final Set<String> submounts = VFS.getSubmounts(this);
        final List<String> names = mount.getFileSystem().getDirectoryEntries(mount.getMountPoint(), this);
        final List<VirtualFile> virtualFiles = new ArrayList<VirtualFile>(names.size() + submounts.size());
        for (String name : names) {
            final VirtualFile child = new VirtualFile(name, this);
            virtualFiles.add(child);
            submounts.remove(name);
        }
        for (String name : submounts) {
            final VirtualFile child = new VirtualFile(name, this);
            virtualFiles.add(child);
        }
        return virtualFiles;
    }

    /**
     * Get the children
     *
     * @param filter to filter the children
     * @return the children
     * @throws IOException           for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed or it is a leaf node
     */
    public List<VirtualFile> getChildren(VirtualFileFilter filter) throws IOException {
        // isDirectory does the read security check
        if (!isDirectory()) { return Collections.emptyList(); }
        if (filter == null) { filter = MatchAllVirtualFileFilter.INSTANCE; }
        FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, null);
        visit(visitor);
        return visitor.getMatched();
    }

    /**
     * Get all the children recursively<p>
     * <p/>
     * This always uses {@link VisitorAttributes#RECURSE}
     *
     * @return the children
     * @throws IOException           for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed
     */
    public List<VirtualFile> getChildrenRecursively() throws IOException {
        return getChildrenRecursively(null);
    }

    /**
     * Get all the children recursively<p>
     * <p/>
     * This always uses {@link VisitorAttributes#RECURSE}
     *
     * @param filter to filter the children
     * @return the children
     * @throws IOException           for any problem accessing the virtual file system
     * @throws IllegalStateException if the file is closed or it is a leaf node
     */
    public List<VirtualFile> getChildrenRecursively(VirtualFileFilter filter) throws IOException {
        // isDirectory does the read security check
        if (!isDirectory()) { return Collections.emptyList(); }
        if (filter == null) { filter = MatchAllVirtualFileFilter.INSTANCE; }
        FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, VisitorAttributes.RECURSE);
        visit(visitor);
        return visitor.getMatched();
    }

    /**
     * Visit the virtual file system
     *
     * @param visitor the visitor
     * @throws IOException              for any problem accessing the virtual file system
     * @throws IllegalArgumentException if the visitor is null
     * @throws IllegalStateException    if the file is closed
     */
    public void visit(VirtualFileVisitor visitor) throws IOException {
        visit(visitor, true);
    }

    private void visit(VirtualFileVisitor visitor, boolean root) throws IOException {
        final VisitorAttributes visitorAttributes = visitor.getAttributes();
        if (root && visitorAttributes.isIncludeRoot()) { visitor.visit(this); }
        // isDirectory does the read security check
        if (!isDirectory()) { return; }
        for (VirtualFile child : getChildren()) {
            // Always visit a leaf, and visit directories when leaves only is false
            if (!child.isDirectory() || !visitorAttributes.isLeavesOnly()) { visitor.visit(child); }
            if (child.isDirectory() && visitorAttributes.isRecurse(child)) { child.visit(visitor, false); }
        }
    }

    /**
     * Get a child virtual file.  The child may or may not exist in the virtual filesystem.
     *
     * @param path the path
     * @return the child
     * @throws IllegalArgumentException if the path is null
     */
    public VirtualFile getChild(String path) {
        if (path == null) {
            throw VFSMessages.MESSAGES.nullArgument("path");
        }
        final List<String> pathParts = PathTokenizer.getTokens(path);
        VirtualFile current = this;
        for (String part : pathParts) {
            if (PathTokenizer.isReverseToken(part)) {
                final VirtualFile parent = current.parent;
                current = parent == null ? current : parent;
            } else if (PathTokenizer.isCurrentToken(part) == false) {
                current = new VirtualFile(part, current);
            }
        }
        return current;
    }

    /**
     * Get file's current URL.  <b>Note:</b> if this VirtualFile refers to a directory <b>at the time of this
     * method invocation</b>, a trailing slash will be appended to the URL; this means that invoking
     * this method may require a filesystem access, and in addition, may not produce consistent results
     * over time.
     *
     * @return the current url
     * @throws MalformedURLException if the URL is somehow malformed
     * @see VirtualFile#asDirectoryURL()
     * @see VirtualFile#asFileURL()
     */
    public URL toURL() throws MalformedURLException {
        return VFSUtils.getVirtualURL(this);
    }

    /**
     * Get file's current URI.  <b>Note:</b> if this VirtualFile refers to a directory <b>at the time of this
     * method invocation</b>, a trailing slash will be appended to the URI; this means that invoking
     * this method may require a filesystem access, and in addition, may not produce consistent results
     * over time.
     *
     * @return the current uri
     * @throws URISyntaxException if the URI is somehow malformed
     * @see VirtualFile#asDirectoryURI()
     * @see VirtualFile#asFileURI()
     */
    public URI toURI() throws URISyntaxException {
        return VFSUtils.getVirtualURI(this);
    }

    /**
     * Get file's URL as a directory.  There will always be a trailing {@code "/"} character.
     *
     * @return the url
     * @throws MalformedURLException if the URL is somehow malformed
     */
    public URL asDirectoryURL() throws MalformedURLException {
        final String pathName = getPathName(false);
        return new URL(VFSUtils.VFS_PROTOCOL, "", -1, parent == null ? pathName : pathName + "/", VFSUtils.VFS_URL_HANDLER);
    }

    /**
     * Get file's URI as a directory.  There will always be a trailing {@code "/"} character.
     *
     * @return the uri
     * @throws URISyntaxException if the URI is somehow malformed
     */
    public URI asDirectoryURI() throws URISyntaxException {
        final String pathName = getPathName(false);
        return new URI(VFSUtils.VFS_PROTOCOL, "", parent == null ? pathName : pathName + "/", null);
    }

    /**
     * Get file's URL as a file.  There will be no trailing {@code "/"} character unless this {@code VirtualFile}
     * represents a root.
     *
     * @return the url
     * @throws MalformedURLException if the URL is somehow malformed
     */
    public URL asFileURL() throws MalformedURLException {
        return new URL(VFSUtils.VFS_PROTOCOL, "", -1, getPathName(false), VFSUtils.VFS_URL_HANDLER);
    }

    /**
     * Get file's URI as a file.  There will be no trailing {@code "/"} character unless this {@code VirtualFile}
     * represents a root.
     *
     * @return the url
     * @throws URISyntaxException if the URI is somehow malformed
     */
    public URI asFileURI() throws URISyntaxException {
        return new URI(VFSUtils.VFS_PROTOCOL, "", getPathName(false), null);
    }

    /**
     * Get the {@link CodeSigner}s for a the virtual file.
     *
     * @return the {@link CodeSigner}s for the virtual file, or {@code null} if not signed
     */
    public CodeSigner[] getCodeSigners() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new VirtualFilePermission(getPathName(), "read"));
        }
        final VFS.Mount mount = VFS.getMount(this);
        return mount.getFileSystem().getCodeSigners(mount.getMountPoint(), this);
    }

    /**
     * Get the {@link Certificate}s for the virtual file.  Simply extracts the certificate entries from
     * the code signers array.
     *
     * @return the certificates for the virtual file, or {@code null} if not signed
     */
    public Certificate[] getCertificates() {
        // getCodeSigners() does the sec check
        final CodeSigner[] codeSigners = getCodeSigners();
        if (codeSigners == null) {
            return null;
        }
        final List<Certificate> certList = new ArrayList<Certificate>(codeSigners.length * 3);
        for (CodeSigner signer : codeSigners) {
            certList.addAll(signer.getSignerCertPath().getCertificates());
        }
        return certList.toArray(new Certificate[certList.size()]);
    }

    /**
     * Get a human-readable (but non-canonical) representation of this virtual file.
     *
     * @return the string
     */
    public String toString() {
        return '"' + getPathName() + '"';
    }

    /**
     * Determine whether the given object is equal to this one.  Returns true if the argument is a {@code VirtualFile}
     * from the same {@code VFS} instance with the same name.
     *
     * @param o the other object
     * @return {@code true} if they are equal
     */
    public boolean equals(Object o) {
        return o instanceof VirtualFile && equals((VirtualFile) o);
    }

    /**
     * Determine whether the given object is equal to this one.  Returns true if the argument is a {@code VirtualFile}
     * from the same {@code VFS} instance with the same name.
     *
     * @param o the other virtual file
     * @return {@code true} if they are equal
     */
    public boolean equals(VirtualFile o) {
        if (o == this) {
            return true;
        }
        if (o == null || hashCode != o.hashCode || !name.equals(o.name)) {
            return false;
        }
        final VirtualFile parent = this.parent;
        final VirtualFile oparent = o.parent;
        return parent != null && parent.equals(oparent) || oparent == null;
    }

    /**
     * Get a hashcode for this virtual file.
     *
     * @return the hash code
     */
    public int hashCode() {
        return hashCode;
    }
}

/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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

import static org.jboss.vfs.VFSMessages.MESSAGES;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.vfs.protocol.FileURLStreamHandler;
import org.jboss.vfs.protocol.VirtualFileURLStreamHandler;
import org.jboss.vfs.spi.MountHandle;
import org.jboss.vfs.util.PaddedManifestStream;
import org.jboss.vfs.util.PathTokenizer;
import org.jboss.vfs.util.automount.Automounter;


/**
 * VFS Utilities
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @version $Revision: 1.1 $
 */
public class VFSUtils {
    /**
     * The default encoding
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Constant representing the URL vfs protocol
     */
    public static final String VFS_PROTOCOL = "vfs";

    /**
     * Constant representing the system property for forcing case sensitive
     */
    public static final String FORCE_CASE_SENSITIVE_KEY = "jboss.vfs.forceCaseSensitive";

    /**
     * The {@link URLStreamHandler} for the 'vfs' protocol
     */
    public static final URLStreamHandler VFS_URL_HANDLER = new VirtualFileURLStreamHandler();

    /**
     * The {@link URLStreamHandler} for the 'file' protocol
     */
    public static final URLStreamHandler FILE_URL_HANDLER = new FileURLStreamHandler();

    /**
     * The default buffer size to use for copies
     */
    public static final int DEFAULT_BUFFER_SIZE = 65536;

    /**
     * This variable indicates if the FileSystem should force case sensitive independently if
     * the underlying file system is case sensitive or not
     */
    private static boolean forceCaseSensitive;

    static {
        forceCaseSensitive = AccessController.doPrivileged(new PrivilegedAction<Boolean> () {
            public Boolean run() {
               String forceString = System.getProperty(VFSUtils.FORCE_CASE_SENSITIVE_KEY, "false");
               return Boolean.valueOf(forceString);
            }
       });
    }

    private VFSUtils() {
    }

    /**
     * Get the paths string for a collection of virtual files
     *
     * @param paths the paths
     * @return the string
     * @throws IllegalArgumentException for null paths
     */
    public static String getPathsString(Collection<VirtualFile> paths) {
        if (paths == null) {
            throw MESSAGES.nullArgument("paths");
        }
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (VirtualFile path : paths) {
            if (path == null) { throw new IllegalArgumentException("Null path in " + paths); }
            if (first == false) {
                buffer.append(':');
            } else {
                first = false;
            }
            buffer.append(path.getPathName());
        }
        if (first == true) {
            buffer.append("<empty>");
        }
        return buffer.toString();
    }

    /**
     * Add manifest paths
     *
     * @param file  the file
     * @param paths the paths to add to
     * @throws IOException              if there is an error reading the manifest or the virtual file is closed
     * @throws IllegalStateException    if the file has no parent
     * @throws IllegalArgumentException for a null file or paths
     */
    public static void addManifestLocations(VirtualFile file, List<VirtualFile> paths) throws IOException {
        if (file == null) {
            throw MESSAGES.nullArgument("file");
        }
        if (paths == null) {
            throw MESSAGES.nullArgument("paths");
        }
        boolean trace = VFSLogger.ROOT_LOGGER.isTraceEnabled();
        Manifest manifest = getManifest(file);
        if (manifest == null) { return; }
        Attributes mainAttributes = manifest.getMainAttributes();
        String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
        if (classPath == null) {
            if (trace) {
                VFSLogger.ROOT_LOGGER.tracef("Manifest has no Class-Path for %s", file.getPathName());
            }
            return;
        }
        VirtualFile parent = file.getParent();
        if (parent == null) {
            VFSLogger.ROOT_LOGGER.debugf("%s has no parent.", file);
            return;
        }
        if (trace) {
            VFSLogger.ROOT_LOGGER.tracef("Parsing Class-Path: %s for %s parent=%s", classPath, file.getName(), parent.getName());
        }
        StringTokenizer tokenizer = new StringTokenizer(classPath);
        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            try {
                VirtualFile vf = parent.getChild(path);
                if (vf.exists()) {
                    if (paths.contains(vf) == false) {
                        paths.add(vf);
                        // Recursively process the jar
                        Automounter.mount(file, vf);
                        addManifestLocations(vf, paths);
                    } else if (trace) {
                        VFSLogger.ROOT_LOGGER.tracef("%s from manifest is already in the classpath %s", vf.getName(), paths);
                    }
                } else if (trace) {
                    VFSLogger.ROOT_LOGGER.trace("Unable to find " + path + " from " + parent.getName());
                }
            } catch (IOException e) {
                VFSLogger.ROOT_LOGGER.debugf("Manifest Class-Path entry %s ignored for %s reason= %s", path, file.getPathName(), e);
            }
        }
    }

    /**
     * Get a manifest from a virtual file, assuming the virtual file is the root of an archive
     *
     * @param archive the root the archive
     * @return the manifest or null if not found
     * @throws IOException              if there is an error reading the manifest or the virtual file is closed
     * @throws IllegalArgumentException for a null archive
     */
    public static Manifest getManifest(VirtualFile archive) throws IOException {
        if (archive == null) {
            throw MESSAGES.nullArgument("archive");
        }
        VirtualFile manifest = archive.getChild(JarFile.MANIFEST_NAME);
        if (manifest == null || !manifest.exists()) {
            if (VFSLogger.ROOT_LOGGER.isTraceEnabled()) {
                VFSLogger.ROOT_LOGGER.tracef("Can't find manifest for %s", archive.getPathName());
            }
            return null;
        }
        return readManifest(manifest);
    }

    /**
     * Read the manifest from given manifest VirtualFile.
     *
     * @param manifest the VF to read from
     * @return JAR's manifest
     * @throws IOException if problems while opening VF stream occur
     */
    public static Manifest readManifest(VirtualFile manifest) throws IOException {
        if (manifest == null) {
            throw MESSAGES.nullArgument("manifest file");
        }
        InputStream stream = new PaddedManifestStream(manifest.openStream());
        try {
            return new Manifest(stream);
        } finally {
            safeClose(stream);
        }
    }

    /**
     * Fix a name (removes any trailing slash)
     *
     * @param name the name to fix
     * @return the fixed name
     * @throws IllegalArgumentException for a null name
     */
    public static String fixName(String name) {
        if (name == null) {
            throw MESSAGES.nullArgument("name");
        }
        int length = name.length();
        if (length <= 1) { return name; }
        if (name.charAt(length - 1) == '/') { return name.substring(0, length - 1); }
        return name;
    }

    /**
     * Decode the path with UTF-8 encoding..
     *
     * @param path the path to decode
     * @return decoded path
     */
    public static String decode(String path) {
        return decode(path, DEFAULT_ENCODING);
    }

    /**
     * Decode the path.
     *
     * @param path     the path to decode
     * @param encoding the encoding
     * @return decoded path
     */
    public static String decode(String path, String encoding) {
        try {
            return URLDecoder.decode(path, encoding);
        } catch (UnsupportedEncodingException e) {
            throw MESSAGES.cannotDecode(path,encoding,e);
        }
    }

    /**
     * Get the name.
     *
     * @param uri the uri
     * @return name from uri's path
     */
    public static String getName(URI uri) {
        if (uri == null) {
            throw MESSAGES.nullArgument("uri");
        }
        String name = uri.getPath();
        if (name != null) {
            // TODO: Not correct for certain uris like jar:...!/
            int lastSlash = name.lastIndexOf('/');
            if (lastSlash > 0) { name = name.substring(lastSlash + 1); }
        }
        return name;
    }


    /**
     * Deal with urls that may include spaces.
     *
     * @param url the url
     * @return uri the uri
     * @throws URISyntaxException for any error
     */
    public static URI toURI(URL url) throws URISyntaxException {
        if (url == null) {
            throw MESSAGES.nullArgument("url");
        }
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            String urispec = url.toExternalForm();
            // Escape percent sign and spaces
            urispec = urispec.replaceAll("%", "%25");
            urispec = urispec.replaceAll(" ", "%20");
            return new URI(urispec);
        }
    }

    /**
     * Ensure the url is convertible to URI by encoding spaces and percent characters if necessary
     *
     * @param url to be sanitized
     * @return sanitized URL
     * @throws URISyntaxException    if URI conversion can't be fixed
     * @throws MalformedURLException if an error occurs
     */
    public static URL sanitizeURL(URL url) throws URISyntaxException, MalformedURLException {
        return toURI(url).toURL();
    }

    /**
     * Copy all the children from the original {@link VirtualFile} the target recursively.
     *
     * @param original the file to copy children from
     * @param target   the file to copy the children to
     * @throws IOException if any problems occur copying the files
     */
    public static void copyChildrenRecursive(VirtualFile original, VirtualFile target) throws IOException {
        if (original == null) {
            throw MESSAGES.nullArgument("Original VirtualFile");
        }
        if (target == null) {
            throw MESSAGES.nullArgument("Target VirtualFile");
        }

        List<VirtualFile> children = original.getChildren();
        for (VirtualFile child : children) {
            VirtualFile targetChild = target.getChild(child.getName());
            File childFile = child.getPhysicalFile();
            if (childFile.isDirectory()) {
                if (!targetChild.getPhysicalFile().mkdir()) {
                    throw MESSAGES.problemCreatingNewDirectory(targetChild);
                }
                copyChildrenRecursive(child, targetChild);
            } else {
                FileInputStream is = new FileInputStream(childFile);
                writeFile(targetChild, is);
            }
        }
    }


    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException {
        copyStreamAndClose(is, os, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is         input stream
     * @param os         output stream
     * @param bufferSize the buffer size to use
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        try {
            copyStream(is, os, bufferSize);
            // throw an exception if the close fails since some data might be lost
            is.close();
            os.close();
        } finally {
            // ...but still guarantee that they're both closed
            safeClose(is);
            safeClose(os);
        }
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is         input stream
     * @param os         output stream
     * @param bufferSize the buffer size to use
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        if (is == null) {
            throw MESSAGES.nullArgument("input stream");
        }
        if (os == null) {
            throw MESSAGES.nullArgument("output stream");
        }
        byte[] buff = new byte[bufferSize];
        int rc;
        while ((rc = is.read(buff)) != -1) { os.write(buff, 0, rc); }
        os.flush();
    }

    /**
     * Write the given bytes to the given virtual file, replacing its current contents (if any) or creating a new file if
     * one does not exist.
     *
     * @param virtualFile the virtual file to write
     * @param bytes       the bytes
     * @throws IOException if an error occurs
     */
    public static void writeFile(VirtualFile virtualFile, byte[] bytes) throws IOException {
        final File file = virtualFile.getPhysicalFile();
        file.getParentFile().mkdirs();
        final FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(bytes);
            fos.close();
        } finally {
            safeClose(fos);
        }
    }

    /**
     * Write the content from the given {@link InputStream} to the given virtual file, replacing its current contents (if any) or creating a new file if
     * one does not exist.
     *
     * @param virtualFile the virtual file to write
     * @param is          the input stream
     * @throws IOException if an error occurs
     */
    public static void writeFile(VirtualFile virtualFile, InputStream is) throws IOException {
        final File file = virtualFile.getPhysicalFile();
        file.getParentFile().mkdirs();
        final FileOutputStream fos = new FileOutputStream(file);
        copyStreamAndClose(is, fos);
    }

    /**
     * Get the virtual URL for a virtual file.  This URL can be used to access the virtual file; however, taking the file
     * part of the URL and attempting to use it with the {@link java.io.File} class may fail if the file is not present
     * on the physical filesystem, and in general should not be attempted.
     * <b>Note:</b> if the given VirtualFile refers to a directory <b>at the time of this
     * method invocation</b>, a trailing slash will be appended to the URL; this means that invoking
     * this method may require a filesystem access, and in addition, may not produce consistent results
     * over time.
     *
     * @param file the virtual file
     * @return the URL
     * @throws MalformedURLException if the file cannot be coerced into a URL for some reason
     * @see VirtualFile#asDirectoryURL()
     * @see VirtualFile#asFileURL()
     */
    public static URL getVirtualURL(VirtualFile file) throws MalformedURLException {
        try {
            final URI uri = getVirtualURI(file);
            final String scheme = uri.getScheme();
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException{
                    if (VFS_PROTOCOL.equals(scheme)) {
                        return new URL(null, uri.toString(), VFS_URL_HANDLER);
                    } else if ("file".equals(scheme)) {
                        return new URL(null, uri.toString(), FILE_URL_HANDLER);
                    } else {
                        return uri.toURL();
                    }
                }
            });
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getException();
        }
    }

    /**
     * Get the virtual URI for a virtual file.
     * <b>Note:</b> if the given VirtualFile refers to a directory <b>at the time of this
     * method invocation</b>, a trailing slash will be appended to the URI; this means that invoking
     * this method may require a filesystem access, and in addition, may not produce consistent results
     * over time.
     *
     * @param file the virtual file
     * @return the URI
     * @throws URISyntaxException if the file cannot be coerced into a URI for some reason
     * @see VirtualFile#asDirectoryURI()
     * @see VirtualFile#asFileURI()
     */
    public static URI getVirtualURI(VirtualFile file) throws URISyntaxException {
        return new URI(VFS_PROTOCOL, "", file.getPathName(true), null);
    }

    /**
     * Get a physical URL for a virtual file.  See the warnings on the {@link VirtualFile#getPhysicalFile()} method
     * before using this method.
     *
     * @param file the virtual file
     * @return the physical file URL
     * @throws IOException if an I/O error occurs getting the physical file
     */
    public static URL getPhysicalURL(VirtualFile file) throws IOException {
        return getPhysicalURI(file).toURL();
    }

    /**
     * Get a physical URI for a virtual file.  See the warnings on the {@link VirtualFile#getPhysicalFile()} method
     * before using this method.
     *
     * @param file the virtual file
     * @return the physical file URL
     * @throws IOException if an I/O error occurs getting the physical file
     */
    public static URI getPhysicalURI(VirtualFile file) throws IOException {
        return file.getPhysicalFile().toURI();
    }

    /**
     * Get the physical root URL of the filesystem of a virtual file.  This URL is suitable for use as a class loader's
     * code source or in similar situations where only standard URL types ({@code jar} and {@code file}) are supported.
     *
     * @param file the virtual file
     * @return the root URL
     * @throws MalformedURLException if the URL is not valid
     */
    public static URL getRootURL(VirtualFile file) throws MalformedURLException {
        final URI uri;
        try {
            uri = getRootURI(file);
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
        return uri.toURL();
    }

    /**
     * Get the physical root URL of the filesystem of a virtual file.  This URI is suitable for conversion to a class loader's
     * code source URL or in similar situations where only standard URL types ({@code jar} and {@code file}) are supported.
     *
     * @param file the virtual file
     * @return the root URI
     * @throws URISyntaxException if the URI is not valid
     */
    public static URI getRootURI(final VirtualFile file) throws URISyntaxException {
        return VFS.getMount(file).getFileSystem().getRootURI();
    }

    /**
     * Safely close some resource without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param c the resource
     */
    public static void safeClose(final Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                VFSLogger.ROOT_LOGGER.trace("Failed to close resource", e);
            }
        }
    }

    /**
     * Safely close some resource without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param closeables the resources
     */
    public static void safeClose(final Closeable... closeables) {
        safeClose(Arrays.asList(closeables));
    }

    /**
     * Safely close some resources without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param ci the resources
     */
    public static void safeClose(final Iterable<? extends Closeable> ci) {
        if (ci != null) {
            for (Closeable closeable : ci) {
                safeClose(closeable);
            }
        }
    }

    /**
     * Safely close some resource without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param zipFile the resource
     */
    public static void safeClose(final ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (Exception e) {
                VFSLogger.ROOT_LOGGER.trace("Failed to close resource", e);
            }
        }
    }

    public static boolean isForceCaseSensitive() {
        return forceCaseSensitive;
    }

    /**
     * In case the file system is not case sensitive we compare the canonical path with
     * the absolute path of the file after normalized.
     * @param file
     * @return
     */
    public static boolean exists(File file) {
        try {
            boolean fileExists = file.exists();
            if(!forceCaseSensitive || !fileExists) {
                return fileExists;
            }

            String absPath = canonicalize(file.getAbsolutePath());
            String canPath = canonicalize(file.getCanonicalPath());
            return fileExists && absPath.equals(canPath);
        } catch(IOException io) {
            return false;
        }
    }

    /**
     * Attempt to recursively delete a real file.
     *
     * @param root the real file to delete
     * @return {@code true} if the file was deleted
     */
    public static boolean recursiveDelete(File root) {
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    ok &= recursiveDelete(file);
                }
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    /**
     * Attempt to recursively delete a virtual file.
     *
     * @param root the virtual file to delete
     * @return {@code true} if the file was deleted
     */
    public static boolean recursiveDelete(VirtualFile root) {
        boolean ok = true;
        if (root.isDirectory()) {
            final List<VirtualFile> files = root.getChildren();
            for (VirtualFile file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    /**
     * Recursively copy a file or directory from one location to another.
     *
     * @param original the original file or directory
     * @param destDir  the destination directory
     * @throws IOException if an I/O error occurs before the copy is complete
     */
    public static void recursiveCopy(File original, File destDir) throws IOException {
        final String name = original.getName();
        final File destFile = new File(destDir, name);
        if (original.isDirectory()) {
            destFile.mkdir();
            for (File file : original.listFiles()) {
                recursiveCopy(file, destFile);
            }
        } else {
            final OutputStream os = new FileOutputStream(destFile);
            try {
                final InputStream is = new FileInputStream(original);
                copyStreamAndClose(is, os);
            } finally {
                // in case the input stream open fails
                safeClose(os);
            }
        }
    }

    /**
     * Recursively copy a file or directory from one location to another.
     *
     * @param original the original file or directory
     * @param destDir  the destination directory
     * @throws IOException if an I/O error occurs before the copy is complete
     */
    public static void recursiveCopy(File original, VirtualFile destDir) throws IOException {
        final String name = original.getName();
        final File destFile = destDir.getChild(name).getPhysicalFile();
        if (original.isDirectory()) {
            destFile.mkdir();
            for (File file : original.listFiles()) {
                recursiveCopy(file, destFile);
            }
        } else {
            final OutputStream os = new FileOutputStream(destFile);
            try {
                final InputStream is = new FileInputStream(original);
                copyStreamAndClose(is, os);
            } finally {
                // in case the input stream open fails
                safeClose(os);
            }
        }
    }

    /**
     * Recursively copy a file or directory from one location to another.
     *
     * @param original the original virtual file or directory
     * @param destDir  the destination directory
     * @throws IOException if an I/O error occurs before the copy is complete
     */
    public static void recursiveCopy(VirtualFile original, File destDir) throws IOException {
        final String name = original.getName();
        final File destFile = new File(destDir, name);
        if (original.isDirectory()) {
            destFile.mkdir();
            for (VirtualFile file : original.getChildren()) {
                recursiveCopy(file, destFile);
            }
        } else {
            final OutputStream os = new FileOutputStream(destFile);
            try {
                final InputStream is = original.openStream();
                copyStreamAndClose(is, os);
            } finally {
                // in case the input stream open fails
                safeClose(os);
            }
        }
    }

    /**
     * Recursively copy a file or directory from one location to another.
     *
     * @param original the original virtual file or directory
     * @param destDir  the destination virtual directory
     * @throws IOException if an I/O error occurs before the copy is complete
     */
    public static void recursiveCopy(VirtualFile original, VirtualFile destDir) throws IOException {
        final String name = original.getName();
        final File destFile = destDir.getChild(name).getPhysicalFile();
        if (original.isDirectory()) {
            destFile.mkdir();
            for (VirtualFile file : original.getChildren()) {
                recursiveCopy(file, destFile);
            }
        } else {
            final OutputStream os = new FileOutputStream(destFile);
            try {
                final InputStream is = original.openStream();
                copyStreamAndClose(is, os);
            } finally {
                // in case the input stream open fails
                safeClose(os);
            }
        }
    }

    private static final InputStream EMPTY_STREAM = new InputStream() {
        public int read() throws IOException {
            return -1;
        }
    };

    /**
     * Get the empty input stream.  This stream always reports an immediate EOF.
     *
     * @return the empty input stream
     */
    public static InputStream emptyStream() {
        return EMPTY_STREAM;
    }


    /**
     * Get an input stream that will always be consumable as a Zip/Jar file.  The input stream will not be an instance
     * of a JarInputStream, but will stream bytes according to the Zip specification.  Using this method, a VFS file
     * or directory can be written to disk as a normal jar/zip file.
     *
     * @param virtualFile The virtual to get a jar file input stream for
     * @return An input stream returning bytes according to the zip spec
     * @throws IOException if any problems occur
     */
    public static InputStream createJarFileInputStream(final VirtualFile virtualFile) throws IOException {
        if (virtualFile.isDirectory()) {
            final VirtualJarInputStream jarInputStream = new VirtualJarInputStream(virtualFile);
            return new VirtualJarFileInputStream(jarInputStream);
        }
        InputStream inputStream = null;
        try {
            final byte[] expectedHeader = new byte[4];

            expectedHeader[0] = (byte) (JarEntry.LOCSIG & 0xff);
            expectedHeader[1] = (byte) ((JarEntry.LOCSIG >>> 8) & 0xff);
            expectedHeader[2] = (byte) ((JarEntry.LOCSIG >>> 16) & 0xff);
            expectedHeader[3] = (byte) ((JarEntry.LOCSIG >>> 24) & 0xff);

            inputStream = virtualFile.openStream();
            final byte[] bytes = new byte[4];
            final int read = inputStream.read(bytes, 0, 4);
            if (read < 4 || !Arrays.equals(expectedHeader, bytes)) {
                throw MESSAGES.invalidJarSignature(Arrays.toString(bytes), Arrays.toString(expectedHeader));
            }
        } finally {
            safeClose(inputStream);
        }
        return virtualFile.openStream();
    }

    /**
     * Expand a zip file to a destination directory.  The directory must exist.  If an error occurs, the destination
     * directory may contain a partially-extracted archive, so cleanup is up to the caller.
     *
     * @param zipFile the zip file
     * @param destDir the destination directory
     * @throws IOException if an error occurs
     */
    public static void unzip(File zipFile, File destDir) throws IOException {
        final ZipFile zip = new ZipFile(zipFile);
        try {
            final Set<File> createdDirs = new HashSet<File>();
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            FILES_LOOP:
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                final String name = zipEntry.getName();
                final List<String> tokens = PathTokenizer.getTokens(name);
                final Iterator<String> it = tokens.iterator();
                File current = destDir;
                while (it.hasNext()) {
                    String token = it.next();
                    if (PathTokenizer.isCurrentToken(token) || PathTokenizer.isReverseToken(token)) {
                        // invalid file; skip it!
                        continue FILES_LOOP;
                    }
                    current = new File(current, token);
                    if ((it.hasNext() || zipEntry.isDirectory()) && createdDirs.add(current)) {
                        current.mkdir();
                    }
                }
                if (!zipEntry.isDirectory()) {
                    final InputStream is = zip.getInputStream(zipEntry);
                    try {
                        final FileOutputStream os = new FileOutputStream(current);
                        try {
                            VFSUtils.copyStream(is, os);
                            // allow an error on close to terminate the unzip
                            is.close();
                            os.close();
                        } finally {
                            VFSUtils.safeClose(os);
                        }
                    } finally {
                        VFSUtils.safeClose(is);
                    }
                    // exclude jsp files last modified time change. jasper jsp compiler Compiler.java depends on last modified time-stamp to re-compile jsp files
                    if (!current.getName().endsWith(".jsp"))
                        current.setLastModified(zipEntry.getTime());
                }
            }
        } finally {
            VFSUtils.safeClose(zip);
        }
    }

    /**
     * Return the mount source File for a given mount handle.
     *
     * @param handle The handle to get the source for
     * @return The mount source file or null if the handle does not have a source, or is not a MountHandle
     */
    public static File getMountSource(Closeable handle) {
        if (handle instanceof MountHandle) { return MountHandle.class.cast(handle).getMountSource(); }
        return null;
    }

    private static final Pattern GLOB_PATTERN = Pattern.compile("(\\*\\*?)|(\\?)|(\\\\.)|(/+)|([^*?]+)");

    /**
     * Get a regular expression pattern which matches any path names which match the given glob.  The glob patterns
     * function similarly to {@code ant} file patterns.  Valid meta-characters in the glob pattern include:
     * <ul>
     * <li><code>"\"</code> - escape the next character (treat it literally, even if it is itself a recognized meta-character)</li>
     * <li><code>"?"</code> - match any non-slash character</li>
     * <li><code>"*"</code> - match zero or more non-slash characters</li>
     * <li><code>"**"</code> - match zero or more characters, including slashes</li>
     * <li><code>"/"</code> - match one or more slash characters.  Consecutive {@code /} characters are collapsed down into one.</li>
     * </ul>
     * In addition, like {@code ant}, if the pattern ends with a {@code /}, then an implicit <code>"**"</code> will be appended.
     * <p/>
     * <b>See also:</b> <a href="http://ant.apache.org/manual/dirtasks.html#patterns">"Patterns" in the Ant Manual</a>
     *
     * @param glob the glob to match
     * @return the pattern
     */
    public static Pattern getGlobPattern(final String glob) {
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("^");
        final Matcher m = GLOB_PATTERN.matcher(glob);
        boolean lastWasSlash = false;
        while (m.find()) {
            lastWasSlash = false;
            String grp;
            if ((grp = m.group(1)) != null) {
                // match a * or **
                if (grp.length() == 2) {
                    // it's a **
                    patternBuilder.append(".*");
                } else {
                    // it's a *
                    patternBuilder.append("[^/]*");
                }
            } else if ((grp = m.group(2)) != null) {
                // match a '?' glob pattern; any non-slash character
                patternBuilder.append("[^/]");
            } else if ((grp = m.group(3)) != null) {
                // backslash-escaped value
                patternBuilder.append(grp.charAt(1));
            } else if ((grp = m.group(4)) != null) {
                // match any number of / chars
                patternBuilder.append("/+");
                lastWasSlash = true;
            } else {
                // some other string
                patternBuilder.append(Pattern.quote(m.group()));
            }
        }
        if (lastWasSlash) {
            // ends in /, append **
            patternBuilder.append(".*");
        }
        patternBuilder.append("$");
        return Pattern.compile(patternBuilder.toString());
    }

    /**
     * Canonicalize the given path.  Removes all {@code .} and {@code ..} segments from the path.
     *
     * @param path the relative or absolute possibly non-canonical path
     * @return the canonical path
     */
    @SuppressWarnings("UnusedLabel") // for documentation
    public static String canonicalize(final String path) {
        final int length = path.length();
        // 0 - start
        // 1 - got one .
        // 2 - got two .
        // 3 - got /
        int state = 0;
        if (length == 0) {
            return path;
        }
        final char[] targetBuf = new char[length];
        // string segment end exclusive
        int e = length;
        // string cursor position
        int i = length;
        // buffer cursor position
        int a = length - 1;
        // number of segments to skip
        int skip = 0;
        loop:
        while (--i >= 0) {
            char c = path.charAt(i);
            outer:
            switch (c) {
                case '/': {
                    inner:
                    switch (state) {
                        case 0:
                            state = 3;
                            e = i;
                            break outer;
                        case 1:
                            state = 3;
                            e = i;
                            break outer;
                        case 2:
                            state = 3;
                            e = i;
                            skip++;
                            break outer;
                        case 3:
                            e = i;
                            break outer;
                        default:
                            throw new IllegalStateException();
                    }
                    // not reached!
                }
                case '.': {
                    inner:
                    switch (state) {
                        case 0:
                            state = 1;
                            break outer;
                        case 1:
                            state = 2;
                            break outer;
                        case 2:
                            break inner; // emit!
                        case 3:
                            state = 1;
                            break outer;
                        default:
                            throw new IllegalStateException();
                    }
                    // fall thru
                }
                default: {
                    final int newE = e > 0 ? path.lastIndexOf('/', e - 1) : -1;
                    final int segmentLength = e - newE - 1;
                    if (skip > 0) {
                        skip--;
                    } else {
                        if (state == 3) {
                            targetBuf[a--] = '/';
                        }
                        path.getChars(newE + 1, e, targetBuf, (a -= segmentLength) + 1);
                    }
                    state = 0;
                    i = newE + 1;
                    e = newE;
                    break;
                }
            }
        }
        if (state == 3) {
            targetBuf[a--] = '/';
        }
        return new String(targetBuf, a + 1, length - a - 1);
    }
}

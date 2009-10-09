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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;
import org.jboss.util.collection.CollectionsFactory;

/**
 * VFS Utilities
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class VFSUtils {

    /**
     * The log
     */
    private static final Logger log = Logger.getLogger(VFSUtils.class);

    /**
     * The default encoding
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Constant representing the URL file protocol
     */
    public static final String FILE_PROTOCOL = "file";

    /**
     * Standard separator for JAR URL
     */
    public static final String JAR_URL_SEPARATOR = "!/";

    /**
     * The default buffer size to use for copies
     */
    public static final int DEFAULT_BUFFER_SIZE = 65536;

    private VFSUtils() {
    }

    /**
     * Get the paths string for a collection of virtual files
     *
     * @param paths the paths
     *
     * @return the string
     *
     * @throws IllegalArgumentException for null paths
     */
    public static String getPathsString(Collection<VirtualFile> paths) {
        if (paths == null)
            throw new IllegalArgumentException("Null paths");
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (VirtualFile path : paths) {
            if (path == null)
                throw new IllegalArgumentException("Null path in " + paths);
            if (first == false)
                buffer.append(':');
            else
                first = false;
            buffer.append(path.getPathName());
        }
        if (first == true)
            buffer.append("<empty>");
        return buffer.toString();
    }

    /**
     * Add manifest paths
     *
     * @param file the file
     * @param paths the paths to add to
     *
     * @throws IOException if there is an error reading the manifest or the virtual file is closed
     * @throws IllegalStateException if the file has no parent
     * @throws IllegalArgumentException for a null file or paths
     */
    public static void addManifestLocations(VirtualFile file, List<VirtualFile> paths) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("Null file");
        if (paths == null)
            throw new IllegalArgumentException("Null paths");
        boolean trace = log.isTraceEnabled();
        Manifest manifest = getManifest(file);
        if (manifest == null)
            return;
        Attributes mainAttributes = manifest.getMainAttributes();
        String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
        if (classPath == null) {
            if (trace)
                log.trace("Manifest has no Class-Path for " + file.getPathName());
            return;
        }
        VirtualFile parent = file.getParent();
        if (parent == null) {
            log.debug(file + " has no parent.");
            return;
        }
        if (trace)
            log.trace("Parsing Class-Path: " + classPath + " for " + file.getName() + " parent=" + parent.getName());
        StringTokenizer tokenizer = new StringTokenizer(classPath);
        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            try {
                VirtualFile vf = parent.getChild(path);
                if (vf != null) {
                    if (paths.contains(vf) == false) {
                        paths.add(vf);
                        // Recursively process the jar
                        addManifestLocations(vf, paths);
                    } else if (trace)
                        log.trace(vf.getName() + " from manifiest is already in the classpath " + paths);
                } else if (trace)
                    log.trace("Unable to find " + path + " from " + parent.getName());
            }
            catch (IOException e) {
                log.debug("Manifest Class-Path entry " + path + " ignored for " + file.getPathName() + " reason=" + e);
            }
        }
    }

    /**
     * Get a manifest from a virtual file, assuming the virtual file is the root of an archive
     *
     * @param archive the root the archive
     *
     * @return the manifest or null if not found
     *
     * @throws IOException if there is an error reading the manifest or the virtual file is closed
     * @throws IllegalArgumentException for a null archive
     */
    public static Manifest getManifest(VirtualFile archive) throws IOException {
        if (archive == null)
            throw new IllegalArgumentException("Null archive");
        VirtualFile manifest = archive.getChild(JarFile.MANIFEST_NAME);
        if (manifest == null) {
            if (log.isTraceEnabled())
                log.trace("Can't find manifest for " + archive.getPathName());
            return null;
        }
        return readManifest(manifest);
    }

    /**
     * Read the manifest from given manifest VirtualFile.
     *
     * @param manifest the VF to read from
     *
     * @return JAR's manifest
     *
     * @throws IOException if problems while opening VF stream occur
     */
    public static Manifest readManifest(VirtualFile manifest) throws IOException {
        if (manifest == null)
            throw new IllegalArgumentException("Null manifest file");
        InputStream stream = manifest.openStream();
        try {
            return new Manifest(stream);
        }
        finally {
            safeClose(stream);
        }
    }

    /**
     * Fix a name (removes any trailing slash)
     *
     * @param name the name to fix
     *
     * @return the fixed name
     *
     * @throws IllegalArgumentException for a null name
     */
    public static String fixName(String name) {
        if (name == null)
            throw new IllegalArgumentException("Null name");
        int length = name.length();
        if (length <= 1)
            return name;
        if (name.charAt(length - 1) == '/')
            return name.substring(0, length - 1);
        return name;
    }

    /**
     * Decode the path with UTF-8 encoding..
     *
     * @param path the path to decode
     *
     * @return decoded path
     */
    public static String decode(String path) {
        return decode(path, DEFAULT_ENCODING);
    }

    /**
     * Decode the path.
     *
     * @param path the path to decode
     * @param encoding the encodeing
     *
     * @return decoded path
     */
    public static String decode(String path, String encoding) {
        try {
            return URLDecoder.decode(path, encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot decode: " + path + " [" + encoding + "]", e);
        }
    }

    /**
     * Get the name.
     *
     * @param uri the uri
     *
     * @return name from uri's path
     */
    public static String getName(URI uri) {
        if (uri == null)
            throw new IllegalArgumentException("Null uri");
        String name = uri.getPath();
        if (name != null) {
            // TODO: Not correct for certain uris like jar:...!/
            int lastSlash = name.lastIndexOf('/');
            if (lastSlash > 0)
                name = name.substring(lastSlash + 1);
        }
        return name;
    }

    /**
     * Take a URL.getQuery string and parse it into name=value pairs
     *
     * @param query Possibly empty/null url query string
     *
     * @return String[] for the name/value pairs in the query. May be empty but never null.
     */
    public static Map<String, String> parseURLQuery(String query) {
        Map<String, String> pairsMap = CollectionsFactory.createLazyMap();
        if (query != null) {
            StringTokenizer tokenizer = new StringTokenizer(query, "=&");
            while (tokenizer.hasMoreTokens()) {
                String name = tokenizer.nextToken();
                String value = tokenizer.nextToken();
                pairsMap.put(name, value);
            }
        }
        return pairsMap;
    }

    /**
     * Deal with urls that may include spaces.
     *
     * @param url the url
     *
     * @return uri the uri
     *
     * @throws URISyntaxException for any error
     */
    public static URI toURI(URL url) throws URISyntaxException {
        if (url == null)
            throw new IllegalArgumentException("Null url");
        try {
            return url.toURI();
        }
        catch (URISyntaxException e) {
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
     *
     * @return sanitized URL
     *
     * @throws URISyntaxException if URI conversion can't be fixed
     * @throws MalformedURLException if an error occurs
     */
    public static URL sanitizeURL(URL url) throws URISyntaxException, MalformedURLException {
        return toURI(url).toURL();
    }

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     *
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException {
        copyStreamAndClose(is, os, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     * @param bufferSize the buffer size to use
     *
     * @throws IOException for any error
     */
    public static void copyStreamAndClose(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        try {
            copyStream(is, os, bufferSize);
            // throw an exception if the close fails since some data might be lost
            is.close();
            os.close();
        }
        finally {
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
     *
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     * @param bufferSize the buffer size to use
     *
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        if (is == null)
            throw new IllegalArgumentException("input stream is null");
        if (os == null)
            throw new IllegalArgumentException("output stream is null");
        byte[] buff = new byte[bufferSize];
        int rc;
        while ((rc = is.read(buff)) != -1) os.write(buff, 0, rc);
        os.flush();
    }

    /**
     * Write the given bytes to the given virtual file, replacing its current contents (if any) or creating a new file if
     * one does not exist.
     *
     * @param virtualFile the virtual file to write
     * @param bytes the bytes
     *
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
     * Get the virtual URL for a virtual file.  This URL can be used to access the virtual file; however, taking the file
     * part of the URL and attempting to use it with the {@link java.io.File} class may fail if the file is not present
     * on the physical filesystem, and in general should not be attempted.
     *
     * @param file the virtual file
     *
     * @return the URL
     *
     * @throws MalformedURLException if the file cannot be coerced into a URL for some reason
     */
    public static URL getVirtualURL(VirtualFile file) throws MalformedURLException {
        // todo: specify the URL handler directly as a minor optimization
        return new URL("file", "", -1, file.getPathName(true));
    }

    /**
     * Get the virtual URI for a virtual file.
     *
     * @param file the virtual file
     *
     * @return the URI
     *
     * @throws URISyntaxException if the file cannot be coerced into a URI for some reason
     */
    public static URI getVirtualURI(VirtualFile file) throws URISyntaxException {
        return new URI("file", "", file.getPathName(true), null);
    }

    /**
     * Get a physical URL for a virtual file.  See the warnings on the {@link VirtualFile#getPhysicalFile()} method
     * before using this method.
     *
     * @param file the virtual file
     *
     * @return the physical file URL
     *
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
     *
     * @return the physical file URL
     *
     * @throws IOException if an I/O error occurs getting the physical file
     */
    public static URI getPhysicalURI(VirtualFile file) throws IOException {
        return file.getPhysicalFile().toURI();
    }

    /**
     * Safely close some resource without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param c the resource
     */
    public static void safeClose(final Closeable c) {
        if (c != null) try {
            c.close();
        }
        catch (Exception e) {
            log.trace("Failed to close resource", e);
        }
    }

    /**
     * Safely close some resources without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param ci the resources
     */
    public static void safeClose(final Iterable<? extends Closeable> ci) {
        if (ci != null) for (Closeable closeable : ci) {
            safeClose(closeable);
        }
    }

    /**
     * Safely close some resource without throwing an exception.  Any exception will be logged at TRACE level.
     *
     * @param zipFile the resource
     */
    public static void safeClose(final ZipFile zipFile) {
        if (zipFile != null) try {
            zipFile.close();
        }
        catch (Exception e) {
            log.trace("Failed to close resource", e);
        }
    }

    /**
     * Attempt to recursively delete a real file.
     *
     * @param root the real file to delete
     *
     * @return {@code true} if the file was deleted
     */
    public static boolean recursiveDelete(File root) {
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
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
     *
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
     * @param destDir the destination directory
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
     * @param destDir the destination directory
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
     * @param destDir the destination directory
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
     * @param destDir the destination virtual directory
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
     * Determine the relative path within the assembly.
     * 
     * @param mountPoint
     * @param target
     * @return
     */
    public static List<String> getRelativePath(VirtualFile mountPoint, VirtualFile target) {
       List<String> pathParts = new LinkedList<String>();
       collectPathParts(mountPoint, target, pathParts);
       return pathParts;
    }

    /**
     * Recursively work from the target to the mount-point and collect the path elements.
     * 
     * @param mountPoint
     * @param current
     * @param pathParts
     */
    private static void collectPathParts(VirtualFile mountPoint, VirtualFile current, List<String> pathParts) {
       if (current == null) {
          throw new IllegalArgumentException("VirtualFile not a child of provided mount point");
       }
       if (current.equals(mountPoint)) {
          return;
       }
       collectPathParts(mountPoint, current.getParent(), pathParts);
       pathParts.add(current.getName());
    }
}

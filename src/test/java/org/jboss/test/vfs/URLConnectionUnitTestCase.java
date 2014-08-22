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
package org.jboss.test.vfs;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.concurrent.Executors;

import junit.framework.Test;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.protocol.FileURLConnection;
import org.jboss.vfs.protocol.VfsUrlStreamHandlerFactory;

/**
 * Basic tests of URL connection
 *
 * @author ales.jutin@jboss.org
 */
public class URLConnectionUnitTestCase extends AbstractVFSTest {
    static {
        URL.setURLStreamHandlerFactory(new VfsUrlStreamHandlerFactory());
    }

    public URLConnectionUnitTestCase(String name) {
        super(name);
    }

    public static Test suite() {
        return suite(URLConnectionUnitTestCase.class);
    }

    protected String getFileName() {
        return "outer.jar";
    }

    protected VirtualFile getFile() throws Exception {
        VirtualFile root = getVirtualFile("/vfs/test/");
        VirtualFile file = root.getChild(getFileName());
        assertNotNull(file);
        return file;
    }

    protected URL getURLAndAssertProtocol(VirtualFile file) throws Exception {
        URL url = file.toURL();
        assertEquals(VFSUtils.VFS_PROTOCOL, url.getProtocol());
        return url;
    }

    /**
     * Test url connection content.
     *
     * @throws Exception for any error
     */
    public void testContent() throws Exception {
        VirtualFile file = getFile();
        URL url = getURLAndAssertProtocol(file);
        URLConnection conn = url.openConnection();
        assertEquals(file, conn.getContent());
    }

    /**
     * Test url connection content lenght.
     *
     * @throws Exception for any error
     */
    public void testContentLenght() throws Exception {
        VirtualFile file = getFile();
        URL url = getURLAndAssertProtocol(file);
        URLConnection conn = url.openConnection();
        assertEquals(file.getSize(), conn.getContentLength());
    }

    /**
     * Test url connection last modified.
     *
     * @throws Exception for any error
     */
    public void testLastModified() throws Exception {
        VirtualFile file = getFile();
        URL url = getURLAndAssertProtocol(file);
        URLConnection conn = url.openConnection();
        assertEquals(file.getLastModified(), conn.getLastModified());
    }

    /**
     * Test url connection input stream.
     *
     * @throws Exception for any error
     */
    public void testInputStream() throws Exception {
        VirtualFile file = getFile();
        URL url = getURLAndAssertProtocol(file);
        URLConnection conn = url.openConnection();
        assertTrue(Arrays.equals(readBytes(file.openStream()), readBytes(conn.getInputStream())));
    }

    public void testPathWithSpaces() throws Exception {
        VirtualFile root = getVirtualFile("/vfs/test/");
        VirtualFile file = root.getChild("path with spaces/spaces.ear");
        File real = file.getPhysicalFile();
        assertTrue(real.exists());
        URL url = getURLAndAssertProtocol(file);
        URLConnection conn = url.openConnection();
        assertTrue(Arrays.equals(readBytes(conn.getInputStream()), readBytes(file.openStream())));
    }

    public void testTempPath() throws Exception {
        File temp = File.createTempFile("123", ".tmp");
        temp.deleteOnExit();
        VirtualFile file = VFS.getChild(temp.toURI());
        assertTrue(file.exists());
        URL url = getURLAndAssertProtocol(file);
        URLConnection conn = url.openConnection();
        assertEquals(file.getLastModified(), conn.getLastModified());
    }

    public void testVfsUrlContentType() throws Exception {
        URL url = getResource("/vfs/test/test-web.xml");
        VirtualFile xml = VFS.getChild(url);
        URLConnection conn = xml.toURL().openConnection();
        String contentType = conn.getContentType();
        assertNotNull(contentType);
        assertEquals("application/xml", contentType);
    }

    public void testOutsideUrl() throws Exception {
        URL url = getResource("/vfs/test/outer.jar");
        File file = new File(url.toURI());

        url = new URL(VFSUtils.VFS_PROTOCOL, url.getHost(), url.getPort(), url.getFile());

        URLConnection conn = url.openConnection();
        assertEquals(file.lastModified(), conn.getLastModified());
    }

    public void testFileUrl() throws Exception {
        // Hack to ensure VFS.init has been called and has taken over the file: protocol
        VFS.getChild("");
        URL resourceUrl = getResource("/vfs/test/outer.jar");
        // Hack to ensure the URL handler is not passed down by the parent URL context
        URL url = new URL("file", resourceUrl.getHost(), resourceUrl.getFile());

        // Make sure we are using our handler
        URLConnection urlConn = url.openConnection();
        assertTrue(urlConn instanceof FileURLConnection);

        File file = new File(url.toURI());
        assertNotNull(file);

        VirtualFile vf = VFS.getChild(url);
        assertTrue(vf.isFile());
        // Mount a temp dir over the jar location in VFS
        TempFileProvider provider = null;
        Closeable handle = null;
        try {
            provider = TempFileProvider.create("temp", Executors.newSingleThreadScheduledExecutor());
            handle = VFS.mountTemp(vf, provider);
            assertTrue(vf.isDirectory());

            File vfsDerivedFile = vf.getPhysicalFile();
            File urlDerivedFile = (File) url.getContent();
            // Make sure the file returned by the file: URL is not the VFS File (In other words, make sure it does not use the mounts)
            assertTrue(urlDerivedFile.isFile());
            assertFalse(vfsDerivedFile.equals(urlDerivedFile));
        } finally {
            VFSUtils.safeClose(handle, provider);
        }
    }

    public void testFileUrlContentType() throws Exception {
        VFS.getChild("");
        URL url = getResource("/vfs/test/test-web.xml");
        url = new URL("file", url.getHost(), url.getFile());

        URLConnection conn = url.openConnection();
        String contentType = conn.getContentType();
        assertNotNull(contentType);
        assertEquals("application/xml", contentType);
    }

    public void testHeaderFields() throws Exception {
        VFS.getChild("");
        URL url = getResource("/vfs/test/test-web.xml");
        url = new URL("file", url.getHost(), url.getFile());

        URLConnection conn = url.openConnection();
        int contentLength = conn.getContentLength();
        assertTrue(contentLength > 0);
        String contentLengthHeader = conn.getHeaderField("content-length");
        assertEquals(String.valueOf(contentLength), contentLengthHeader);

        assertTrue(conn.getLastModified() > 0);
        String lastModifiedHeader = conn.getHeaderField("last-modified");
        assertNotNull(lastModifiedHeader);
    }

    protected static byte[] readBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        byte[] bytes = new byte[1024];
        try {
            while (read >= 0) {
                read = inputStream.read(bytes);
                baos.write(bytes);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
        return baos.toByteArray();
    }
}

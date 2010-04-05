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
package org.jboss.test.vfs;

import org.jboss.vfs.*;
import org.jboss.vfs.VirtualJarFileInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public class VirtualJarFileInputStreamTest extends AbstractVFSTest {

    public VirtualJarFileInputStreamTest(final String name) {
        super(name);
    }


    public void testDirectoryStream() throws Exception {
        VirtualFile testDir = getVirtualFile("/vfs/test/jar1");

        InputStream inputStream = VFSUtils.createJarFileInputStream(testDir);

        TempDir tempDir = provider.createTempDir("test");

        File tempFile = tempDir.getFile("test.zip");

        VFSUtils.copyStreamAndClose(inputStream, new FileOutputStream(tempFile));

        JarFile jarFile = new JarFile(tempFile);

        assertEntryContent("META-INF/MANIFEST.MF", jarFile, testDir);
        assertEntry("org/", jarFile);
        assertEntry("org/jboss/", jarFile);
        assertEntry("org/jboss/test/", jarFile);
        assertEntry("org/jboss/test/vfs/", jarFile);
        assertEntry("org/jboss/test/vfs/support/", jarFile);
        assertEntry("org/jboss/test/vfs/support/jar1/", jarFile);
        assertEntryContent("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class", jarFile, testDir);
        assertEntryContent("org/jboss/test/vfs/support/jar1/ClassInJar1.class", jarFile, testDir);
    }

    public void testFileStream() throws Exception {
        VirtualFile testJar = getVirtualFile("/vfs/test/jar1.jar");

        InputStream inputStream = VFSUtils.createJarFileInputStream(testJar);

        TempDir tempDir = provider.createTempDir("test");

        File tempFile = tempDir.getFile("test.zip");

        VFSUtils.copyStreamAndClose(inputStream, new FileOutputStream(tempFile));

        JarFile jarFile = new JarFile(tempFile);

        assertEntry("META-INF/MANIFEST.MF", jarFile);
        assertEntry("org/", jarFile);
        assertEntry("org/jboss/", jarFile);
        assertEntry("org/jboss/test/", jarFile);
        assertEntry("org/jboss/test/vfs/", jarFile);
        assertEntry("org/jboss/test/vfs/support/", jarFile);
        assertEntry("org/jboss/test/vfs/support/jar1/", jarFile);
        assertEntry("org/jboss/test/vfs/support/jar1/ClassInJar1$InnerClass.class", jarFile);
        assertEntry("org/jboss/test/vfs/support/jar1/ClassInJar1.class", jarFile);
    }

    public void testInvalidFileStream() throws Exception {
        VirtualFile testJar = getVirtualFile("/vfs/test/filesonly.mf");
        try {
            VFSUtils.createJarFileInputStream(testJar);
            fail("Should have thrown IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().startsWith("Invalid jar signature"));
        }

    }

    private void assertEntry(String name, JarFile jarFile) throws Exception {
        JarEntry entry = jarFile.getJarEntry(name);
        assertNotNull(entry);
    }

    private void assertEntryContent(String name, JarFile jarFile, VirtualFile parent) throws Exception {
        JarEntry entry = jarFile.getJarEntry(name);
        assertNotNull(entry);
        InputStream entryStream = jarFile.getInputStream(entry);
        InputStream fileStream = parent.getChild(name).openStream();
        assertContentEqual(fileStream, entryStream);
    }

    private void assertContentEqual(InputStream in, InputStream other) throws Exception {
        assertArrayEquals(getContent(in), getContent(other));
    }
}

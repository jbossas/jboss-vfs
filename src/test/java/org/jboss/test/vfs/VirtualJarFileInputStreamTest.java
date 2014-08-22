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
package org.jboss.test.vfs;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.vfs.TempDir;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

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
            assertTrue(expected.getMessage().contains("Invalid jar signature"));
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

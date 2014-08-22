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
package org.jboss.vfs.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.jboss.vfs.VFS;

import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

/**
 * @author ehsavoie
 */
public class PaddedManifestStreamTest {

    public PaddedManifestStreamTest() {
    }

    /**
     * Test of read method, of class PaddedManifestStream.
     */
    @Test
    public void testReadUnpadded() throws Exception {
        PaddedManifestStream input = new PaddedManifestStream(new ByteArrayInputStream("HelloWorld !".getBytes("UTF-8")));
        ByteArrayOutputStream output = new ByteArrayOutputStream(15);
        byte[] result = new byte[0];
        try {
            int c = 0;
            while ((c = input.read()) != -1) {
                output.write(c);
            }
            result = output.toByteArray();
        } finally {
            VFSUtils.safeClose(input);
            VFSUtils.safeClose(output);
        }
        assertEquals("HelloWorld !\n", new String(result, "UTF-8"));
    }

    /**
     * Test of read method, of class PaddedManifestStream.
     */
    @Test
    public void testReadPadded() throws Exception {
        PaddedManifestStream input = new PaddedManifestStream(new ByteArrayInputStream("HelloWorld !\n".getBytes("UTF-8")));
        ByteArrayOutputStream output = new ByteArrayOutputStream(15);
        byte[] result = new byte[0];
        try {
            int c = 0;
            while ((c = input.read()) != -1) {
                output.write(c);
            }
            result = output.toByteArray();
        } finally {
            VFSUtils.safeClose(input);
            VFSUtils.safeClose(output);
        }
        assertEquals("HelloWorld !\n", new String(result, "UTF-8"));
    }

    @Test
    public void testReadUnpaddedPadded() throws Exception {
        PaddedManifestStream input = new PaddedManifestStream(new ByteArrayInputStream("HelloWorld !".getBytes("UTF-8")));
        ByteArrayOutputStream output = new ByteArrayOutputStream(15);
        byte[] result = new byte[0];
        try {
            int c = 0;
            byte[] buffer = new byte[8];
            while ((c = input.read(buffer)) != -1) {
                output.write(buffer, 0, c);
            }
            result = output.toByteArray();
        } finally {
            VFSUtils.safeClose(input);
            VFSUtils.safeClose(output);
        }
        assertEquals("HelloWorld !\n", new String(result, "UTF-8"));
        input = new PaddedManifestStream(new ByteArrayInputStream("HelloWorld !".getBytes("UTF-8")));
        output = new ByteArrayOutputStream(15);
        try {
            int c = 8;
            byte[] buffer = new byte[8];
            while ((c = input.read(buffer, 0, c)) != -1) {
                output.write(buffer, 0, c);
            }
            result = output.toByteArray();
        } finally {
            VFSUtils.safeClose(input);
            VFSUtils.safeClose(output);
        }
        assertEquals("HelloWorld !\n", new String(result, "UTF-8"));
    }

    @Test
    public void testReadPaddedBulk() throws Exception {
        PaddedManifestStream input = new PaddedManifestStream(new ByteArrayInputStream("HelloWorld !\n".getBytes("UTF-8")));
        ByteArrayOutputStream output = new ByteArrayOutputStream(15);
        byte[] result = new byte[0];
        try {
            int c = 0;
            byte[] buffer = new byte[8];
            while ((c = input.read(buffer)) != -1) {
                output.write(buffer, 0, c);
            }
            result = output.toByteArray();
        } finally {
            VFSUtils.safeClose(input);
            VFSUtils.safeClose(output);
        }
        assertEquals("HelloWorld !\n", new String(result, "UTF-8"));
        input = new PaddedManifestStream(new ByteArrayInputStream("HelloWorld !".getBytes("UTF-8")));
        output = new ByteArrayOutputStream(15);
        try {
            int c = 8;
            byte[] buffer = new byte[8];
            while ((c = input.read(buffer, 0, c)) != -1) {
                output.write(buffer, 0, c);
            }
            result = output.toByteArray();
        } finally {
            VFSUtils.safeClose(input);
            VFSUtils.safeClose(output);
        }
        assertEquals("HelloWorld !\n", new String(result, "UTF-8"));
    }

    @Test
    public void testAntlr() throws Exception {
        VirtualFile antlr = VFS.getChild(Thread.currentThread().getContextClassLoader().getResource("vfs/test/antlr/META-INF/MANIFEST.MF").toURI());
        VFSUtils.readManifest(antlr);
        VirtualFile antlrArchive = VFS.getChild(Thread.currentThread().getContextClassLoader().getResource("vfs/test/antlr-2.7.5H3.jar").toURI());
        VFSUtils.getManifest(antlrArchive);
    }

}

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
package org.jboss.vfs.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.jboss.vfs.VFSUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
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

}

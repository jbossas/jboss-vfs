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
package org.jboss.test.virtual.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Basic tests of URL connection
 *
 * @author ales.jutin@jboss.org
 */
public class URLConnectionUnitTestCase extends AbstractVFSTest
{
   public URLConnectionUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(URLConnectionUnitTestCase.class);
   }

   protected String getFileName()
   {
      return "outer.jar";
   }

   protected VirtualFile getFile() throws IOException
   {
      URL url = getResource("/vfs/test/");
      VirtualFile root = VFS.getRoot(url);
      VirtualFile file = root.getChild(getFileName());
      assertNotNull(file);
      return file;
   }

   /**
    * Test url connection content.
    *
    * @throws Exception for any error
    */
   public void testContent() throws Exception
   {
      VirtualFile file = getFile();
      URL url = file.toURL();
      URLConnection conn = url.openConnection();
      assertEquals(file, conn.getContent());
   }

   /**
    * Test url connection content lenght.
    *
    * @throws Exception for any error
    */
   public void testContentLenght() throws Exception
   {
      VirtualFile file = getFile();
      URL url = file.toURL();
      URLConnection conn = url.openConnection();
      assertEquals(file.getSize(), conn.getContentLength());
   }

   /**
    * Test url connection last modified.
    *
    * @throws Exception for any error
    */
   public void testLastModified() throws Exception
   {
      VirtualFile file = getFile();
      URL url = file.toURL();
      URLConnection conn = url.openConnection();
      assertEquals(file.getLastModified(), conn.getLastModified());
   }

   /**
    * Test url connection input stream.
    *
    * @throws Exception for any error
    */
   public void testInputStream() throws Exception
   {
      VirtualFile file = getFile();
      URL url = file.toURL();
      URLConnection conn = url.openConnection();
      assertTrue(Arrays.equals(readBytes(file.openStream()), readBytes(conn.getInputStream())));
   }

   protected static byte[] readBytes(InputStream inputStream) throws Exception
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int read = 0;
      byte[] bytes = new byte[1024];
      try
      {
         while (read >=0)
         {
            read = inputStream.read(bytes);
            baos.write(bytes);
         }
      }
      finally
      {
         try
         {
            inputStream.close();
         }
         catch (IOException ignored)
         {
         }
      }
      return baos.toByteArray();
   }
}

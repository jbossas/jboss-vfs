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
package org.jboss.test.vfs;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import junit.framework.Test;

import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.protocol.FileURLConnection;

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

   protected VirtualFile getFile() throws Exception
   {
      VirtualFile root = getVirtualFile("/vfs/test/");
      VirtualFile file = root.getChild(getFileName());
      assertNotNull(file);
      return file;
   }

   protected URL getURLAndAssertProtocol(VirtualFile file) throws Exception
   {
      URL url = file.toURL();
      assertEquals(VFSUtils.VFS_PROTOCOL, url.getProtocol());
      return url;
   }

   /**
    * Test url connection content.
    *
    * @throws Exception for any error
    */
   public void testContent() throws Exception
   {
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
   public void testContentLenght() throws Exception
   {
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
   public void testLastModified() throws Exception
   {
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
   public void testInputStream() throws Exception
   {
      VirtualFile file = getFile();
      URL url = getURLAndAssertProtocol(file);
      URLConnection conn = url.openConnection();
      assertTrue(Arrays.equals(readBytes(file.openStream()), readBytes(conn.getInputStream())));
   }

   public void testPathWithSpaces() throws Exception
   {
      VirtualFile root = getVirtualFile("/vfs/test/");
      VirtualFile file = root.getChild("path with spaces/spaces.ear");
      File real = file.getPhysicalFile();
      assertTrue(real.exists());
      URL url = getURLAndAssertProtocol(file);
      URLConnection conn = url.openConnection();
      assertTrue(Arrays.equals(readBytes(conn.getInputStream()), readBytes(file.openStream())));
   }

   public void testTempPath() throws Exception
   {
      File temp = File.createTempFile("123", ".tmp");
      temp.deleteOnExit();
      VirtualFile file = VFS.getChild(temp.toURI());
      assertTrue(file.exists());
      URL url = getURLAndAssertProtocol(file);
      URLConnection conn = url.openConnection();
      assertEquals(file.getLastModified(), conn.getLastModified());
   }

   public void testOutsideUrl() throws Exception
   {
      URL url = getResource("/vfs/test/outer.jar");
      File file = new File(url.toURI());
       
      url = new URL(VFSUtils.VFS_PROTOCOL, url.getHost(), url.getPort(), url.getFile());

      URLConnection conn = url.openConnection();
      assertEquals(file.lastModified(), conn.getLastModified());
   }

   public void testFileUrl() throws Exception
   {
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
         File urlDerivedFile = (File)url.getContent();
         // Make sure the file returned by the file: URL is not the VFS File (In other words, make sure it does not use the mounts)
         assertTrue(urlDerivedFile.isFile());
         assertFalse(vfsDerivedFile.equals(urlDerivedFile));
      } finally {
         VFSUtils.safeClose(handle, provider);
      }
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

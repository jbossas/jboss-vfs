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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.context.zip.ZipEntryContext;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * ZipEntryVFSContextUnitTestCase.
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class ZipEntryVFSContextUnitTestCase extends JARVFSContextUnitTestCase
{
   public ZipEntryVFSContextUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      VFS.init();
      System.out.println("java.protocol.handler.pkgs: " + System.getProperty("java.protocol.handler.pkgs"));
      return suite(ZipEntryVFSContextUnitTestCase.class);
   }

   protected VFSContext getVFSContext(String name) throws Exception
   {
      URL url = getResource("/vfs/context/jar/" + name + ".jar");
      url = JarUtils.createJarURL(url);
      return new ZipEntryContext(url);
   }

   protected VFSContext createVSFContext(URL url) throws Exception
   {
      return new ZipEntryContext(url);
   }

   /**
    * Test detection of underlying jar file removal through exists()
    *
    * @throws Exception for any error
    */
   public void testRootExists() throws Exception
   {
      URL url = getResource("/vfs/test/outer.jar");
      File tmpJar = File.createTempFile("vfstest", ".jar");

      InputStream is = url.openStream();
      OutputStream os = new FileOutputStream(tmpJar);

      byte [] buff = new byte[65536];
      int count = is.read(buff);
      while(count != -1)
      {
         os.write(buff, 0, count);
         count = is.read(buff);
      }
      os.close();

      ZipEntryContext context = new ZipEntryContext(tmpJar.toURL());
      assertTrue("context.getRoot().exists()", context.getRoot().exists());

      boolean isDeleted = context.getRoot().delete(1000);
      assertTrue("delete tmp file: " + tmpJar, isDeleted);
      assertFalse("context.getRoot().exists()", context.getRoot().exists());
   }

   /**
    * Test for proper handling when file appears to be an archive but
    * trying to handle it produces an exception. Proper behaviour
    * is to ignore exception and treat the file as non-archive.
    *
    * @throws Exception for any error
    */
   public void testNotAnArchive() throws Exception
   {
      URL url = getResource("/vfs/context/jar/");
      FileSystemContext ctx = new FileSystemContext(url);

      // check that vfszip is active
      VirtualFileHandler handler = ctx.getRoot().getChild("archive.jar");
      assertTrue("is vfszip", "vfszip".equals(handler.toURL().getProtocol()));
      assertFalse("is leaf", handler.isLeaf());

      handler = ctx.getRoot().getChild("notanarchive.jar");
      assertTrue("is leaf", handler.isLeaf());
   }

   // we need to make sure this doesn't get touched before
   protected String getNestedName()
   {
      return super.getNestedName() + "_copy";
   }

   protected String getProtocol()
   {
      return "vfszip";
   }
}
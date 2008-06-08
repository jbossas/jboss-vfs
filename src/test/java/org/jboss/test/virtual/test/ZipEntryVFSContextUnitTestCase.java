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

   /**
    * Analog to the same test in {@link JARVFSContextUnitTestCase}
    *
    * @throws Exception
    */
   public void testJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      ZipEntryContext context = new ZipEntryContext(entry);
      assertEquals("child", context.getRoot().getName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = new ZipEntryContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
   }

   /**
    * Analog to the same test in {@link JARVFSContextUnitTestCase}
    *
    * @throws Exception for any error
    */
   public void testPathIsEmptryForJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      ZipEntryContext context = new ZipEntryContext(entry);
      assertEquals("child", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = new ZipEntryContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());
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

      // use noReaper so that the underlying file is not locked
      // when we try to delete it
      String jarUrl = tmpJar.toURL().toString() + "?noReaper=true";
      ZipEntryContext context = new ZipEntryContext(new URL(jarUrl));
      assertTrue("context.getRoot().exists()", context.getRoot().exists());

      boolean isDeleted = tmpJar.delete();
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

   /**
    * Handler representing a directory must return a zero length stream
    *
    * @throws Exception for any error
    */
   public void testDirectoryZipEntryOpenStream() throws Exception
   {
      URL url = getResource("/vfs/context/jar/complex.jar");
      ZipEntryContext ctx = new ZipEntryContext(url);

      VirtualFileHandler sub = ctx.getRoot().getChild("subfolder");
      InputStream is = sub.openStream();
      assertTrue("input stream closed", is.read() == -1);
   }

   /**
    * There was a problem with noCopy inner jars returning empty streams
    *
    * @throws Exception for any error
    */
   public void testInnerJarFileEntryOpenStream() throws Exception
   {
      URL url = getResource("/vfs/context/jar/nested.jar");
      ZipEntryContext ctx = new ZipEntryContext(url);

      VirtualFileHandler nested = ctx.getRoot().getChild("complex.jar");
      VirtualFileHandler target = nested.getChild("META-INF/MANIFEST.MF");

      InputStream is = target.openStream();
      assertFalse("input stream closed", is.read() == -1);
   }
}
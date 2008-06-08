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

import java.io.InputStream;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.virtual.VFS;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.jar.JarContext;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * JARVFSContextUnitTestCase.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class JARVFSContextUnitTestCase extends AbstractVFSContextTest
{
   public JARVFSContextUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      VFS.init();
      System.out.println("java.protocol.handler.pkgs: " + System.getProperty("java.protocol.handler.pkgs"));
      return new TestSuite(JARVFSContextUnitTestCase.class);
   }

   protected VFSContext getVFSContext(String name) throws Exception
   {
      URL url = getResource("/vfs/context/jar/" + name + ".jar");
      url = JarUtils.createJarURL(url);
      return new JarContext(url);
   }

   protected VFSContext getParentVFSContext() throws Exception
   {
      URL url = getResource("/vfs/context/jar/");
      return new FileSystemContext(url);
   }

   /**
    * Create vfs context from url.
    *
    * @param url the url
    * @return new vfs context
    * @throws Exception for any error
    */
   protected VFSContext createVSFContext(URL url) throws Exception
   {
      if (url.toExternalForm().startsWith("jar") == false)
         url = JarUtils.createJarURL(url);
      return new JarContext(url);
   }

   protected String getSuffix()
   {
      return ".jar";
   }

   /**
    * Was having problems with a jar entry as root of VFS.
    *
    * @throws Exception for any error
    */
   public void testJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      VFSContext context = createVSFContext(entry);
      assertEquals("child", context.getRoot().getName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = createVSFContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
   }

   /**
    * Was having problems with a jar entry as root of VFS.
    * A JarEntry that is the root of the VFS should have a VFS Path of ""
    *
    * @throws Exception for any error
    */
   public void testPathIsEmptryForJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      VFSContext context = createVSFContext(entry);
      assertEquals("child", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = createVSFContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());
   }


   /**
    * Handler representing a directory must return a zero length stream
    *
    * @throws Exception for any error
    */
   public void testDirectoryZipEntryOpenStream() throws Exception
   {
      URL url = getResource("/vfs/context/jar/complex.jar");
      VFSContext ctx = createVSFContext(url);

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
      VFSContext ctx = createVSFContext(url);

      VirtualFileHandler nested = ctx.getRoot().getChild("complex.jar");
      VirtualFileHandler target = nested.getChild("META-INF/MANIFEST.MF");

      InputStream is = target.openStream();
      assertFalse("input stream closed", is.read() == -1);
   }
}

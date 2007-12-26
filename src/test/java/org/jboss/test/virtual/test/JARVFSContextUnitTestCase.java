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

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.virtual.VFS;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.jar.JarContext;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.spi.VFSContext;

/**
 * JARVFSContextUnitTestCase.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
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

   protected String getSuffix()
   {
      return ".jar";
   }

   /**
    * Was having problems with a jar entry as root of VFS.
    *
    * @throws Exception
    */
   public void testJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      JarContext context = new JarContext(entry);
      assertEquals("child", context.getRoot().getName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = new JarContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
   }

   /**
    * Was having problems with a jar entry as root of VFS.
    * A JarEntry that is the root of the VFS should have a VFS Path of ""
    *
    * @throws Exception
    */
   public void testPathIsEmptryForJarEntryAsRoot() throws Exception
   {
      URL url = getResource("/vfs/context/jar/simple.jar");
      URL entry = new URL("jar:" + url.toString() + "!/child");
      //entry.openStream().close();
      JarContext context = new JarContext(entry);
      assertEquals("child", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());

      url = getResource("/vfs/test/outer.jar");
      entry = new URL("jar:" + url.toString() + "!/jar2.jar ");
      //entry.openStream().close();
      context = new JarContext(entry);
      assertEquals("jar2.jar", context.getRoot().getName());
      assertEquals("", context.getRoot().getPathName());
   }

}

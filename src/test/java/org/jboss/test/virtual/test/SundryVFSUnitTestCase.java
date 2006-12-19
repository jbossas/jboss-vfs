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

import org.jboss.test.BaseTestCase;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.jar.JarUtils;

/**
 * SundryVFSTests.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class SundryVFSUnitTestCase extends BaseTestCase
{
   public SundryVFSUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return new TestSuite(SundryVFSUnitTestCase.class);
   }

   protected VirtualFile getVirtualFile(String root, String path) throws Exception
   {
      URL url = getResource(root);
      assertNotNull(url);
      return VFS.getVirtualFile(url, path);
   }
   
   public void testBrokenContext() throws Exception
   {
      VirtualFile file = getVirtualFile("/vfs/sundry", "jar/archive.jar");
      log.debug(file.getName() + " " + file);
      assertFalse("Should not be a leaf", file.isLeaf());
      file = file.findChild("empty");
      log.debug(file.getName() + " " + file);
   }
   
   public void testArchive() throws Exception
   {
      VirtualFile file = getVirtualFile("/vfs/sundry/", "jar/archive.jar");
      log.debug(file.getName() + " " + file);
      assertFalse("Should not be a leaf", file.isLeaf());
      file = file.findChild("empty");
      log.debug(file.getName() + " " + file);
   }
   
   public void testArchive2() throws Exception
   {
      URL url = getResource("/vfs/sundry/jar/archive.jar");
      url = JarUtils.createJarURL(url);
      VFS vfs = VFS.getVFS(url);
      VirtualFile file = vfs.getRoot();
      log.debug(file.getName() + " " + file);
      assertFalse("Should not be a leaf", file.isLeaf());
      file = file.findChild("empty");
      log.debug(file.getName() + " " + file);
   }
}

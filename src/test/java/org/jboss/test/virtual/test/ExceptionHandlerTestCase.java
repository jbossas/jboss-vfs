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
import java.util.List;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * ExceptionHandlerTestCase.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class ExceptionHandlerTestCase extends AbstractVFSTest
{
   public static Test suite()
   {
      return suite(ExceptionHandlerTestCase.class);
   }

   public ExceptionHandlerTestCase(String name)
   {
      super(name, true);
   }

   public void testZipEntriesInit() throws Exception
   {
      URL url = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(url);
      vfs.setExceptionHandler(new NamesExceptionHandler("_sqljdbc.jar"));
      VirtualFile root = vfs.getRoot();
      VirtualFile zipeinit = root.findChild("zipeinit.jar");
      VirtualFile child = zipeinit.findChild("sqljdbc.jar");
      List<VirtualFile> children = child.getChildren();
      assertTrue(children.isEmpty());
   }
}

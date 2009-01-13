/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Test file closing
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class FileCloseUnitTestCase extends AbstractVFSTest
{
   public FileCloseUnitTestCase(String name)
   {
      super(name, true, false);
   }

   protected FileCloseUnitTestCase(String name, boolean forceCopy)
   {
      super(name, forceCopy, false);
   }

   public static Test suite()
   {
      VFS.init();
      return suite(FileCloseUnitTestCase.class);
   }

   public void testNestedJarClosing() throws Exception
   {
      URL url = getResource("/vfs/test/nested/nested.jar");
      VirtualFile root = VFS.getRoot(url);
      assertNotNull(root);
      VirtualFile child = root.getChild("complex.jar");
      assertNotNull(child);
      VirtualFile nestedChild = child.getChild("child");
      assertNotNull(nestedChild);

      // TODO - some real test of deletion
      nestedChild.close();
      child.close();
      root.close();
   }
}
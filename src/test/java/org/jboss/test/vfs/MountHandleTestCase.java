/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.spi.MountHandle;

import java.io.File;

/**
 * Tests functionality of the MountHandle retrieving mount source.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 */
public class MountHandleTestCase extends AbstractVFSTest {

   public MountHandleTestCase(final String name) {
      super(name);
   }

   public void testZipGetMountSource() throws Exception {
      VirtualFile jar  = getVirtualFile("/vfs/test/jar1.jar");
      File origin = jar.getPhysicalFile();
      MountHandle mountHandle = VFS.mountZip(jar, jar, provider);
      try
      {
         File mounted = jar.getPhysicalFile();
         File source = mountHandle.getMountSource();

         assertNotNull(origin);
         assertNotNull(mounted);
         assertNotNull(source);
         assertFalse(origin.equals(mounted));
         assertFalse(origin.equals(source));
         assertFalse(mounted.equals(source));

         assertTrue(origin.isFile());
         assertTrue(source.isFile());
         assertTrue(mounted.isDirectory());

         assertEquals(origin.length(), source.length());
      } finally {
         VFSUtils.safeClose(mountHandle);
      }
   }
}

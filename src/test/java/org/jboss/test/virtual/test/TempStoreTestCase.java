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
import java.net.URL;

import junit.framework.Test;
import org.jboss.test.virtual.support.MockTempStore;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.copy.AbstractCopyMechanism;
import org.jboss.virtual.plugins.copy.DeleteOnExitTempStore;
import org.jboss.virtual.plugins.copy.MkdirTempStore;
import org.jboss.virtual.plugins.copy.TrackingTempStore;
import org.jboss.virtual.spi.TempStore;

/**
 * Test TempStore usage.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class TempStoreTestCase extends AbstractVFSTest
{
   public TempStoreTestCase(String s)
   {
      super(s);
   }

   public static Test suite()
   {
      return suite(TempStoreTestCase.class);
   }

   public void testCopyMechanism() throws Throwable
   {
      long seed = System.nanoTime();
      URL url = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(url);
      TempStore store = new MkdirTempStore(new DeleteOnExitTempStore(new MockTempStore(seed)));
      vfs.setTempStore(store);
      VirtualFile file = vfs.getChild("jar1.jar");
      VirtualFile temp = VFSUtils.explode(file);
      try
      {
         File tempRoot = AbstractCopyMechanism.getTempDirectory();
         File test = new File(tempRoot, "jar1.jar" + '_' + seed);
         assertTrue(test.exists()); // should be created by MockTS
      }
      finally
      {
         temp.cleanup();
      }
   }

   public void testNestedZip() throws Throwable
   {
      long seed = System.nanoTime();
      URL url = getResource("/vfs/test/nested");
      VFS vfs = VFS.getVFS(url);
      VFSUtils.enableCopy(vfs);
      TempStore store = new MkdirTempStore(new TrackingTempStore(new MockTempStore(seed)));
      try
      {
         vfs.setTempStore(store);
         VirtualFile file = vfs.getChild("nested.jar");
         assertNotNull(file.getChild("complex.jar/subfolder/subchild"));
         try
         {
            File tempRoot = AbstractCopyMechanism.getTempDirectory();
            File test = new File(tempRoot, "complex.jar" + '_' + seed);
            assertTrue(test.exists()); // should be created by MockTS
         }
         finally
         {
            file.cleanup();
         }
      }
      finally
      {
         store.clear();
      }
   }
}
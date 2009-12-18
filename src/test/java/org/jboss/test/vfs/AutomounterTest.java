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
package org.jboss.test.deployers.vfs.structure;

import java.net.URL;

import org.jboss.deployers.vfs.plugins.structure.Automounter;
import org.jboss.test.BaseTestCase;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Test for {@link Automounter}
 * 
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class AutomounterTest extends BaseTestCase
{

   public AutomounterTest(String name)
   {
      super(name);
   }

   public void testMountAndCleanup() throws Exception
   {
      VirtualFile virtualFile = getVirtualFile("/structure/ear/archive.ear");
      Automounter.mount(virtualFile, virtualFile);
      assertTrue(Automounter.isMounted(virtualFile));
      Automounter.cleanup(virtualFile);
      assertFalse(Automounter.isMounted(virtualFile));
   }

   public void testCleanupWithOwner() throws Exception
   {
      VirtualFile earVirtualFile = getVirtualFile("/structure/ear/simple/simple.ear");
      Automounter.mount(earVirtualFile);

      VirtualFile jarVirtualFile = earVirtualFile.getChild("archive.jar");
      Automounter.mount(earVirtualFile, jarVirtualFile);

      VirtualFile warVirtualFile = earVirtualFile.getChild("simple.war");
      Automounter.mount(earVirtualFile, warVirtualFile);

      assertTrue(Automounter.isMounted(earVirtualFile));
      assertTrue(Automounter.isMounted(warVirtualFile));
      assertTrue(Automounter.isMounted(jarVirtualFile));

      Automounter.cleanup(earVirtualFile);

      assertFalse(Automounter.isMounted(earVirtualFile));
      assertFalse(Automounter.isMounted(warVirtualFile));
      assertFalse(Automounter.isMounted(jarVirtualFile));
   }

   public void testCleanupRecursive() throws Exception
   {
      VirtualFile earVirtualFile = getVirtualFile("/structure/ear/simple/simple.ear");
      Automounter.mount(earVirtualFile);

      VirtualFile jarVirtualFile = earVirtualFile.getChild("archive.jar");
      Automounter.mount(jarVirtualFile);

      VirtualFile warVirtualFile = earVirtualFile.getChild("simple.war");
      Automounter.mount(warVirtualFile);

      assertTrue(Automounter.isMounted(earVirtualFile));
      assertTrue(Automounter.isMounted(warVirtualFile));
      assertTrue(Automounter.isMounted(jarVirtualFile));

      Automounter.cleanup(earVirtualFile);

      assertFalse(Automounter.isMounted(earVirtualFile));
      assertFalse(Automounter.isMounted(warVirtualFile));
      assertFalse(Automounter.isMounted(jarVirtualFile));
   }

   public void testCleanupRefereces() throws Exception
   {
      VirtualFile earVirtualFile = getVirtualFile("/structure/ear/simple/simple.ear");
      Automounter.mount(earVirtualFile);

      VirtualFile jarVirtualFile = getVirtualFile("/structure/jar/indirectory/archive.jar");
      Automounter.mount(earVirtualFile, jarVirtualFile);

      VirtualFile warVirtualFile = getVirtualFile("/structure/war/simple/simple.war");
      Automounter.mount(earVirtualFile, warVirtualFile);

      assertTrue(Automounter.isMounted(earVirtualFile));
      assertTrue(Automounter.isMounted(warVirtualFile));
      assertTrue(Automounter.isMounted(jarVirtualFile));

      VirtualFile otherEarVirtualFile = getVirtualFile("/structure/ear/scanning.ear");
      Automounter.mount(otherEarVirtualFile, jarVirtualFile);

      Automounter.cleanup(earVirtualFile);

      assertFalse(Automounter.isMounted(earVirtualFile));
      assertFalse(Automounter.isMounted(warVirtualFile));
      assertTrue("Should not have unmounted the reference from two locations", Automounter.isMounted(jarVirtualFile));

      Automounter.cleanup(otherEarVirtualFile);
      assertFalse(Automounter.isMounted(jarVirtualFile));

   }

   protected VirtualFile getVirtualFile(String path) throws Exception
   {
      URL url = getResource(path);
      VirtualFile rootFile = VFS.getChild(url);
      return rootFile;
   }

}

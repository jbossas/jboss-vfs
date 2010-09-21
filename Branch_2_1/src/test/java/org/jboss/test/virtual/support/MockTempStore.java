/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.test.virtual.support;

import java.io.File;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.copy.AbstractCopyMechanism;
import org.jboss.virtual.spi.TempStore;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MockTempStore implements TempStore
{
   private File tempRoot = AbstractCopyMechanism.getTempDirectory();
   private long seed;

   public MockTempStore(long seed)
   {
      this.seed = seed;
   }

   public File createTempFolder(VirtualFile file)
   {
      String name = file.getName();
      return new File(tempRoot, name + '_' + seed);
   }

   public File createTempFolder(String outerName, String innerName)
   {
      return new File(tempRoot, innerName + '_' + seed);
   }

   public void clear()
   {      
   }
}

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
package org.jboss.virtual.plugins.copy;

import java.io.File;

import org.jboss.virtual.spi.TempStore;
import org.jboss.virtual.VirtualFile;

/**
 * Delete on exit temp store wrapper.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DeleteOnExitTempStore implements TempStore
{
   private TempStore delegate;

   public DeleteOnExitTempStore(TempStore delegate)
   {
      if (delegate == null)
         throw new IllegalArgumentException("Null delegate");
      this.delegate = delegate;
   }

   public File createTempFolder(VirtualFile file)
   {
      File dir = delegate.createTempFolder(file);
      dir.deleteOnExit();
      return dir;
   }

   public File createTempFolder(String outerName, String innerName)
   {
      File dir = delegate.createTempFolder(outerName, innerName);
      dir.deleteOnExit();
      return dir;
   }

   public void clear()
   {
      // do nothing
   }
}

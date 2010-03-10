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
package org.jboss.vfs;

import org.jboss.vfs.spi.FileSystem;
import org.jboss.vfs.spi.MountHandle;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * MountHandle implementation.  Provides the default behavior
 * of delegating to the FileSystem to get the mount source as
 * well as cleaning up resources.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 */
class BasicMountHandle implements MountHandle {
   private final FileSystem fileSystem;
   private final Closeable mountHandle;
   private final Closeable[] closeables;

   /**
    * Create new DefaultMountHandle with a FileSystem and an array of closeable.
    *
    * @param fileSystem to use to retrieve the mount source
    * @param mountHandle the handle to close the actual mount
    * @param additionalCloseables addition Closeable to execute on close 
    */
   public BasicMountHandle(final FileSystem fileSystem, Closeable mountHandle, Closeable... additionalCloseables) {
      this.fileSystem = fileSystem;
      this.mountHandle = mountHandle;
      this.closeables = additionalCloseables;
   }

   /* {@inheritDoc} */
   public File getMountSource() {
      return fileSystem.getMountSource();
   }

   /* {@inheritDoc} */
   public void close() throws IOException {
      VFSUtils.safeClose(fileSystem);
      VFSUtils.safeClose(mountHandle);
      for(Closeable closeable : closeables) {
         VFSUtils.safeClose(closeable);
      }
   }
}

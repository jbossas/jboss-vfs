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

package org.jboss.virtual.spi;

import java.io.File;
import java.io.IOException;
import java.io.Closeable;
import java.io.InputStream;
import java.util.List;
import java.util.Iterator;

/**
 * A file system which is mounted in to the VFS.  This is the driver class for a given virtual file system type.  An
 * instance of {@code FileSystem} will be mounted at some point on a VFS.  The specific instance is only called when
 * a file from this filesystem is called upon.  The path components passed in to the operations are canonical, with
 * no "." or ".." components.
 */
public interface FileSystem extends Closeable
{
   /**
    * Get a real {@code File} for the given path within this filesystem.  Some filesystem types will need to make a copy
    * in order to return this file; such copies should be cached and retained until the filesystem is closed.  Depending
    * on the file type, the real path of the returned {@code File} may or may not bear a relationship to the virtual path
    * provided; if such a relationship is required, it must be negotiated at the time the filesystem is mounted.
    *
    * @param pathComponents the relative path components
    * @return the file instance
    * @throws IOException if an I/O error occurs
    */
   File getFile(List<String> pathComponents) throws IOException;

   /**
    * Open an input stream for the file at the given relative path.
    *
    * @param pathComponents the relative path components
    * @return the input stream
    * @throws IOException if an I/O error occurs
    */
   InputStream openInputStream(List<String> pathComponents) throws IOException;

   /**
    * Determine whether this filesystem is read-only.  A read-only filesystem prohibits file modification or
    * deletion.  It is not an error to mount a read-write filesystem within a read-only filesystem however (this
    * operation does not take place within the {@code FileSystem} implementation).
    *
    * @return {@code true} if the filesystem is read-only
    */
   boolean isReadOnly();

   boolean delete(List<String> pathComponents) throws IOException;

   long getSize(List<String> pathComponents) throws IOException;

   long getLastModified(List<String> pathComponents) throws IOException;

   boolean exists(List<String> pathComponents) throws IOException;

   boolean isDirectory(List<String> pathComponents) throws IOException;

   /**
    * Read a directory.  Returns all the simple path names (excluding "." and "..").
    *
    * @param directoryPathComponents the relative path components for the directory
    * @return the directory entries, or {@code null} if the specified path does not refer to a directory
    * @throws IOException if an I/O error occurs
    */
   Iterator<String> getDirectoryEntries(List<String> directoryPathComponents) throws IOException;

   /**
    * Destroy this filesystem instance.  After this method is called, the filesystem may not be used in any way.  This
    * method should be called only after all mounts of this filesystem have been cleared; otherwise, VFS accesses may
    * result in {@code IOException}s.
    *
    * @throws IOException if an I/O error occurs during close
    */
   void close() throws IOException;
}

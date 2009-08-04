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

package org.jboss.vfs.spi;

import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.io.Closeable;
import java.io.InputStream;
import java.util.List;

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
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return the file instance
    * @throws IOException if an I/O error occurs
    */
   File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Open an input stream for the file at the given relative path.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return the input stream
    * @throws IOException if an I/O error occurs
    */
   InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Determine whether this filesystem is read-only.  A read-only filesystem prohibits file modification or
    * deletion.  It is not an error to mount a read-write filesystem within a read-only filesystem however (this
    * operation does not take place within the {@code FileSystem} implementation).
    *
    * @return {@code true} if the filesystem is read-only
    */
   boolean isReadOnly();

   /**
    * Attempt to delete a virtual file within this filesystem.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return {@code true} if the file was deleted, {@code false} if it failed for any reason
    * @throws IOException if an I/O error occurs
    */
   boolean delete(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Get the size of a virtual file within this filesystem.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return the size, in bytes
    * @throws IOException if an I/O error occurs
    */
   long getSize(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Get the last modification time of a virtual file within this filesystem.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return the modification time in milliseconds
    * @throws IOException if an I/O error occurs
    */
   long getLastModified(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Ascertain the existance of a virtual file within this filesystem.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return {@code true} if the file exists, {@code false} otherwise
    * @throws IOException if an I/O error occurs
    */
   boolean exists(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Ascertain whether a virtual file within this filesystem is a directory.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return {@code true} if the file exists and is a directory, {@code false} otherwise
    * @throws IOException if an I/O error occurs
    */
   boolean isDirectory(VirtualFile mountPoint, VirtualFile target);

   /**
    * Read a directory.  Returns all the simple path names (excluding "." and "..").  The returned list will be
    * empty if the node is not a directory.
    *
    * @param mountPoint the mount point of the filesystem instance (guaranteed to be a parent of {@code target})
    * @param target the virtual file to act upon
    * @return the collection of children names
    * @throws IOException if an I/O error occurs
    */
   List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) throws IOException;

   /**
    * Destroy this filesystem instance.  After this method is called, the filesystem may not be used in any way.  This
    * method should be called only after all mounts of this filesystem have been cleared; otherwise, VFS accesses may
    * result in {@code IOException}s.
    *
    * @throws IOException if an I/O error occurs during close
    */
   void close() throws IOException;
}

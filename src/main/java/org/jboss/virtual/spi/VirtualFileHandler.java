/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.VirtualFile;

/**
 * A virtual file handler
 * 
 * @author Scott.Stark@jboss.org
 * @author Adrian.Brock
 * @version $Revision: 44334 $
 */
public interface VirtualFileHandler extends Serializable
{
   /**
    * Get the simple VF name (X.java)
    * 
    * @return the simple file name
    */
   String getName();

   /**
    * Get the VFS relative path name (org/jboss/X.java)
    * 
    * @return the VFS relative path name
    */
   String getPathName();

   /**
    * Get a VFS-based URL
    *
    * @return the url
    * @throws URISyntaxException for an error parsing the URI
    * @throws MalformedURLException for any error
    */
   URL toVfsUrl() throws MalformedURLException, URISyntaxException;
   

   /**
    * Get the VF URI (file://root/org/jboss/X.java)
    * 
    * @return the full URI to the VF in the VFS.
    * @throws URISyntaxException for an error parsing the URI 
    */
   URI toURI() throws URISyntaxException;

   /**
    * Get the VF URL (file://root/org/jboss/X.java)
    * 
    * @return the full URL to the VF in the VFS.
    * @throws URISyntaxException for an error parsing the URI 
    * @throws MalformedURLException for any error
    */
   URL toURL() throws MalformedURLException, URISyntaxException;

   /**
    * When the file was last modified
    * 
    * @return the last modified time
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if closed
    */
   long getLastModified() throws IOException;

   /**
    * Returns true if the file has been modified since this method was last called
    * Last modified time is initialized at handler instantiation.
    *
    * @return true if modified, false otherwise
    * @throws IOException for any error
    */
   boolean hasBeenModified() throws IOException;
   
   /**
    * Get the size
    * 
    * @return the size
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if closed
    */
   long getSize() throws IOException;

   /**
    * Tests whether the underlying implementation file still exists.
    * @return true if the file exists, false otherwise.
    * @throws IOException - thrown on failure to detect existence.
    */
   boolean exists() throws IOException;

   /**
    * Whether it is a simple leaf of the VFS,
    * i.e. whether it can contain other files
    * 
    * @return true if a simple file.
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   boolean isLeaf() throws IOException;
   
   /**
    * Whether it is hidden
    * 
    * @return true if hidden.
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if closed
    */
   boolean isHidden() throws IOException;

   /**
    * Access the file contents.
    * 
    * @return An InputStream for the file contents.
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if closed
    */
   InputStream openStream() throws IOException;

   /**
    * Get the parent
    * 
    * @return the parent
    * @throws IOException for an error accessing the file system
    * @throws IllegalStateException if closed
    */
   VirtualFileHandler getParent() throws IOException;

   /**
    * Get the children
    * 
    * @param ignoreErrors whether to ignore errors
    * @return the children
    * @throws IOException for an error accessing the file system
    * @throws IllegalStateException if closed
    */
   List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException;

   /**
    * Find a child
    * 
    * @param path the path
    * @return the child
    * @throws IOException for an error accessing the file system (or the child doesn't exist)
    * @throws IllegalStateException if closed
    */
   VirtualFileHandler findChild(String path) throws IOException;

   /**
    * Get a child
    *
    * @param path the path
    * @return the child or <code>null</code> if not found
    * @throws IOException for an error accessing the file system
    * @throws IllegalStateException if closed
    */
   VirtualFileHandler getChild(String path) throws IOException;

   /**
    * Get the VFSContext this file belongs to
    * 
    * @return the context
    * @throws IllegalStateException if closed
    */
   VFSContext getVFSContext();

   /**
    * Get the virtual file wrapper
    * 
    * @return the wrapper
    * @throws IllegalStateException if closed
    */
   VirtualFile getVirtualFile();
   
   /**
    * Close the resources
    */
   void close();
}

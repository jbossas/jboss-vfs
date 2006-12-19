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
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jboss.virtual.VFS;

/** 
 * A virtual file context
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 55466 $
 */
public interface VFSContext
{
   /**
    * Get the root uri
    * 
    * @return the root uri
    */
   URI getRootURI();

   /**
    * Get the VFS for this context
    * 
    * @return the vfs
    */
   VFS getVFS();
   
   /**
    * Return the root virtual file
    * 
    * @return the root
    * @throws IOException for any problem accessing the VFS
    */
   VirtualFileHandler getRoot() throws IOException;

   /**
    * Get the context option settings
    * 
    * @return a map of the context options
    */
   Map<String, String> getOptions();

   /**
    * Get the children
    * 
    * @param parent the parent
    * @param ignoreErrors whether to ignore errors
    * @return the children
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException for a null parent
    */
   List<VirtualFileHandler> getChildren(VirtualFileHandler parent, boolean ignoreErrors) throws IOException;
   
   /**
    * Find a child
    * 
    * @param parent the parent
    * @param path the path
    * @return the child
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException for a null parent or name
    */
   VirtualFileHandler findChild(VirtualFileHandler parent, String path) throws IOException;
   
   /**
    * Visit the virtual file system
    * 
    * @param handler the reference handler
    * @param visitor the visitor
    * @throws IOException for any error
    * @throws IllegalArgumentException if the handler or visitor is null
    */
   void visit(VirtualFileHandler handler, VirtualFileHandlerVisitor visitor) throws IOException;
}

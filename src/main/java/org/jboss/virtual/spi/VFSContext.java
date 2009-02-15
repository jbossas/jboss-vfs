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

import org.jboss.virtual.VFS;

/** 
 * A virtual file context
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @author ales.justin@jboss.org
 * @version $Revision: 55466 $
 */
public interface VFSContext
{
   /**
    * Get the name.
    *
    * @return the name
    */
   String getName();

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
    * Return the peer representing the root of this context within another context.
    * Used when mounting contexts within other contexts
    *
    * @return the root peer
    */
   VirtualFileHandler getRootPeer();

   /**
    * Get options.
    *
    * @return the options
    */
   Options getOptions();

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
    * Get a child
    *
    * @param parent the parent
    * @param path the path
    * @return the child or <code>null</code> if not found
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException for a null parent or name
    */
   VirtualFileHandler getChild(VirtualFileHandler parent, String path) throws IOException;

   /**
    * Visit the virtual file system
    * 
    * @param handler the reference handler
    * @param visitor the visitor
    * @throws IOException for any error
    * @throws IllegalArgumentException if the handler or visitor is null
    */
   void visit(VirtualFileHandler handler, VirtualFileHandlerVisitor visitor) throws IOException;

   /**
    * Get the exception handler.
    *
    * @return the exception handler
    */
   ExceptionHandler getExceptionHandler();

   /**
    * Set exception handler.
    *
    * @param exceptionHandler the exception handler.
    */
   void setExceptionHandler(ExceptionHandler exceptionHandler);

   /**
    * Add temp info.
    *
    * @param tempInfo the temp info
    */
   void addTempInfo(TempInfo tempInfo);

   /**
    * Get exact temp info match.
    *
    * @param path the path to match
    * @return temp info instance or null if not found
    */
   TempInfo getTempInfo(String path);

   /**
    * Iterate over all temp infos.
    * This should return lexicographically ordered temp infos.
    *
    * @return ordered temp infos
    */
   Iterable<TempInfo> getTempInfos();

   /**
    * Cleanup all temp infos under path param.
    *
    * @param path the path to cleanup
    */
   void cleanupTempInfo(String path);
}

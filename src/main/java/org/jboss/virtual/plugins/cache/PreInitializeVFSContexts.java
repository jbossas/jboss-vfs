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
package org.jboss.virtual.plugins.cache;

import java.net.URL;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;

/**
 * Initialize vfs contexts - performance improvements.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class PreInitializeVFSContexts
{
   private Logger log = Logger.getLogger(PreInitializeVFSContexts.class);
   private List<URL> initializedVFSContexts;

   /**
    * Start initializer.
    *
    * @throws Exception for any exception
    */
   public void start() throws Exception
   {
      if (initializedVFSContexts != null && initializedVFSContexts.isEmpty() == false)
      {
         for (URL url : initializedVFSContexts)
         {
            VFS vfs = VFS.getVFS(url);
            log.debug("Initialized Virtual File: " + vfs.getRoot());
         }
      }
   }

   /**
    * Set URLs that need to be initialized before anything else.
    *
    * @param initializedVFSContexts the URLs to be initialized
    */
   public void setInitializedVFSContexts(List<URL> initializedVFSContexts)
   {
      this.initializedVFSContexts = initializedVFSContexts;
   }
}

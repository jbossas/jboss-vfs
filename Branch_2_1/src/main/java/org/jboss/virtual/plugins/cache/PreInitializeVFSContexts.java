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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.spi.ExceptionHandler;
import org.jboss.virtual.spi.TempStore;

/**
 * Initialize vfs contexts - performance improvements.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class PreInitializeVFSContexts
{
   protected Logger log = Logger.getLogger(getClass());

   private Map<URL, ExceptionHandler> initializedVFSContexts;
   private Map<URL, TempStore> tempStores;
   private boolean holdReference;
   private List<VFS> references;

   /**
    * Start initializer.
    *
    * @throws Exception for any exception
    */
   public void start() throws Exception
   {
      Map<URL, VFS> refs = new HashMap<URL, VFS>();

      // apply exception handlers
      if (initializedVFSContexts != null && initializedVFSContexts.isEmpty() == false)
      {
         for (Map.Entry<URL, ExceptionHandler> entry : initializedVFSContexts.entrySet())
         {
            URL url = entry.getKey();
            VFS vfs = VFS.getVFS(url);

            ExceptionHandler eh = entry.getValue();
            if (eh != null)
               vfs.setExceptionHandler(eh);

            log.debug("Initialized Virtual File: " + vfs.getRoot());
            refs.put(url, vfs);
         }
      }

      // apply stores
      if (tempStores != null && tempStores.isEmpty() == false)
      {
         for (Map.Entry<URL, TempStore> entry : tempStores.entrySet())
         {
            URL url = entry.getKey();
            VFS vfs = refs.get(url);
            if (vfs == null)
            {
               vfs = VFS.getVFS(url);
               log.debug("Initialized Virtual File: " + vfs.getRoot());
               refs.put(url, vfs);
            }

            TempStore ts = entry.getValue();
            if (ts != null)
               vfs.setTempStore(ts);
         }
      }

      if (holdReference)
         references = new ArrayList<VFS>(refs.values());
   }

   /**
    * Clear possible references.
    */
   public void stop()
   {
      if (references != null)
         references.clear();
   }

   /**
    * Get VFS references.
    *
    * @return the VFS references
    */
   public List<VFS> getReferences()
   {
      if (references == null || references.isEmpty())
         return Collections.emptyList();
      else
         return Collections.unmodifiableList(references);
   }

   /**
    * Set URLs that need to be initialized before anything else.
    *
    * @param initializedVFSContexts the URLs to be initialized
    */
   public void setInitializedVFSContexts(Map<URL, ExceptionHandler> initializedVFSContexts)
   {
      this.initializedVFSContexts = initializedVFSContexts;
   }

   /**
    * Set temp stores.
    *
    * @param tempStores the temp stores
    */
   public void setTempStores(Map<URL, TempStore> tempStores)
   {
      this.tempStores = tempStores;
   }

   /**
    * Should we hold the reference to initialized VFSs.
    *
    * @param holdReference the hold reference flag
    */
   public void setHoldReference(boolean holdReference)
   {
      this.holdReference = holdReference;
   }
}

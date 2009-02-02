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
package org.jboss.virtual.spi.cache;

import java.net.URI;
import java.net.URL;

import org.jboss.virtual.spi.VFSContext;

/**
 * Simple vfs cache interface.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public interface VFSCache
{
   /**
    * Find the context based on uri.
    *
    * @param uri the uri
    * @return found context or null
    */
   VFSContext findContext(URI uri);

   /**
    * Find the context based on url.
    *
    * @param url the url
    * @return found context or null
    */
   VFSContext findContext(URL url);

   /**
    * Put vfs context to cache.
    *
    * @param context the vfs context
    */
   void putContext(VFSContext context);

   /**
    * Remove vfs context from cache.
    *
    * @param context the vfs context
    */
   void removeContext(VFSContext context);

   /**
    * Start the cache.
    *
    * @throws Exception for any error
    */
   void start() throws Exception;

   /**
    * Stop the cache.
    */
   void stop();

   /**
    * Flush the cache.
    */
   void flush();
}

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

import java.net.URI;
import java.util.List;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.VFSContext;

/**
 * Iterable vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class PathMatchingVFSCache extends AbstractVFSCache
{
   /**
    * Match the uri's path with cached contexts path.
    *
    * @param uri the uri to match
    * @return found context or null
    */
   public VFSContext findContext(URI uri)
   {
      String uriString = VFSUtils.stripProtocol(uri);
      List<String> tokens = PathTokenizer.getTokens(uriString);
      StringBuilder sb = new StringBuilder("/");
      readLock();
      try
      {
         for (String token : tokens)
         {
            sb.append(token).append("/");
            VFSContext context = getContext(sb.toString());
            if (context != null)
               return context;
         }
      }
      finally
      {
         readUnlock();
      }
      return null;
   }
}
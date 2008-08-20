/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors. 
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
package org.jboss.virtual.plugins.context.memory;


import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Virtual memory context.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class MemoryContext extends AbstractVFSContext implements Serializable
{
   private static final long serialVersionUID = 1L;

   /** The root file */
   private final MemoryContextHandler root;
   
   /** A reference to the virtual file of the root to stop it getting closed */
   private final VirtualFile rootFile;

   protected MemoryContext(URL url) throws URISyntaxException
   {
      super(url);
      root = new MemoryContextHandler(this, null, url, url.getFile());
      rootFile = root.getVirtualFile();
   }

   public String getName()
   {
      return root.getName();
   }

   public VirtualFileHandler getRoot() throws IOException
   {
      return root;
   }
   
   VirtualFile createDirectory(URL url)
   {
      return putFile(url, null);
   }
   
   VirtualFile putFile(URL url, byte[] contents)
   {
      try
      {
         List<String> tokens = PathTokenizer.getTokens(url.getPath());
         if (tokens == null || tokens.size() == 0)
         {
            return null;
         }

         boolean definitelyNew = false;
         String protocolAndHost = url.getProtocol() + "://" + url.getHost();
         StringBuffer path = new StringBuffer(protocolAndHost);
         MemoryContextHandler current = root;
         for (String token : tokens)
         {
            path.append("/");
            path.append(token);

            if (definitelyNew == false)
            {
               try
               {
                  MemoryContextHandler child = current.getDirectChild(token);
                  if (child != null)
                  {
                     current = child;
                     continue;
                  }
               }
               catch (Exception ignore)
               {
               }
               definitelyNew = true;
            }

            URL localUrl = new URL(path.toString());
            if (current.getContents() != null)
            {
               throw new IllegalStateException("Cannot add a child to " + current + ", it already has contents");
            }
            current = new MemoryContextHandler(this, current, localUrl, token);
         }
         
         current.setContents(contents);
         return current.getVirtualFile();
      }
      catch(MalformedURLException e)
      {
         throw new RuntimeException(e);
      }
   }  
}

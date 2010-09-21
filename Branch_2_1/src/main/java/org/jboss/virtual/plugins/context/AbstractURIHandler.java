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
package org.jboss.virtual.plugins.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * URIHandler stub.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractURIHandler extends AbstractVirtualFileHandler
{
   private static final long serialVersionUID = 1L;

   /** The uri */
   private final URI uri;
   
   /**
    * Create a newURLHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param uri the uri
    * @param name the name
    * @throws IllegalArgumentException for a null context, vfsPath or url
    */
   public AbstractURIHandler(VFSContext context, VirtualFileHandler parent, URI uri, String name)
   {
      super(context, parent, name);
      if (uri == null)
         throw new IllegalArgumentException("Null uri");
      this.uri = uri;
   }
   
   /**
    * Get the uri
    * 
    * @return the uri
    */
   public URI getURI()
   {
      return uri;
   }

   public long getLastModified() throws IOException
   {
      checkClosed();
      return 0;
   }

   public long getSize() throws IOException
   {
      checkClosed();
      return 0;
   }

   public boolean isHidden() throws IOException
   {
      checkClosed();
      return false;
   }

   public InputStream openStream() throws IOException
   {
      checkClosed();
      return null;
   }

   public URI toURI()
   {
      return uri;
   }
}

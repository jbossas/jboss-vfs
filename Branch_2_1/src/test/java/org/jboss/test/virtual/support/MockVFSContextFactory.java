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
package org.jboss.test.virtual.support;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;

/**
 * MockVFSContextFactory.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MockVFSContextFactory implements VFSContextFactory
{
   /** The protocols */
   public static final String[] protocols = { "mock" };

   /** The contexts */
   private Map<URI, VFSContext> contexts = new ConcurrentHashMap<URI, VFSContext>();
   
   /** When to throw an IOException */
   private String ioException = "";
   
   public String[] getProtocols()
   {
      return protocols;
   }

   /**
    * Set the ioException.
    * 
    * @param ioException the ioException.
    */
   public void setIOException(String ioException)
   {
      this.ioException = ioException;
   }

   /**
    * Check whether we should throw an IOException
    * 
    * @param when when to throw
    * @throws IOException when requested
    */
   public void throwIOException(String when) throws IOException
   {
      if (ioException.equals(when))
         throw new IOException("Throwing IOException from " + when);
   }

   public VFSContext getVFS(URI rootURI) throws IOException
   {
      throwIOException("getVFSURI");
      VFSContext context = contexts.get(rootURI);
      if (context == null)
         throw new IOException("No such context " + rootURI);
      return context;
   }
   
   public VFSContext getVFS(URL rootURL) throws IOException
   {
      throwIOException("getVFSURL");
      try
      {
         return getVFS(rootURL.toURI());
      }
      catch (URISyntaxException e)
      {
         throw new IOException("Error creating URI: " + rootURL);
      }
   }

   /**
    * Add a context
    * 
    * @param context the context
    * @throws IllegalArgumentException for a null context
    */
   public void addVFSContext(VFSContext context)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      contexts.put(context.getRootURI(), context);
   }

   /**
    * Remove a context
    * 
    * @param context the context
    * @throws IllegalArgumentException for a null context
    */
   public void removeVFSContext(VFSContext context)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      contexts.remove(context.getRootURI());
   }

   /**
    * Reset
    */
   public void reset()
   {
      contexts.clear();
      ioException = "";
   }
}

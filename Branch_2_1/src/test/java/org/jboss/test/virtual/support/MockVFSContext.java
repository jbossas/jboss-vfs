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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.util.UnexpectedThrowable;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * MockVFSContext.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MockVFSContext extends AbstractVFSContext
{
   /** The root handler */
   private volatile VirtualFileHandler root;

   /** The root virtual file */
   private volatile VirtualFile rootFile;

   /** When to throw an IOException */
   private volatile String ioException = "";

   /**
    * Create a root mock uri
    *
    * @param name the name
    * @return the uri
    */
   public static final URI createRootMockURI(String name)
   {
      try
      {
         return new URI("mock", "", "/" + name, null);
      }
      catch (URISyntaxException e)
      {
         throw new UnexpectedThrowable("Unexpected", e);
      }
   }

   /**
    * Create mock URL
    *
    * @param uri the uri
    * @return the url
    * @throws MalformedURLException for any error
    */
   public static URL createMockURL(URI uri) throws MalformedURLException
   {
      return new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getRawPath(), MockURLStreamHandler.INSTANCE);
   }

   /**
    * Create a new MockVFSContext.
    *
    * @param name the name
    */
   public MockVFSContext(String name)
   {
      super(createRootMockURI(name));
   }

   public String getName()
   {
      return root.getName();
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

   public VirtualFileHandler getRoot() throws IOException
   {
      throwIOException("getRoot");
      return root;
   }

   public AbstractMockVirtualFileHandler getMockRoot() throws IOException
   {
      return (AbstractMockVirtualFileHandler) root;
   }

   /**
    * Set the root.
    *
    * @param root the root.
    */
   public void setRoot(VirtualFileHandler root)
   {
      this.root = root;
      if (root != null)
         rootFile = root.getVirtualFile();
   }

   @Override
   protected void finalize() throws Throwable
   {
      if (rootFile != null)
         rootFile.close();
      super.finalize();
   }

   /**
    * Get the root URL
    *
    * @return the url
    * @throws MalformedURLException for any error
    */
   public URL getRootURL() throws MalformedURLException
   {
      return createMockURL(getRootURI());
   }
}

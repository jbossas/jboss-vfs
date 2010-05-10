/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.vfs.util;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

/**
 * Lazy input stream.
 *
 * Delaying opening stream from underlying virtual file as long as possible.
 * Won't be opened if not used at all.
 *
 * Synchronization is very simplistic, as it's highly unlikely
 * there will be a lot of concurrent requests.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class LazyInputStream extends InputStream
{
   private VirtualFile file;
   private InputStream stream;

   public LazyInputStream(VirtualFile file)
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      this.file = file;
   }

   /**
    * Open stream.
    *
    * @return file's stream
    * @throws IOException for any IO error
    */
   protected synchronized InputStream openStream() throws IOException
   {
      if (stream == null)
         stream = file.openStream();
      return stream;
   }

   @Override
   public int read() throws IOException
   {
      return openStream().read();
   }

   @Override
   public int read(byte[] b) throws IOException
   {
      return openStream().read(b);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException
   {
      return openStream().read(b, off, len);
   }

   @Override
   public long skip(long n) throws IOException
   {
      return openStream().skip(n);
   }

   @Override
   public int available() throws IOException
   {
      return openStream().available();
   }

   @Override
   public synchronized void close() throws IOException
   {
      if (stream == null)
         return;

      openStream().close();
      stream = null; // reset the stream
   }

   @Override
   public void mark(int readlimit)
   {
      try
      {
         openStream().mark(readlimit);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void reset() throws IOException
   {
      openStream().reset();
   }

   @Override
   public boolean markSupported()
   {
      try
      {
         return openStream().markSupported();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }
}

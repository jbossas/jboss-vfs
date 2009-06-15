/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.virtual.spi.zip.jdk;

import java.io.InputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class IgnoreCloseInputStream extends InputStream
{
   private InputStream delegate;

   IgnoreCloseInputStream(InputStream zis)
   {
      this.delegate = zis;
   }

   public int read() throws IOException
   {
      return delegate.read();
   }

   @Override
   public boolean markSupported()
   {
      return delegate.markSupported();
   }

   @Override
   public void reset() throws IOException
   {
      delegate.reset();
   }

   @Override
   public void mark(int readlimit)
   {
      delegate.mark(readlimit);
   }

   @Override
   public void close() throws IOException
   {
      // no-op
   }

   @Override
   public int available() throws IOException
   {
      return delegate.available();
   }

   @Override
   public long skip(long n) throws IOException
   {
      return delegate.skip(n);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException
   {
      return delegate.read(b, off, len);
   }

   @Override
   public int read(byte[] b) throws IOException
   {
      return delegate.read(b);
   }
}

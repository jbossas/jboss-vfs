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
package org.jboss.virtual.spi.zip;

import java.io.InputStream;
import java.io.IOException;
import java.io.Closeable;
import java.util.Enumeration;

/**
 * Zip file abstraction.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ZipFile extends Closeable
{
   /**
    * Get the input stream for a specific entry.  The caller <b>must</b> close the input stream or
    * file locking/cleanup issues may ensue.
    *
    * @param entry the zip entry
    * @return the input stream
    * @throws IOException if an I/O error occurs
    */
   InputStream getInputStream(ZipEntry entry) throws IOException;

   /**
    * Close the zip file.  May or may not close any {@code InputStream}s associated with this zip file; to be
    * safe, any outstanding {@code InputStream}s returned by {@link #getInputStream(ZipEntry)} must be closed
    * explicitly.
    *
    * @throws IOException if an I/O error occurs
    */
   void close() throws IOException;

   /**
    * Get an enumeration over all the entries.  May fail if the zip file has been closed.
    *
    * @return the enumeration
    */
   Enumeration<? extends ZipEntry> entries();
}
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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * Zip factory.  This is the main entry point into the Zip abstraction layer.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ZipFactory
{

   /**
    * Create a zip entry provider for an input stream.  The provider will provide the means to iterate over a zip
    * file one entry at a time.  The returned provider <b>must</b> be closed or else file locking or cleanup issues
    * may ensue.
    *
    * @param is the input stream to read
    * @return the zip entry provider.
    * @throws IOException if an I/O error occurs
    */
   ZipEntryProvider createProvider(InputStream is) throws IOException;

   /**
    * Create a handle to a randomly-accessible zip file.  The zip file will allow entries to be accessed at random
    * and possibly in parallel.  The returned object <b>must</b> be closed or else file locking or cleanup issues
    * may ensue.
    *
    * @param file the file to read
    * @return the zip file handle
    * @throws IOException if an I/O error occurs
    */
   ZipFile createFile(File file) throws IOException;
}
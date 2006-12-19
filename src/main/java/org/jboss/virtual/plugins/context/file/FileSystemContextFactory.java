/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.virtual.plugins.context.file;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.IOException;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;

/**
 * A file system context factory
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 44217 $
 */
public class FileSystemContextFactory implements VFSContextFactory
{
   /** The protocols supported */
   private static final String[] PROTOCOLS = { "file" };

   public String[] getProtocols()
   {
      return PROTOCOLS;
   }

   public VFSContext getVFS(URL root) throws IOException
   {
      try
      {
         return new FileSystemContext(root);
      }
      catch(URISyntaxException e)
      {
         MalformedURLException ex = new MalformedURLException("non-URI compliant URL");
         ex.initCause(e);
         throw ex;
      }
   }
   public VFSContext getVFS(URI root) throws IOException
   {
      return new FileSystemContext(root);
   }
}

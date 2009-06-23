/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

package org.jboss.virtual.plugins.context.jzip;

import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;

public final class ZipEntryContext extends AbstractVFSContext
{

   public ZipEntryContext(URL url) throws URISyntaxException
   {
      super(url);
   }

   public ZipEntryContext(URL delegatorUrl, DelegatingHandler delegator, URL fileUrl) throws URISyntaxException
   {
      super(delegatorUrl);
   }

   public String getName()
   {
      return null;
   }

   public VirtualFileHandler getRoot() throws IOException
   {
      return null;
   }
}

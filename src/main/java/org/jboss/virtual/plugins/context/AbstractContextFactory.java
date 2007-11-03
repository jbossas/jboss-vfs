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

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.virtual.spi.VFSContextFactory;

/**
 * Abstract context factory.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractContextFactory implements VFSContextFactory
{
   private String[] protocols;

   protected AbstractContextFactory(String... protocols)
   {
      this.protocols = protocols;
   }

   public String[] getProtocols()
   {
      return protocols;
   }

   protected static URL fromVFS(URL url) throws MalformedURLException
   {
      String externalForm = url.toExternalForm();
      if (externalForm.startsWith("vfs"))
         return new URL(externalForm.substring(3));

      return url;
   }

   protected static URI fromVFS(URI uri) throws URISyntaxException
   {
      String scheme = uri.getScheme();
      if (scheme.startsWith("vfs"))
         return new URI(
               scheme.substring(3),
               uri.getUserInfo(),
               uri.getHost(),
               uri.getPort(),
               uri.getPath(),
               uri.getQuery(),
               uri.getFragment()
         );

      return uri;
   }
}

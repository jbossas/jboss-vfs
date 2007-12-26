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

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.IOException;

/**
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class OptionsAwareURI
{
   private static final String NoCopy = "useNoCopyJarHandler=true";

   private static ThreadLocal<Boolean> flag = new ThreadLocal<Boolean>()
   {
      protected Boolean initialValue()
      {
         return Boolean.FALSE;
      }
   };

   public static void set()
   {
      flag.set(true);
   }

   public static void clear()
   {
      flag.set(false);
   }

   public static URL toURL(URL url) throws IOException
   {
      if (flag.get())
      {
         try
         {
            URI uri = toURI(url.toURI());
            return uri.toURL();
         }
         catch (URISyntaxException e)
         {
            throw new IOException(e.getReason());
         }
      }
      else
         return url;
   }

   public static URI toURI(URI uri) throws IOException
   {
      if (flag.get())
      {
         try
         {
            return new URI(uri.toString() + "?" + NoCopy);
//            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getPath(), NoCopy, uri.getFragment());
         }
         catch (URISyntaxException e)
         {
            throw new IOException(e.getReason());
         }
      }
      else
         return uri;
   }
}

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

import org.jboss.virtual.VFSUtils;

/**
 * Flag holder.
 * 
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class OptionsAwareURI
{
   private static final String Copy = VFSUtils.USE_COPY_QUERY + "=true";

   private static ThreadLocal<Boolean> flag = new InheritableThreadLocal<Boolean>()
   {
      protected Boolean initialValue()
      {
         return Boolean.FALSE;
      }

      public String toString()
      {
         Boolean value = get();
         return String.valueOf(value);
      }
   };

   public static void set()
   {
      flag.set(Boolean.TRUE);
   }

   public static boolean get()
   {
      return flag.get();
   }

   public static void clear()
   {
      flag.set(Boolean.FALSE);
   }

   public static URL toURL(URL url) throws IOException
   {
      if (get())
      {
         return new URL(url.toExternalForm() + "?" + Copy);
      }
      else
         return url;
   }

   public static URI toURI(URI uri) throws IOException
   {
      if (get())
      {
         try
         {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getPath(), Copy, uri.getFragment());
         }
         catch (URISyntaxException e)
         {
            throw new IOException(e.getReason());
         }
      }
      else
         return uri;
   }

   public String toString()
   {
      return "flag=" + get();
   }
}

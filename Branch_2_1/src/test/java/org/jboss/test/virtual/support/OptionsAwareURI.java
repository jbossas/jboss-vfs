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
import java.util.Iterator;
import java.util.LinkedList;

import org.jboss.virtual.VFSUtils;

/**
 * Flag holder.
 * 
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @authos <a href="mailto:strukelj@parsek.net">Marko Strukelj</a>
 */
public class OptionsAwareURI
{
   public static final String Copy = VFSUtils.USE_COPY_QUERY + "=true";
   public static final String NoReaper = VFSUtils.NO_REAPER_QUERY + "=true";

   private static ThreadLocal<Boolean> flagCopy = new OAInheritableThreadLocal();
   private static ThreadLocal<Boolean> flagNoReaper = new OAInheritableThreadLocal();

   public static void set(String name)
   {
      if (Copy.equals(name))
         flagCopy.set(Boolean.TRUE);
      else if (NoReaper.equals(name))
         flagNoReaper.set(Boolean.TRUE);
      else
         throw new IllegalArgumentException(name);
   }

   public static boolean get(String name)
   {
      if (Copy.equals(name))
         return flagCopy.get();
      else if (NoReaper.equals(name))
         return flagNoReaper.get();
      else
         throw new IllegalArgumentException(name);
   }

   public static void clear(String name)
   {
      if (Copy.equals(name))
         flagCopy.set(Boolean.FALSE);
      else if (NoReaper.equals(name))
         flagNoReaper.set(Boolean.FALSE);
      else
         throw new IllegalArgumentException(name);
   }

   public static URL toURL(URL url) throws IOException
   {
      LinkedList params = new LinkedList();

      if (get(Copy))
         params.add(Copy);
      if (get(NoReaper))
         params.add(NoReaper);

      if (params.size() == 0)
         return url;

      StringBuilder sb = new StringBuilder(url.toExternalForm());

      // if options are set on URL we overwrite them
      int qpos = sb.indexOf("?");
      if (qpos > 0)
         sb.setLength(qpos);

      Iterator it = params.iterator();
      for (int i=0; it.hasNext(); i++)
      {
         if (i == 0)
            sb.append("?");
         else
            sb.append("&");

         sb.append(it.next());
      }

      return new URL(sb.toString());
   }

   public static URI toURI(URI uri) throws IOException
   {
      try
      {
         return toURL(uri.toURL()).toURI();
      }
      catch (URISyntaxException e)
      {
         throw new IOException(e.getReason());
      }
   }

   public String toString()
   {
      return "flagCopy=" + get(Copy) + ", flagNoReaper=" + get(NoReaper);
   }


   static class OAInheritableThreadLocal extends InheritableThreadLocal<Boolean>
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
   }
}

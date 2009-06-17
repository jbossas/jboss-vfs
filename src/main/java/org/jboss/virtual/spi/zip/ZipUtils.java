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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.virtual.spi.zip.jdk.JDKZipFactory;

/**
 * Zip utils.
 * This is the entry point to the ZipFactory.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ZipUtils
{
   private static final Logger log = Logger.getLogger(ZipUtils.class);
   public static final String KEY = ZipFactory.class.getName();

   private static final Map<String, String> mappings;
   private static ZipFactory factory;

   static
   {
      mappings = new HashMap<String, String>();
      mappings.put("jzipfile", "org.jboss.virtual.spi.zip.jzipfile.JZipFileZipFactory");
      mappings.put("truezip", "org.jboss.virtual.spi.zip.truezip.TrueZipFactory");
   }

   /**
    * Get the zip factory.
    *
    * @return the zip factory
    */
   public static ZipFactory getFactory()
   {
      if (factory == null)
         init();

      return factory;
   }

   /**
    * Initialize zip factory.
    */
   private static void init()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
      {
         factory = createZipFactory();
      }
      else
      {
         PrivilegedAction<ZipFactory> action = new PrivilegedAction<ZipFactory>()
         {
            public ZipFactory run()
            {
               return createZipFactory();
            }
         };
         factory = AccessController.doPrivileged(action);
      }
   }

   /**
    * Instantiate zip factory.
    *
    * @return the zip factory
    */
   private static ZipFactory createZipFactory()
   {
      String factoryClass = System.getProperty(KEY);
      if (factoryClass != null)
      {
         String mappingClass = mappings.get(factoryClass);
         if (mappingClass != null)
            factoryClass = mappingClass;

         try
         {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(factoryClass);
            Object result = clazz.newInstance();
            log.debug("Using custom ZipFactory - " + result.getClass().getName());
            return ZipFactory.class.cast(result);
         }
         catch (Exception e)
         {
            log.warn("Exception instantiating ZipFactory: " + e);
         }
      }
      log.debug("Using default ZipFactory - " + JDKZipFactory.class.getName());
      return new JDKZipFactory();
   }
}
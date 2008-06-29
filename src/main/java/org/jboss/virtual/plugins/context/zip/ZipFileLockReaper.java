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
package org.jboss.virtual.plugins.context.zip;

import java.util.Iterator;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;

import org.jboss.logging.Logger;

/**
 * A monitoring object that closes ZipFiles when they haven't been used for a while
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class ZipFileLockReaper
{
   /** Logger */
   private static final Logger log = Logger.getLogger(ZipFileLockReaper.class);

   /**
    * Time after which unused ZipFiles can be closed. This shouldn't be a large number
    * to ensure smooth releasing of file locks
    */
   private static final int PERIOD = 5000;

   /** Timer thread period */
   private static final int TIMER_PERIOD = 1000;

   /** If timer finds out there haven't been any ZipFiles open for a while it shuts down until some are (re)opened */
   private static final int TIMER_UNUSED_PERIOD = 30000;

   /** There is only one instance that serves all ZipFileWrappers */
   private static ZipFileLockReaper singleton;

   /** A list of monitored ZipFileWrappers */
   private Queue<ZipFileWrapper> monitored = new ConcurrentLinkedQueue<ZipFileWrapper>();

   /** The number of monitored ZipFileWrappers */
   private int monitoredCount = 0;

   /** Timer used for actual reaping - async closure of ZipFiles */
   private Timer timer;

   /** Timestamp of last unregister() call */
   private long lastUsed;

   /**
    * Private constructor - to force retrieval through {@link #getInstance()}
    */
   private ZipFileLockReaper()
   {
   }

   /** Factory method to be used to retrieve reference to ZipFileLockReaper */
   public synchronized static ZipFileLockReaper getInstance()
   {
      if (singleton == null)
         singleton = new ZipFileLockReaper();

      return singleton;
   }

   /**
    * Register a ZipFileWrapper instance with this reaper
    *
    * @param w wrapper to register
    */
   public synchronized void register(ZipFileWrapper w)
   {
      monitored.add(w);
      monitoredCount++;
      if (timer == null)
      {
         timer = new Timer("ZipFile Lock Reaper", true);
         timer.schedule(new ReaperTimerTask(), TIMER_PERIOD, TIMER_PERIOD);
      }
      if (log.isTraceEnabled())
         log.trace("Registered: " + w);
   }

   /**
    * Unregister a ZipFileWrapper instance from this reaper
    *
    * @param w wrapper to unregister
    */
   public synchronized void unregister(ZipFileWrapper w)
   {
      monitored.remove(w);
      monitoredCount--;
      lastUsed = System.currentTimeMillis();
      if (log.isTraceEnabled())
         log.trace("Unregistered: " + w);
   }

   public void deleteFile(ZipFileWrapper zipFileWrapper) throws IOException
   {
      synchronized (ZipFileLockReaper.this)
      {
         Iterator it = monitored.iterator();
         while (it.hasNext())
         {
            ZipFileWrapper w = (ZipFileWrapper) it.next();
            w.deleteFile(zipFileWrapper);
         }
      }
   }

   /** Timer task that does the actual reaping */
   class ReaperTimerTask extends TimerTask
   {
      public void run()
      {
         boolean trace = log.isTraceEnabled();

         if (trace)
            log.trace("Timer called");

         long now = System.currentTimeMillis();
         synchronized (ZipFileLockReaper.this)
         {
            if (monitoredCount == 0)
            {
               if (now - lastUsed > TIMER_UNUSED_PERIOD)
               {
                  timer.cancel();
                  timer = null;
                  if (trace)
                     log.trace("Cancelled the timer");
               }
               return;
            }
         }

         Iterator it = monitored.iterator();
         while (it.hasNext())
         {
            ZipFileWrapper w = (ZipFileWrapper) it.next();

            // stream leak debug
            /*
            Iterator<ZipEntryInputStream> sit = w.streams.iterator();
            while (sit.hasNext())
            {
               ZipEntryInputStream eis = sit.next();
               if (!eis.isClosed())
               {
                  System.out.println("Stream not closed: " + eis.debugCount + " - " + eis);
               }
            }
            */

            if (w.getReferenceCount() <= 0 && now - w.getLastUsed() > PERIOD)
            {
               try
               {
                  w.closeZipFile();
                  if (log.isTraceEnabled())
                     log.trace("Asynchronously closed an unused ZipFile: " + w);
               }
               catch(Exception ignored)
               {
                  log.debug("IGNORING: Failed to close ZipFile: " + w, ignored);
               }
            }
         }
      }
   }
}

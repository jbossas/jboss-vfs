/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.virtual.support.ps.hotdeploy;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.test.virtual.support.ps.MockProfileServiceRepository;
import org.jboss.test.virtual.support.ps.ModificationInfo;
import org.jboss.test.virtual.support.ps.VFSDeployment;

/**
 * A DeploymentScanner built on the ProfileService and MainDeployer. This
 * is really just a simple ExecutorService Runnable that knows nothing
 * about how to detect changed deployers. The ProfileService determines
 * this.
 * 
 * @see MainDeployer
 * @see ProfileService
 * 
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 64997 $
 */
public class HDScanner
   implements Runnable
{
   private static final Logger log = Logger.getLogger(HDScanner.class);
   // Private Data --------------------------------------------------

   /** The ProfileService used to determine modified deployments */
   private MockProfileServiceRepository profileService;

   /** The ExecutorService/ThreadPool for performing scans */
   private ScheduledExecutorService scanExecutor;
   private ScheduledFuture activeScan;
   /** Thread name used when the ScheduledExecutorService is created internally */
   private String scanThreadName = "HDScanner";

   /** Period in ms between deployment scans */
   private long scanPeriod = 5000;
   /** The number of scans that have been done */
   private int scanCount;
   private boolean skipScan;

   // Constructor ---------------------------------------------------
   
   public HDScanner()
   {
      // empty
   }
   
   // Attributes ----------------------------------------------------

   public MockProfileServiceRepository getProfileService()
   {
      return profileService;
   }
   public void setProfileService(MockProfileServiceRepository profileService)
   {
      this.profileService = profileService;
   }

   /**
    * @return Returns the scanExecutor.
    */
   public ScheduledExecutorService getScanExecutor()
   {
      return this.scanExecutor;
   }

   /**
    * @param scanExecutor The scanExecutor to set.
    */
   public void setScanExecutor(ScheduledExecutorService scanExecutor)
   {
      this.scanExecutor = scanExecutor;
   }

   public String getScanThreadName()
   {
      return scanThreadName;
   }

   public void setScanThreadName(String scanThreadName)
   {
      this.scanThreadName = scanThreadName;
   }

   /* (non-Javadoc)
    * @see org.jboss.deployment.scanner.VFSDeploymentScanner#getScanPeriod()
    */
   public long getScanPeriod()
   {
      return scanPeriod;
   }
   /* (non-Javadoc)
    * @see org.jboss.deployment.scanner.VFSDeploymentScanner#setScanPeriod(long)
    */
   public void setScanPeriod(long period)
   {
      this.scanPeriod = period;
   }

   /** 
    * Are deployment scans enabled.
    * 
    * @return whether scan is enabled
    */
   public boolean isScanEnabled()
   {
      return activeScan != null;
   }

   
   public synchronized boolean isSkipScan()
   {
      return skipScan;
   }

   public synchronized void setSkipScan(boolean skipScan)
   {
      this.skipScan = skipScan;
   }

   public synchronized int getScanCount()
   {
      return scanCount;
   }
   public synchronized void resetScanCount()
   {
      this.scanCount = 0;
   }

   /**
    * Enable/disable deployment scans.
    * @param scanEnabled true to enable scans, false to disable.
    */
   public synchronized void setScanEnabled(boolean scanEnabled)
   {
      if( scanEnabled == true && activeScan == null )
      {
         activeScan = this.scanExecutor.scheduleWithFixedDelay(this, 0,
               scanPeriod, TimeUnit.MILLISECONDS);
      }
      else if( scanEnabled == false && activeScan != null )
      {
         activeScan.cancel(true);
         activeScan = null;
      }
   }


   // Operations ----------------------------------------------------
   
   public void start() throws Exception
   {
      // Default to a single thread executor
      if( scanExecutor == null )
      {
         scanExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory()
            {
               public Thread newThread(Runnable r)
               {
                  return new Thread(r, HDScanner.this.getScanThreadName());
               }
            }
        );
      }
      activeScan = scanExecutor.scheduleWithFixedDelay(this, 0,
            scanPeriod, TimeUnit.MILLISECONDS);
   }

   /**
    * Executes scan 
    *
    */
   public void run()
   {
      try
      {
         scan();
      }
      catch(Throwable e)
      {
         log.warn("Scan failed", e);
      }
      finally
      {
         incScanCount();
      }
   }

   public void stop()
   {
      if( activeScan != null )
      {
         activeScan.cancel(true);
         activeScan = null;
      }
   }

   public synchronized void scan() throws Exception
   {
      if(isSkipScan())
         return;

      // Query the ProfileService for deployments
      log.debug("Begin deployment scan");

      
      // Get the modified deployments
      Collection<ModificationInfo> modified  = profileService.getModifiedDeployments();
      for(ModificationInfo info : modified)
      {
         VFSDeployment ctx = info.getDeployment();
         log.debug("Saw modified ctx: "+ctx.getName());
         // TODO: cause the file to be opened/closed?
      }
      log.debug("End deployment scan");
   }

   /**
    * Inc the scanCount and to a notifyAll.
    *
    */
   protected synchronized void incScanCount()
   {
      scanCount ++;
      notifyAll();
   }

   // Private -------------------------------------------------------

}

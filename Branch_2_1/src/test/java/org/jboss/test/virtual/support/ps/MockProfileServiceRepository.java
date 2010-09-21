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
package org.jboss.test.virtual.support.ps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipInputStream;

import org.jboss.logging.Logger;
import org.jboss.test.virtual.support.ps.DeploymentPhase;
import org.jboss.test.virtual.support.ps.ModificationInfo;
import org.jboss.test.virtual.support.ps.VFSDeployment;
import org.jboss.test.virtual.support.ps.VFSDeploymentFactory;
import org.jboss.test.virtual.support.ps.ModificationInfo.ModifyStatus;
import org.jboss.util.file.Files;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * An implementation of DeploymentRepository that relies on java
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class MockProfileServiceRepository
{
   private static final Logger log = Logger.getLogger(MockProfileServiceRepository.class);

   /** The server root container the deployments */
   private File root;
   /** The application phase deployments dir */
   private File[] applicationDirs;
   private LinkedHashMap<String,VFSDeployment> applicationCtxs = new LinkedHashMap<String,VFSDeployment>();
   private Set<String> lockedApps = Collections.synchronizedSet(new HashSet<String>());
   /** The last time the profile was modified */
   private long lastModified;
   /** A lock for the hot deployment/{@link #getModifiedDeployments()} */
   private ReentrantReadWriteLock contentLock = new ReentrantReadWriteLock(true);
   /** Should an attempt to overwrite existing content fail in {@link #addDeploymentContent(String, ZipInputStream, DeploymentPhase)}*/
   private boolean failIfAlreadyExists = false;

   public MockProfileServiceRepository(File root, URI[] appURIs)
   {
      this.root = root;
      this.setApplicationURIs(appURIs);
   }

   public URI[] getApplicationURIs()
   {
      URI[] appURIs = new URI[applicationDirs.length];
      for (int n = 0; n < applicationDirs.length; n ++)
      {
         File applicationDir = applicationDirs[n];
         appURIs[n] = applicationDir.toURI();
      }
      return appURIs;
   }
   public void setApplicationURIs(URI[] uris)
   {
      applicationDirs = new File[uris.length];
      for (int n = 0; n < uris.length; n ++)
      {
         URI uri = uris[n];
         applicationDirs[n] = new File(uri);
      }
   }

   public boolean exists()
   {
      return root.exists();
   }

   public long getLastModified()
   {
      return this.lastModified;
   }

   public URI getDeploymentURI(DeploymentPhase phase)
   {
      URI uri = null;
      switch( phase )
      {
         case BOOTSTRAP:
            uri = this.getBootstrapURI();
            break;
         case DEPLOYER:
            break;
         case APPLICATION:
            uri = this.getApplicationURI();
            break;
      }
      return uri;
   }
   public void setDeploymentURI(URI uri, DeploymentPhase phase)
   {
      switch( phase )
      {
         case BOOTSTRAP:
            this.setBootstrapURI(uri);
            break;
         case DEPLOYER:
            this.setDeployersURI(uri);
            break;
         case APPLICATION:
            this.setApplicationURIs(new URI[]{uri});
            break;
      }
   }
   public Set<String> getDeploymentNames()
   {
      HashSet<String> names = new HashSet<String>();
      names.addAll(applicationCtxs.keySet());
      return names;
   }
   public Set<String> getDeploymentNames(DeploymentPhase phase)
   {
      HashSet<String> names = new HashSet<String>();
      switch( phase )
      {
         case BOOTSTRAP:
            break;
         case DEPLOYER:
            break;
         case APPLICATION:
            names.addAll(this.applicationCtxs.keySet());
            break;
      }
      return names;      
   }

   /**
    * 
    */
   public void addDeploymentContent(String name, InputStream contentIS, DeploymentPhase phase)
      throws IOException
   {
      boolean trace = log.isTraceEnabled();
      // Suspend hot deployment checking
      if( trace )
         log.trace("Aquiring content read lock");
      contentLock.writeLock().lock();
      try
      {
         // Write the content out
         File contentRoot = getPhaseDir(phase);
         if(contentRoot == null)
            throw new FileNotFoundException("Failed to obtain content dir for phase: "+phase);
         File contentFile = new File(contentRoot, name);
         if(failIfAlreadyExists && contentFile.exists())
            throw new SyncFailedException("Deployment content already exists: "+contentFile.getAbsolutePath());
         FileOutputStream fos = new FileOutputStream(contentFile);
         try
         {
            byte[] tmp = new byte[4096];
            int read;
            while((read = contentIS.read(tmp)) > 0)
            {
               if (trace)
                  log.trace("write, " + read);
               fos.write(tmp, 0, read);
            }
            fos.flush();
         }
         finally
         {
            try
            {
               fos.close();
            }
            catch (IOException ignored)
            {
            }
         }
         //contentIS.close();

         // Lock the content
         lockDeploymentContent(name, phase);
      }
      finally
      {
         // Allow hot deployment checking
         contentLock.writeLock().unlock();
         if(trace)
            log.trace("Released content write lock");
      }
   }

   public VirtualFile getDeploymentContent(String vfsPath, DeploymentPhase phase)
         throws IOException
   {
      URI rootURI = this.getDeploymentURI(phase);
      VirtualFile root = VFS.getRoot(rootURI);
      VirtualFile content = root.getChild(vfsPath);
      if(content == null)
         throw new FileNotFoundException(vfsPath+" not found under root: "+rootURI);
      return content;
   }

   public void lockDeploymentContent(String vfsPath, DeploymentPhase phase)
   {
      lockedApps.add(vfsPath);
   }

   public void unlockDeploymentContent(String vfsPath, DeploymentPhase phase)
   {
      lockedApps.remove(vfsPath);
   }

   public void acquireDeploymentContentLock()
   {
      contentLock.writeLock().lock();
      if( log.isTraceEnabled() )
         log.trace("acquireDeploymentContentLock, have write lock"); 
   }
   public void releaseDeploymentContentLock()
   {
      contentLock.writeLock().unlock();
      if( log.isTraceEnabled() )
         log.trace("releaseDeploymentContentLock, gave up write lock");       
   }

   public void addDeployment(String vfsPath, VFSDeployment d, DeploymentPhase phase)
      throws Exception
   {
      switch( phase )
      {
         case BOOTSTRAP:
            this.addBootstrap(vfsPath, d);
            break;
         case DEPLOYER:
            this.addDeployer(vfsPath, d);
            break;
         case APPLICATION:
            this.addApplication(vfsPath, d);
            break;
         case APPLICATION_TRANSIENT:
            this.addApplication(vfsPath, d);
            break;
      }
   }

   public void updateDeployment(String vfsPath, VFSDeployment d, DeploymentPhase phase)
      throws Exception
   {
   }
   public VFSDeployment getDeployment(String name, DeploymentPhase phase)
      throws Exception
   {
      VFSDeployment ctx = null;
      if( phase == null )
      {
         try
         {
            if( ctx == null )
               ctx = this.getApplication(name);
         }
         catch(Exception ignore)
         {
         }
      }
      else
      {
         switch( phase )
         {
            case BOOTSTRAP:
               ctx = this.getBootstrap(name);
               break;
            case DEPLOYER:
               break;
            case APPLICATION:
               ctx = this.getApplication(name);
               break;
         }
      }
      // Make sure we don't return null
      if( ctx == null )
         throw new Exception("name="+name+", phase="+phase);
      return ctx;
   }

   public Collection<VFSDeployment> getDeployments()
   {
      HashSet<VFSDeployment> deployments = new HashSet<VFSDeployment>();
      deployments.addAll(this.applicationCtxs.values());
      return Collections.unmodifiableCollection(deployments);
   }

   /**
    * Scan the applications for changes.
    */
   public synchronized Collection<ModificationInfo> getModifiedDeployments()
      throws Exception
   {
      ArrayList<ModificationInfo> modified = new ArrayList<ModificationInfo>();
      Collection<VFSDeployment> apps = getApplications();
      boolean trace = log.isTraceEnabled();
      if( trace )
         log.trace("Checking applications for modifications");
      if( trace )
         log.trace("Aquiring content read lock");
      contentLock.readLock().lock();
      try
      {
         if( apps != null )
         {
            Iterator<VFSDeployment> iter = apps.iterator();
            while( iter.hasNext() )
            {
               VFSDeployment ctx = iter.next();
               VirtualFile root = ctx.getRoot();
               // See if this file is locked
               if(this.lockedApps.contains(root.getPathName()))
               {
                  if(trace)
                     log.trace("Ignoring locked application: "+root);
                  continue;
               }
               Long rootLastModified = root.getLastModified();
               String name = root.getPathName();
               // Check for removal
               if( root.exists() == false )
               {
                  ModificationInfo info = new ModificationInfo(ctx, rootLastModified, ModifyStatus.REMOVED);
                  modified.add(info);
                  iter.remove();
                  if( trace )
                     log.trace(name+" was removed");
               }
               // Check for modification
               else if( root.hasBeenModified() )
               {
                  if( trace )
                     log.trace(name+" was modified: "+rootLastModified);
                  // Need to create a duplicate ctx
                  VFSDeployment ctx2 = loadDeploymentData(root);
                  ModificationInfo info = new ModificationInfo(ctx2, rootLastModified, ModifyStatus.MODIFIED);
                  modified.add(info);
               }
               // TODO: this could check metadata files modifications as well
            }
            // Now check for additions
            for (File applicationDir : applicationDirs)
            {
               VirtualFile deployDir = VFS.getRoot(applicationDir.toURI());
               List<VirtualFile> children = deployDir.getChildren();
               for(VirtualFile vf : children)
               {
                  URI uri = vf.toURI();
                  if( applicationCtxs.containsKey(uri.toString()) == false )
                  {
                     VFSDeployment ctx = loadDeploymentData(vf);
                     ModificationInfo info = new ModificationInfo(ctx, vf.getLastModified(), ModifyStatus.ADDED);
                     modified.add(info);
                     applicationCtxs.put(vf.toURI().toString(), ctx);
                  }
               }
            }
         }
      }
      finally
      {
         contentLock.readLock().unlock();
         if( trace )
            log.trace("Released content read lock");
      }

      if(modified.size() > 0)
         lastModified = System.currentTimeMillis();
      return modified;
   }

   public Collection<VFSDeployment> getDeployments(DeploymentPhase phase)
      throws Exception
   {
      Collection<VFSDeployment> ctxs = null;
      switch( phase )
      {
         case BOOTSTRAP:
            ctxs = this.getBootstraps();
            break;
         case DEPLOYER:
            break;
         case APPLICATION:
            ctxs = this.getApplications();
            break;
      }
      return ctxs;
   }

   public VFSDeployment removeDeployment(String name, DeploymentPhase phase)
      throws Exception
   {
      VFSDeployment ctx = null;
      switch( phase )
      {
         case BOOTSTRAP:
            ctx = this.removeBootstrap(name);
            break;
         case DEPLOYER:
            ctx = this.removeDeployer(name);
            break;
         case APPLICATION:
            ctx = this.removeApplication(name);
            break;
      }
      return ctx;
   }
   public String toString()
   {
      StringBuilder tmp = new StringBuilder(super.toString());
      tmp.append("(root=");
      tmp.append(root);
      tmp.append(")");
      return tmp.toString();
   }

   /**
    * Create a profile deployment repository
    * 
    * @throws IOException
    */
   public void create() throws Exception
   {
      File profileRoot = root;

      // server/{name}/deploy
      for (File applicationDir : applicationDirs)
      {
         if(applicationDir.exists() == true)
            continue;
         if( applicationDir.mkdirs() == false )
            throw new IOException("Failed to create profile deploy dir: "+applicationDir);
      }
   }

   /**
    * Load the profile deployments
    * 
    * @throws IOException
    * @throws NoSuchProfileException
    */
   public void load() throws Exception
   {
      File profileRoot = root;
      if( profileRoot.exists() == false )
         throw new Exception("Profile root does not exists: "+profileRoot);

      // server/{name}/deploy
      for (File applicationDir : applicationDirs)
      {
         if( applicationDir.exists() == false )
            throw new FileNotFoundException("Profile contains no deploy dir: "+applicationDir);
      }

      for (File applicationDir : applicationDirs)
      {
         VFS deployVFS = VFS.getVFS(applicationDir.toURI());
         loadApplications(deployVFS.getRoot());
      }
      this.lastModified = System.currentTimeMillis();
   }

   /**
    * Remove the contents of the profile repository
    * @throws IOException
    * @throws NoSuchProfileException
    */
   public void remove() throws IOException
   {
      File profileRoot = root;
      Files.delete(profileRoot);
   }

   protected void addBootstrap(String vfsPath, VFSDeployment ctx)
      throws Exception
   {
   }


   protected void addDeployer(String vfsPath, VFSDeployment ctx)
      throws Exception
   {
   }

   protected void addApplication(String vfsPath, VFSDeployment ctx)
      throws Exception
   {
      this.applicationCtxs.put(vfsPath, ctx);
   }

   protected VFSDeployment getBootstrap(String vfsPath)
      throws Exception
   {
      VFSDeployment ctx = null;
      return ctx;
   }

   protected Collection<VFSDeployment> getBootstraps()
      throws Exception
   {
      Collection<VFSDeployment> ctxs = null;
      return ctxs;
   }

   protected URI getPhaseURI(DeploymentPhase phase)
   {
      URI uri = null;
      switch( phase )
      {
         case BOOTSTRAP:
            uri = getBootstrapURI();
            break;
         case DEPLOYER:
            break;
         case APPLICATION:
            uri = getApplicationURI();
            break;
         case APPLICATION_TRANSIENT:
            // TODO
            break;
      }
      return uri;
   }

   protected File getPhaseDir(DeploymentPhase phase)
   {
      File dir = null;
      switch( phase )
      {
         case BOOTSTRAP:
            break;
         case DEPLOYER:
            break;
         case APPLICATION:
            dir = applicationDirs[0];
            break;
         case APPLICATION_TRANSIENT:
            // TODO
            break;
      }
      return dir;
   }

   protected URI getBootstrapURI()
   {
      return null;
   }
   protected URI getApplicationURI()
   {
      File applicationDir = applicationDirs[0];
      return applicationDir.toURI();
   }

   protected VFSDeployment getApplication(String vfsPath)
      throws Exception
   {
      VFSDeployment ctx = applicationCtxs.get(vfsPath);
      if( ctx == null )
      {
         // Try to find the simple name
         log.debug("Failed to find application for: "+vfsPath+", scanning for simple name");
         for(VFSDeployment deployment : applicationCtxs.values())
         {
            log.info("Checking: "+deployment.getSimpleName());
            if(deployment.getSimpleName().equals(vfsPath))
            {
               log.debug("Matched to simple name of deployment:"+deployment);
               ctx = deployment;
               break;
            }
         }
         if(ctx == null)
            throw new Exception(vfsPath);
      }
      return ctx;
   }

   protected Collection<VFSDeployment> getApplications()
      throws Exception
   {
      return applicationCtxs.values();
   }

   protected VFSDeployment removeBootstrap(String vfsPath) throws IOException
   {
      VFSDeployment vfsDeployment = null;
      return vfsDeployment;
   }

   // this is an infinite loop
   protected VFSDeployment removeDeployer(String vfsPath) throws IOException
   {
      VFSDeployment vfsDeployment = null;
      return vfsDeployment;
   }
   protected VFSDeployment removeApplication(String vfsPath) throws IOException
   {
      VFSDeployment vfsDeployment = applicationCtxs.get(vfsPath);
      if(vfsDeployment == null)
         throw new IllegalStateException("Deployment not found: " + vfsPath);
      // Find the application dir
      File applicationDir = applicationDirs[0];
      File deploymentFile = new File(applicationDir, vfsDeployment.getSimpleName());
      if( Files.delete(deploymentFile) == false )
         throw new IOException("Failed to delete: "+deploymentFile);
      return this.applicationCtxs.remove(vfsPath);
   }
   protected void setBootstrapURI(URI uri)
   {
   }
   protected void setDeployersURI(URI uri)
   {
   }

   /**
    * Load all the applications under the applicationDir.
    * 
    * @param applicationDir
    * @throws IOException
    */
   private void loadApplications(VirtualFile applicationDir)
      throws IOException
   {
      List<VirtualFile> children = applicationDir.getChildren();
      for(VirtualFile vf : children)
      {
         VFSDeployment vfCtx = loadDeploymentData(vf);
         applicationCtxs.put(vfCtx.getName(), vfCtx);         
      }
   }

   /**
    * TODO: this could be dropped since the serialize aspect loads the data
    * @param file
    * @return the deployment
    */
   private VFSDeployment loadDeploymentData(VirtualFile file)
   {
      // Check for a persisted context
      // Load the base deployment
      VFSDeployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(file);
      log.debug("Created deployment: "+deployment);
      return deployment;
   }

}

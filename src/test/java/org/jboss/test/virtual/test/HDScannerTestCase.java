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
package org.jboss.test.virtual.test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jboss.test.virtual.support.ps.DeploymentPhase;
import org.jboss.test.virtual.support.ps.MockProfileServiceRepository;
import org.jboss.test.virtual.support.ps.VFSDeployment;
import org.jboss.test.virtual.support.ps.VFSDeploymentFactory;
import org.jboss.test.virtual.support.ps.hotdeploy.HDScanner;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class HDScannerTestCase extends OSAwareVFSTest
{

   public HDScannerTestCase(String name, boolean forceCopy)
   {
      super(name, forceCopy);
   }

   public HDScannerTestCase(String name)
   {
      super(name);
   }

   public void testDeleteWhileScanning()
      throws Exception
   {
      // Create a root in the system tmp dir
      File root = File.createTempFile("testDeleteWhileScanning", ".root");
      root.delete();
      assertTrue(root.mkdir());
      getLog().info("Created root dir: "+root);
      File deployDir = new File(root, "deploy");
      assertTrue(deployDir.mkdir());
      // Remove any existing content
      for(File f : deployDir.listFiles())
         f.delete();

      URI[] appURIs = {deployDir.toURI()};
      MockProfileServiceRepository repository = new MockProfileServiceRepository(root, appURIs);
      HDScanner scanner = new HDScanner();
      scanner.setProfileService(repository);
      scanner.setScanPeriod(1000);

      VFS vfs = VFS.getVFS(root.toURI());
      File archive = generateArchive(deployDir);
      VirtualFile archiveVF = vfs.getChild("deploy/"+archive.getName());      
      VFSDeployment vfsd = VFSDeploymentFactory.getInstance().createVFSDeployment(archiveVF);
      repository.addDeployment(archiveVF.toURI().toString(), vfsd, DeploymentPhase.APPLICATION);

      getLog().debug("Waiting for 10 scans...");
      scanner.start();
      while(scanner.getScanCount() < 10)
      {
         Thread.sleep(1000);
         // Update the archive last modifed time
         archive.setLastModified(System.currentTimeMillis());
      }
      getLog().info("Trying to remove: "+archive.getAbsolutePath());
      assertTrue(archive.delete());
      getLog().info("Deleted deployed archive");
      scanner.stop();
   }

   protected File generateArchive(File deployDir)
      throws Exception
   {
      File tmpJar = File.createTempFile("archive", ".jar", deployDir);
      FileOutputStream fos = new FileOutputStream(tmpJar);
      JarOutputStream jos = new JarOutputStream(fos);
      jos.setLevel(5);
      jos.setComment(tmpJar.getName());
      JarEntry je = new JarEntry("META-INF/");
      je.setComment("META-INF directory");
      je.setTime(System.currentTimeMillis());
      jos.putNextEntry(je);
      je = new JarEntry("META-INF/metadata.xml");
      StringBuffer contents = new StringBuffer();
      contents.append("<metadata name='"+tmpJar.getName()+"'/>");
      je.setSize(contents.length());
      je.setTime(System.currentTimeMillis()+1);
      je.setMethod(JarEntry.DEFLATED);
      jos.putNextEntry(je);
      jos.write(contents.toString().getBytes());
      jos.closeEntry();
      jos.close();
      fos.close();
      assertTrue(tmpJar.exists());
      return tmpJar;
   }
}

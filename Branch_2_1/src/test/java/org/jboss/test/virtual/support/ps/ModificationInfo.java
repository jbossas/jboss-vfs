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

import java.io.Serializable;

import org.jboss.test.virtual.support.ps.VFSDeployment;

/**
 * Represents a modified deployment returned from the modified deployments scan.
 *  
 * @see Profile#getModifiedDeployments()
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 63730 $
 */
public class ModificationInfo implements Serializable
{
   private final static long serialVersionUID = 1;

   public enum ModifyStatus {ADDED, MODIFIED, REMOVED};
   private VFSDeployment deployment;
   private long lastModified;
   private ModifyStatus status;

   public ModificationInfo(VFSDeployment deployment, long lastModified, ModifyStatus status)
   {
      this.deployment = deployment;
      this.lastModified = lastModified;
      this.status = status;
   }

   public VFSDeployment getDeployment()
   {
      return deployment;
   }

   public long getLastModified()
   {
      return lastModified;
   }

   public ModifyStatus getStatus()
   {
      return status;
   }

}


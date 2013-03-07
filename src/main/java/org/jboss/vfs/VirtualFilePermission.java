/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.vfs;

import java.io.FilePermission;
import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * A permission to a file on the virtual file system.
 *
 * @see FilePermission
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class VirtualFilePermission extends Permission {
    private final FilePermission filePermission;

    /**
     * Construct a new instance.
     *
     * @param path the path
     * @param actions the actions to grant
     */
    public VirtualFilePermission(final String path, final String actions) {
        super(path);
        filePermission = new FilePermission(path, actions);
    }

    public boolean implies(final Permission permission) {
        return permission instanceof VirtualFilePermission && implies((VirtualFilePermission) permission);
    }

    public boolean implies(final VirtualFilePermission permission) {
        return permission != null && filePermission.implies(permission.filePermission);
    }

    public boolean equals(final Object permission) {
        return permission instanceof VirtualFilePermission && equals((VirtualFilePermission) permission);
    }

    public boolean equals(final Permission permission) {
        return permission instanceof VirtualFilePermission && equals((VirtualFilePermission) permission);
    }

    public boolean equals(final VirtualFilePermission permission) {
        return permission != null && filePermission.equals(permission.filePermission);
    }

    public int hashCode() {
        return filePermission.hashCode();
    }

    public String getActions() {
        return filePermission.getActions();
    }

    public PermissionCollection newPermissionCollection() {
        return new VirtualFilePermissionCollection(filePermission.newPermissionCollection());
    }

    FilePermission getFilePermission() {
        return filePermission;
    }

    Object writeReplace() {
        return new Serialized(getName(), getActions());
    }

    public static final class Serialized implements Serializable {

        private static final long serialVersionUID = 1L;

        final String path;
        final String actions;

        public Serialized(final String path, final String actions) {
            this.path = path;
            this.actions = actions;
        }

        Object readResolve() {
            return new VirtualFilePermission(path, actions);
        }
    }
}

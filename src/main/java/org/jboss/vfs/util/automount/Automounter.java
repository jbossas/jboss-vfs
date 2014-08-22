/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.vfs.util.automount;

import static org.jboss.vfs.VFSMessages.MESSAGES;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSLogger;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.PathTokenizer;

/**
 * Utility used to manage mounting Virtual FileSystems.
 *
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class Automounter {
    /* Root entry in the tree. */
    private static final RegistryEntry rootEntry = new RegistryEntry();

    /* Map of owners and their references */
    private static final ConcurrentMap<MountOwner, Set<RegistryEntry>> ownerReferences = new ConcurrentHashMap<MountOwner, Set<RegistryEntry>>();

    /* Provider of temp files/directories*/
    private static TempFileProvider tempFileProvider;

    /**
     * Private constructor
     */
    private Automounter() {
    }

    /**
     * Mount provided {@link VirtualFile} (if not mounted) and set the owner to be the provided target.  (Self owned mount)
     *
     * @param target       VirtualFile to mount
     * @param mountOptions optional configuration to use for mounting
     * @throws IOException when the target can not be mounted.
     */
    public static void mount(VirtualFile target, MountOption... mountOptions) throws IOException {
        mount(new VirtualFileOwner(target), target, mountOptions);
    }

    /**
     * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
     *
     * @param owner        Object that owns the reference to the mount
     * @param target       VirtualFile to mount
     * @param mountOptions optional configuration to use for mounting
     * @throws IOException when the target can not be mounted.
     */
    public static void mount(Object owner, VirtualFile target, MountOption... mountOptions) throws IOException {
        mount(new SimpleMountOwner(owner), target, mountOptions);
    }

    /**
     * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
     *
     * @param owner        VirtualFile that owns the reference to the mount
     * @param target       VirtualFile to mount
     * @param mountOptions optional configuration to use for mounting
     * @throws IOException when the target can not be mounted.
     */
    public static void mount(VirtualFile owner, VirtualFile target, MountOption... mountOptions) throws IOException {
        mount(new VirtualFileOwner(owner), target, mountOptions);
    }

    /**
     * Mount provided {@link VirtualFile} (if not mounted) and add an owner entry.  Also creates a back-reference to from the owner to the target.
     *
     * @param owner        MountOwner that owns the reference to the mount
     * @param target       VirtualFile to mount
     * @param mountOptions optional configuration to use for mounting
     * @throws IOException when the target can not be mounted
     */
    public static void mount(MountOwner owner, VirtualFile target, MountOption... mountOptions) throws IOException {
        final RegistryEntry targetEntry = getEntry(target);
        targetEntry.mount(target, getMountConfig(mountOptions));
        targetEntry.inboundReferences.add(owner);
        ownerReferences.putIfAbsent(owner, new HashSet<RegistryEntry>());
        ownerReferences.get(owner).add(targetEntry);
    }

    /**
     * Creates a MountConfig and applies the provided mount options
     *
     * @param mountOptions options to use for mounting
     * @return a MountConfig
     */
    private static MountConfig getMountConfig(MountOption[] mountOptions) {
        final MountConfig config = new MountConfig();
        for (MountOption option : mountOptions) {
            option.applyTo(config);
        }
        return config;
    }

    /**
     * Add handle to owner, to be auto closed.
     *
     * @param owner  the handle owner
     * @param handle the handle
     * @return add result
     */
    public static boolean addHandle(VirtualFile owner, Closeable handle) {
        RegistryEntry entry = getEntry(owner);
        return entry.handles.add(handle);
    }

    /**
     * Remove handle from owner.
     *
     * @param owner  the handle owner
     * @param handle the handle
     * @return remove result
     */
    public static boolean removeHandle(VirtualFile owner, Closeable handle) {
        RegistryEntry entry = getEntry(owner);
        return entry.handles.remove(handle);
    }

    /**
     * Cleanup all references from the owner.  Cleanup any mounted entries that become un-referenced in the process.
     *
     * @param owner {@link Object} to cleanup references for
     */
    public static void cleanup(Object owner) {
        cleanup(new SimpleMountOwner(owner));
    }

    /**
     * Cleanup all references from the owner.  Cleanup any mounted entries that become un-referenced in the process.
     *
     * @param owner {@link Object} to cleanup references for
     */
    public static void cleanup(VirtualFile owner) {
        cleanup(new VirtualFileOwner(owner));
    }

    /**
     * Cleanup all references from the {@link MountOwner}.  Cleanup any mounted entries that become un-referenced in the process.
     *
     * @param owner {@link MountOwner} to cleanup references for
     */
    public static void cleanup(MountOwner owner) {
        final Set<RegistryEntry> references = ownerReferences.remove(owner);
        if (references != null) {
            for (RegistryEntry entry : references) {
                entry.removeInboundReference(owner);
            }
        }
        owner.onCleanup();
    }

    /**
     * Determines whether a target {@link VirtualFile} is mounted.
     *
     * @param target target to check
     * @return true if mounted, false otherwise
     */
    public static boolean isMounted(VirtualFile target) {
        return getEntry(target).isMounted();
    }

    /**
     * Get the entry from the tree creating the entry if not present.
     *
     * @param virtualFile entry's owner file
     * @return registry entry
     */
    static RegistryEntry getEntry(VirtualFile virtualFile) {
        if (virtualFile == null) {
            throw MESSAGES.nullArgument("VirutalFile");
        }
        return rootEntry.find(virtualFile);
    }

    private static TempFileProvider getTempFileProvider() throws IOException {
        if (tempFileProvider == null) { tempFileProvider = TempFileProvider.create("automount", Executors.newScheduledThreadPool(2)); }
        return tempFileProvider;
    }

    static class RegistryEntry {
        private final ConcurrentMap<String, RegistryEntry> children = new ConcurrentHashMap<String, RegistryEntry>();

        private final Set<MountOwner> inboundReferences = new HashSet<MountOwner>();

        private final List<Closeable> handles = new LinkedList<Closeable>();

        private final AtomicBoolean mounted = new AtomicBoolean();

        private void mount(VirtualFile target, MountConfig mountConfig) throws IOException {
            if (mounted.compareAndSet(false, true)) {
                if (target.isFile()) {
                    VFSLogger.ROOT_LOGGER.debugf("Automounting: %s with options %s", target, mountConfig);

                    final TempFileProvider provider = getTempFileProvider();
                    if (mountConfig.mountExpanded()) {
                        if (mountConfig.copyTarget()) { handles.add(VFS.mountZipExpanded(target, target, provider)); } else {
                            handles.add(VFS.mountZipExpanded(target.getPhysicalFile(), target, provider));
                        }
                    } else {
                        if (mountConfig.copyTarget()) { handles.add(VFS.mountZip(target, target, provider)); } else {
                            handles.add(VFS.mountZip(target.getPhysicalFile(), target, provider));
                        }
                    }
                }
            }
        }

        private void removeInboundReference(MountOwner owner) {
            inboundReferences.remove(owner);
            if (inboundReferences.isEmpty()) {
                cleanup();
            }
        }

        void cleanup() {
            if (mounted.compareAndSet(true, false)) {
                VFSUtils.safeClose(handles);
                handles.clear();

                final Collection<RegistryEntry> entries = getEntriesRecursive();
                for (RegistryEntry entry : entries) {
                    entry.cleanup();
                }
            }
        }

        private boolean isMounted() {
            return mounted.get();
        }

        private RegistryEntry find(VirtualFile file) {
            return find(PathTokenizer.getTokens(file.getPathName()));
        }

        private RegistryEntry find(List<String> path) {
            if (path.isEmpty()) {
                return this;
            }
            final String current = path.remove(0);
            children.putIfAbsent(current, new RegistryEntry());
            final RegistryEntry childEntry = children.get(current);
            return childEntry.find(path);
        }

        private Collection<RegistryEntry> getEntriesRecursive() {
            final List<RegistryEntry> allHandles = new LinkedList<RegistryEntry>();
            collectEntries(this, allHandles);
            return allHandles;
        }

        private void collectEntries(RegistryEntry registryEntry, List<RegistryEntry> entries) {
            for (RegistryEntry childEntry : registryEntry.children.values()) {
                collectEntries(childEntry, entries);
                entries.add(childEntry);
            }
        }
    }
}

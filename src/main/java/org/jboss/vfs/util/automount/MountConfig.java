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

/**
 * Configuration used to control the auto-mount behavior.
 *
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
class MountConfig {
    private boolean mountExpanded;

    private boolean copyTarget;

    /**
     * Should the archive be mounted as an expanded zip filesystem.  Defaults to false.
     *
     * @return true if it should be expanded
     */
    boolean mountExpanded() {
        return mountExpanded;
    }

    /**
     * Set whether the mount should be an expanded zip filesystem.
     *
     * @param mountExpanded the boolean value to set it to
     */
    void setMountExpanded(boolean mountExpanded) {
        this.mountExpanded = mountExpanded;
    }

    /**
     * Should the archive be copied to a temporary location before being mounted.
     * Defaults to false.
     *
     * @return true if the archive should be copied before being mounted
     */
    boolean copyTarget() {
        return copyTarget;
    }

    /**
     * Set whether the archive should be copied before being mounted.
     *
     * @param copyTarget the boolean value to set it to
     */
    void setCopyTarget(boolean copyTarget) {
        this.copyTarget = copyTarget;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("MountConfig[Expanded: ").append(mountExpanded).append(", Copy: ").append(
                copyTarget).append("]").toString();
    }

}

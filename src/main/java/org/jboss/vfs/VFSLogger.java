package org.jboss.vfs;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */

@MessageLogger(projectCode = "VFS")
public interface VFSLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    VFSLogger ROOT_LOGGER = Logger.getMessageLogger(VFSLogger.class, VFSLogger.class.getPackage().getName());

    @LogMessage(level = WARN)
    @Message(id = 1, value = "A VFS mount (%s) was leaked!")
    void vfsMountLeaked(VirtualFile mountPoint, @Cause Throwable cause);


}

/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * Copyright: 2005
 *     The copyright to this program is held by it's authors.
 *
 */
package org.crosswire.common.activate;

/**
 * Enumeration of how memory is returned.
 * 
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's authors.
 * @author Joe Walker [joe at eireneh dot com]
 */
public enum Kill {
    /** Try as hard as possible to conserve memory */
    EVERYTHING  {
        @Override
        public void reduceMemoryUsage() {
            Activator.deactivateAll();
        }
    },

    /** Reduce memory usage, but only where sensible */
    LEAST_USED,

    /** Reduce memory usage, but only if we really need to */
    ONLY_IF_TIGHT;

    public void reduceMemoryUsage() {
        throw new IllegalArgumentException("Not implemented");
    }
}

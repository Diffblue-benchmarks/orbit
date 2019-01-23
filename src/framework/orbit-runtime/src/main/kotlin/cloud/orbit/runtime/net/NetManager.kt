/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

class NetManager {
    val localNodeManipulator = NodeManipulator()
    val localNode get() = localNodeManipulator.nodeInfo
}
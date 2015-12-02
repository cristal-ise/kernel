/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
/**
 * Export and Import of Items.
 * 
 * <p>{@link TransferItem} provides a mechanism for marshalling all of the 
 * C2KLocalObjects in an Item to XML and exporting them to disk, and then 
 * importing that Item on another server. {@link TransferSet} can export many 
 * Items at a time and preserve their domain paths. 
 * 
 * <p>This package is not currently used, as with the previous system key 
 * integer sequence it was not possible to import collections onto other servers
 * but now Items are identified using UUIDs, this may now converge with the 
 * module mechanism.
 * 
 */

package org.cristalise.kernel.entity.transfer;
/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.impl.trans.r;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.impl.trans.ReadFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * List VPP reader, allowing read of all the elements
 *
 * @param <D> Specific DataObject derived type, that is handled by this reader
 */
@Beta
public interface ListVppReader<D extends DataObject & Identifiable<K>, K extends Identifier<D>> extends VppReader<D> {

    /**
     * Read all elements in this list
     *
     * @param id Wildcarded identifier of list managed by this reader
     *
     * @return List of all entries in this list
     * @throws ReadFailedException if read was unsuccessful
     */
    @Nonnull
    List<D> readList(@Nonnull final InstanceIdentifier<D> id) throws ReadFailedException;
}

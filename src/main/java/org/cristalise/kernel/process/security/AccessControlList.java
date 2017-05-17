package org.cristalise.kernel.process.security;

import org.cristalise.kernel.utils.CastorArrayList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @RequiredArgsConstructor @Getter @Setter
public class AccessControlList extends CastorArrayList<ReadAccessControl> {

    @NonNull
    String clusterPath;
}

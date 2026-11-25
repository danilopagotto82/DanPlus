package com.nuvio.tv.data.mediaserver

import com.nuvio.tv.data.mediaserver.silo.SiloNativeProvider
import com.nuvio.tv.data.mediaserver.silo.SiloProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiloStreamBadgeTest {
    @Test fun onlyTheRegisteredSiloProvidersReceiveTheBadge() {
        assertTrue(isSiloStreamProvider(SiloProvider.ID))
        assertTrue(isSiloStreamProvider(SiloNativeProvider.ID))
        assertFalse(isSiloStreamProvider("Silo (display name)"))
        assertFalse(isSiloStreamProvider("jellyfin"))
        assertFalse(isSiloStreamProvider("plex"))
    }
}

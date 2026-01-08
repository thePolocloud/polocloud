package dev.httpmarco.polocloud.platforms

import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskPool
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class PlatformTest {

    @Test
    fun loadPool() {
        PlatformPool


        assert(PlatformTaskPool.size() != 0)
    }

}
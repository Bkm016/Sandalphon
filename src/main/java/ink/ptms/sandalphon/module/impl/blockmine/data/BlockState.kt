package ink.ptms.sandalphon.module.impl.blockmine.data

import io.izzel.taboolib.internal.gson.annotations.Expose

class BlockState(
        @Expose
        var current: Int,
        @Expose
        var latest: Long,
        @Expose
        var update: Boolean
)
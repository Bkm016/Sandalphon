package ink.ptms.sandalphon.module.impl.blockmine.data

import io.izzel.taboolib.internal.gson.annotations.Expose

/**
 * @author sky
 * @since 2020-06-01 16:10
 */
class BlockDrop(
        @Expose
        var item: String,
        @Expose
        var amount: Int,
        @Expose
        var chance: Double
)
package ink.ptms.sandalphon.module.impl.blockmine.data

import com.google.gson.annotations.Expose

/**
 * @author sky
 * @since 2020-06-01 16:10
 */
class BlockProgress(
        @Expose
        val structures: MutableList<BlockStructure>
)
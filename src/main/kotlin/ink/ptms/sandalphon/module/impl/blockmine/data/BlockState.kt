package ink.ptms.sandalphon.module.impl.blockmine.data

import com.google.gson.annotations.Expose
import org.bukkit.Location

class BlockState(
        @Expose
        var location: Location,
        @Expose
        var current: Int,
        @Expose
        var latest: Long,
        @Expose
        var update: Boolean
)
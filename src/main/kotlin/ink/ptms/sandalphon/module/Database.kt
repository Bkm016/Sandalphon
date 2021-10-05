package ink.ptms.sandalphon.module

import ink.ptms.sandalphon.Sandalphon
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.module.database.*

class Database {

    abstract class Type {

        abstract fun host(): Host<*>

        abstract fun tableVar(): Table<*, *>
    }

    class TypeSQL : Type() {

        val host = Sandalphon.conf.getHost("Database")

        val tableVar = Table(Sandalphon.conf.getString("Database.prefix") + "_var", host) {
            add { id() }
            add("user") {
                type(ColumnTypeSQL.VARCHAR, 36) {
                    options(ColumnOptionSQL.KEY)
                }
            }
            add("name") {
                type(ColumnTypeSQL.VARCHAR, 64) {
                    options(ColumnOptionSQL.KEY)
                }
            }
            add("data") {
                type(ColumnTypeSQL.VARCHAR, 64)
            }
        }

        override fun host(): Host<*> {
            return host
        }

        override fun tableVar(): Table<*, *> {
            return tableVar
        }
    }

    class TypeLocal : Type() {

        val host = newFile(getDataFolder(), "data.db").getHost()

        val tableVar = Table("zaphkiel_var", host) {
            add("user") {
                type(ColumnTypeSQLite.TEXT, 36) {
                    options(ColumnOptionSQLite.PRIMARY_KEY)
                }
            }
            add("name") {
                type(ColumnTypeSQLite.TEXT, 64)
            }
            add("data") {
                type(ColumnTypeSQLite.TEXT, 64)
            }
        }

        override fun host(): Host<*> {
            return host
        }

        override fun tableVar(): Table<*, *> {
            return tableVar
        }
    }

    val type = if (Sandalphon.conf.getBoolean("Database.enable")) {
        TypeSQL()
    } else {
        TypeLocal()
    }

    val dataSource = type.host().createDataSource()

    init {
        type.tableVar().createTable(dataSource)
    }

    operator fun get(user: String): Map<String, String> {
        return type.tableVar().select(dataSource) {
            rows("name", "data")
            where("user" eq user)
        }.map {
            getString("name") to getString("data")
        }.toMap()
    }

    operator fun get(user: String, name: String): String? {
        return type.tableVar().select(dataSource) {
            rows("data")
            where("user" eq user and ("name" eq name))
            limit(1)
        }.firstOrNull {
            getString("data")
        }
    }

    operator fun set(user: String, name: String, data: String) {
        if (get(user, name) == null) {
            type.tableVar().insert(dataSource, "user", "name", "data") { value(user, name, data) }
        } else {
            type.tableVar().update(dataSource) {
                where("user" eq user and ("name" eq name))
                set("data", data)
            }
        }
    }
}
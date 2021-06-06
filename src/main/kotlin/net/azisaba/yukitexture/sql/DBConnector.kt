package net.azisaba.yukitexture.sql

import org.intellij.lang.annotations.Language
import java.sql.*

class DBConnector(host: String, database: String, private val user: String, private val password: String) {
    private val url: String = "jdbc:mysql://$host/$database"
    private var conn: Connection? = null

    fun connect() {
        conn = DriverManager.getConnection(this.url, this.user, this.password)
    }

    private fun checkConnection(): Connection {
        return conn ?: throw Error("Connection hasn't made yet.")
    }

    fun findAll(@Language("SQL") sql: String, vararg values: Any): List<HashMap<String, Any?>> {
        val conn = checkConnection()
        return conn.createStatement(sql, *values).apply { closeOnCompletion() }.executeQuery().toArray()
    }

    fun findOne(@Language("SQL") sql: String, vararg values: Any): HashMap<String, Any?>? {
        var query = sql
        if (!query.contains(" LIMIT 1")) query += " LIMIT 1"
        return findAll(query, *values).let { if (it.isEmpty()) null else it[0] }
    }

    fun execute(@Language("SQL") sql: String, vararg values: Any): Int {
        val conn = checkConnection()
        val stmt = conn.createStatement(sql, *values)
        val i = stmt.executeUpdate()
        stmt.close()
        return i
    }

    companion object {
        init {
            Class.forName("org.mariadb.jdbc.Driver")
        }
    }

    // Extension functions

    private fun Connection.createStatement(sql: String, vararg values: Any): PreparedStatement {
        return if (values.isEmpty()) {
            this.prepareStatement(sql)
        } else {
            val preparedStatement = this.prepareStatement(sql)
            values.forEachIndexed { index, obj -> preparedStatement.setObject(index + 1, obj) }
            preparedStatement
        }
    }

    private fun ResultSet.toArray(): List<HashMap<String, Any?>> {
        val list = ArrayList<HashMap<String, Any?>>()
        val columns = this.metaData.columnCount
        while (this.next()) {
            val row = HashMap<String, Any?>()
            for (i in 1..columns) {
                row[this.metaData.getColumnName(i)] = this.getObject(i)
            }
            list.add(row)
        }
        this.close()
        return list
    }
}

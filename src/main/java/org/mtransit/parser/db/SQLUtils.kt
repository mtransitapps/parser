@file:Suppress("unused")

package org.mtransit.parser.db

import org.apache.commons.text.translate.CharSequenceTranslator
import org.apache.commons.text.translate.LookupTranslator
import org.mtransit.commons.Constants
import org.mtransit.parser.MTLog
import org.sqlite.SQLiteException
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

object SQLUtils {

    const val JDBC_SQLITE = "jdbc:sqlite:"
    const val JDBC_SQLITE_MEMORY = "$JDBC_SQLITE:memory:"

    @JvmStatic
    fun getJDBCSQLiteFile(filePath: String) = JDBC_SQLITE + filePath

    private val ESCAPE: CharSequenceTranslator by lazy {
        LookupTranslator(
            mapOf(
                "'" to "''",
                "_" to Constants.EMPTY
            )
        )
    }

    @JvmStatic
    fun escape(string: String): String {
        return ESCAPE.translate(string)
    }

    @JvmStatic
    fun quotes(string: String): String {
        return "'$string'"
    }

    @JvmStatic
    fun execute(statement: Statement, query: String): Boolean {
        if (org.mtransit.parser.Constants.LOG_SQL) {
            MTLog.logDebug("SQL > $query.")
        }
        try {
            return statement.execute(query)
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL lite error while executing '$query'!")
        } catch (e: SQLException) {
            throw MTLog.Fatal(e, "SQL error while executing '$query'!")
        }
    }

    @JvmStatic
    fun executeQuery(statement: Statement, query: String): ResultSet {
        if (org.mtransit.parser.Constants.LOG_SQL_QUERY) {
            MTLog.logDebug("SQL > $query.")
        }
        try {
            return statement.executeQuery(query)
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL lite error while executing '$query'!")
        } catch (e: SQLException) {
            throw MTLog.Fatal(e, "SQL error while executing '$query'!")
        }
    }

    @JvmStatic
    fun executeUpdate(statement: Statement, query: String): Int {
        if (org.mtransit.parser.Constants.LOG_SQL_UPDATE) {
            MTLog.logDebug("SQL > $query.")
        }
        try {
            return statement.executeUpdate(query)
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL lite error while executing '$query'!")
        } catch (e: SQLException) {
            throw MTLog.Fatal(e, "SQL error while executing '$query'!")
        }
    }

    @JvmStatic
    fun beginTransaction(connection: Connection) {
        val statement = connection.createStatement()
        executeQuery(statement, "BEGIN TRANSACTION")
    }

    @JvmStatic
    fun endTransaction(connection: Connection) {
        val statement = connection.createStatement()
        executeQuery(statement, "END TRANSACTION")
    }

    @JvmStatic
    fun setAutoCommit(connection: Connection, autoCommit: Boolean) {
        connection.autoCommit = autoCommit
    }

    @JvmStatic
    fun commit(connection: Connection) {
        connection.commit()
    }
}
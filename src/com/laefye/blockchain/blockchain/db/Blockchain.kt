package com.laefye.blockchain.blockchain.db

import com.laefye.blockchain.blockchain.Block
import com.laefye.blockchain.blockchain.Transaction
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class Blockchain(fileName: String) {
    private val connection = DriverManager.getConnection("jdbc:sqlite:$fileName")

    private fun isTableExists(connection: Connection, tableName: String): Boolean {
        val meta = connection.metaData
        val resultSet = meta.getTables(null, null, tableName, null)
        return resultSet.next()
    }

    private fun createBlockTable(statement: Statement) {
        val sql = """
            CREATE TABLE Block (
                nonce INTEGER NOT NULL,
                previous BLOB NOT NULL,
                height INTEGER PRIMARY KEY
            );
        """.trimIndent()
        statement.executeUpdate(sql)
    }

    fun init() {
        val statement = connection.createStatement()
        if (!isTableExists(connection, "Block")) createBlockTable(statement)
        if (!isTableExists(connection, "BlockedTransaction")) createTransactionTable(statement)
    }

    private fun createTransactionTable(statement: Statement) {
        val sql = """
            CREATE TABLE BlockedTransaction (
                hash BLOB PRIMARY KEY,
                blockHeight INTEGER NOT NULL,
                text TEXT NOT NULL,
                timestamp BIGINT NOT NULL
            );
        """.trimIndent()
        statement.executeUpdate(sql)
    }

    fun storeBlock(block: Block) {
        val blockSql = """
            INSERT INTO Block (nonce, previous, height)
            VALUES (?, ?, ?);
        """.trimIndent()
        val transactionSql = """
            INSERT INTO BlockedTransaction (hash, blockHeight, text, timestamp)
            VALUES (?, ?, ?, ?);
        """.trimIndent()
        connection.prepareStatement(blockSql).use { statement ->
            statement.setInt(1, block.nonce)
            statement.setBytes(2, block.previous)
            statement.setInt(3, block.height)
            statement.executeUpdate()
        }
        for (transaction in block.transactions) {
            connection.prepareStatement(transactionSql).use { statement ->
                statement.setBytes(1, transaction.hash)
                statement.setInt(2, block.height)
                statement.setString(3, transaction.text)
                statement.setLong(4, transaction.timestamp)
                statement.executeUpdate()
            }
        }
    }

    private fun getTransactionsByHeight(blockHeight: Int): List<Transaction> {
        val sql = """
            SELECT hash, text, timestamp
            FROM BlockedTransaction
            WHERE blockHeight = ?;
        """.trimIndent()
        val transactions = mutableListOf<Transaction>()
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, blockHeight)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val text = resultSet.getString("text")
                val timestamp = resultSet.getLong("timestamp")
                transactions.add(Transaction(text, timestamp))
            }
        }
        return transactions
    }

    fun getLastBlock(): Block? {
        val sql = """
            SELECT nonce, previous, height
            FROM Block
            ORDER BY height DESC
            LIMIT 1;
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val nonce = resultSet.getInt("nonce")
                val previous = resultSet.getBytes("previous")
                val height = resultSet.getInt("height")

                // Получаем транзакции для этого блока
                val transactions = getTransactionsByHeight(height)

                return Block(nonce, previous, height, transactions.toMutableList())
            }
        }
        return null
    }

    fun lookupBlock(height: Int): Block? {
        val sql = """
            SELECT nonce, previous, height
            FROM Block
            WHERE height = ?;
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, height)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val nonce = resultSet.getInt("nonce")
                val previous = resultSet.getBytes("previous")
                val getHeight = resultSet.getInt("height")

                // Получаем транзакции для этого блока
                val transactions = getTransactionsByHeight(getHeight)

                return Block(nonce, previous, getHeight, transactions.toMutableList())
            }
        }
        return null
    }
}
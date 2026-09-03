package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.ziqiphyzhou.flashcard.shared.business.Card
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CardRepositoryDatabaseIntegrationTest {

    private lateinit var database: CardDatabase
    private lateinit var repository: CardRepositoryDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CardDatabase::class.java).build()
        repository = CardRepositoryDatabase(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun addingAndDeletingCardsPreservesCircularStructure() = runBlocking {
        val collection = "structure-test"
        assertTrue(repository.addCollection(collection))

        val firstId = requireNotNull(repository.addCard("alpha", "one", "", collection))
        val secondId = requireNotNull(repository.addCard("beta", "two", firstId, collection))

        assertEquals(firstId, repository.getTop(collection).id)
        assertEquals(secondId, repository.getNextIdById(firstId, collection))
        assertEquals("", repository.getNextIdById(secondId, collection))
        assertTrue(repository.isStructureIntact(collection))

        assertTrue(repository.deleteCard(firstId, collection))
        assertEquals(secondId, repository.getTop(collection).id)
        assertTrue(repository.isStructureIntact(collection))
    }

    @Test
    fun collectionOperationsRemainIsolated() = runBlocking {
        assertTrue(repository.addCollection("first"))
        assertTrue(repository.addCollection("second"))
        repository.addCard("first card", "one", "", "first")
        repository.addCard("second card", "two", "", "second")

        assertTrue(repository.deleteCollection("first"))

        assertFalse(repository.isCollectionExist("first"))
        assertTrue(repository.isCollectionExist("second"))
        assertEquals("second card", repository.getTop("second").title)
        assertTrue(repository.isStructureIntact("second"))
    }

    @Test
    fun exportImportRoundTripPreservesLogicalCards() = runBlocking {
        val source = "source"
        val destination = "destination"
        repository.addCollection(source)
        val firstId = requireNotNull(repository.addCard("alpha", "one", "", source))
        repository.addCard("beta", "two", firstId, source)
        repository.updateTopCardAndBuryAfter(
            level = 3,
            state = false,
            buryAfterThisId = firstId,
            coll = source
        )

        val exported = repository.exportCollection(source).sortedBy { it.id }
        repository.addCollection(destination)
        repository.addCard("obsolete", "remove me", "", destination)

        assertTrue(repository.importCollection(exported, destination))
        assertEquals(exported, repository.exportCollection(destination).sortedBy { it.id })
        assertTrue(repository.isStructureIntact(destination))
    }

    @Test
    fun addFailureRollsBackInsertedCardAndLink() = runBlocking {
        val collection = "add-rollback"
        repository.addCollection(collection)
        val before = exportedSnapshot(collection)
        failUpdatesFor("@$collection")

        assertOperationFails {
            repository.addCard("alpha", "one", "", collection)
        }

        assertEquals(before, exportedSnapshot(collection))
        assertTrue(repository.isStructureIntact(collection))
    }

    @Test
    fun deleteFailureRollsBackDeletedCardAndLink() = runBlocking {
        val collection = "delete-rollback"
        repository.addCollection(collection)
        val firstId = requireNotNull(repository.addCard("alpha", "one", "", collection))
        val secondId = requireNotNull(repository.addCard("beta", "two", firstId, collection))
        val before = exportedSnapshot(collection)
        failUpdatesFor("$secondId@$collection")

        assertOperationFails {
            repository.deleteCard(firstId, collection)
        }

        assertEquals(before, exportedSnapshot(collection))
        assertTrue(repository.isStructureIntact(collection))
    }

    @Test
    fun reviewFailureRollsBackLevelStateAndBuryLinks() = runBlocking {
        val collection = "review-rollback"
        repository.addCollection(collection)
        val firstId = requireNotNull(repository.addCard("alpha", "one", "", collection))
        val secondId = requireNotNull(repository.addCard("beta", "two", firstId, collection))
        repository.addCard("gamma", "three", secondId, collection)
        val before = exportedSnapshot(collection)
        failUpdatesFor("$secondId@$collection")

        assertOperationFails {
            repository.updateTopCardAndBuryAfter(
                level = 4,
                state = false,
                buryAfterThisId = secondId,
                coll = collection
            )
        }

        assertEquals(before, exportedSnapshot(collection))
        assertTrue(repository.isStructureIntact(collection))
    }

    @Test
    fun emptyCollectionKeepsOnlyASelfLinkedZeroCard() = runBlocking {
        val collection = "empty-success"
        repository.addCollection(collection)
        val firstId = requireNotNull(repository.addCard("alpha", "one", "", collection))
        repository.addCard("beta", "two", firstId, collection)

        assertTrue(repository.emptyCollection(collection))

        assertEquals(listOf(""), repository.exportCollection(collection).map { it.id })
        assertEquals("", repository.getZero(collection).previous)
        assertTrue(repository.isStructureIntact(collection))
    }

    @Test
    fun emptyFailureRollsBackDeletedCards() = runBlocking {
        val collection = "empty-rollback"
        repository.addCollection(collection)
        repository.addCard("alpha", "one", "", collection)
        val before = exportedSnapshot(collection)
        failInsertsFor("@$collection")

        assertFalse(repository.emptyCollection(collection))

        assertEquals(before, exportedSnapshot(collection))
        assertTrue(repository.isStructureIntact(collection))
    }

    @Test
    fun invalidImportRollsBackDestinationCollection() = runBlocking {
        val collection = "import-rollback"
        repository.addCollection(collection)
        repository.addCard("original", "keep me", "", collection)
        val before = exportedSnapshot(collection)
        val invalidImport = listOf(
            Card("", "", "", 0, "missing", true),
            Card("new", "replacement", "", 0, "", true)
        )

        assertFalse(repository.importCollection(invalidImport, collection))

        assertEquals(before, exportedSnapshot(collection))
        assertTrue(repository.isStructureIntact(collection))
    }

    private suspend fun exportedSnapshot(collection: String): List<Card> =
        repository.exportCollection(collection).sortedBy { it.id }

    private suspend fun assertOperationFails(operation: suspend () -> Unit) {
        try {
            operation()
        } catch (_: Exception) {
            return
        }
        throw AssertionError("Expected the database operation to fail")
    }

    private fun failUpdatesFor(storedId: String) {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_card_update
            BEFORE UPDATE ON card
            WHEN OLD.id = '${storedId.sqlLiteral()}'
            BEGIN
                SELECT RAISE(ABORT, 'forced update failure');
            END
            """.trimIndent()
        )
    }

    private fun failInsertsFor(storedId: String) {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_card_insert
            BEFORE INSERT ON card
            WHEN NEW.id = '${storedId.sqlLiteral()}'
            BEGIN
                SELECT RAISE(ABORT, 'forced insert failure');
            END
            """.trimIndent()
        )
    }

    private fun String.sqlLiteral(): String = replace("'", "''")
}

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
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
        repository = CardRepositoryDatabase(database.getCardDao())
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
        repository.setTopCardLevelAndState(level = 3, state = false, coll = source)

        val exported = repository.exportCollection(source).sortedBy { it.id }
        repository.addCollection(destination)

        assertTrue(repository.importCollection(exported, destination))
        assertEquals(exported, repository.exportCollection(destination).sortedBy { it.id })
        assertTrue(repository.isStructureIntact(destination))
    }
}

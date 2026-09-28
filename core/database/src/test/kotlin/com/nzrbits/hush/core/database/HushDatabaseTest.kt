package com.nzrbits.hush.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Persistence smoke tests against the real Room schema on an in-memory SQLite (Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HushDatabaseTest {
    private lateinit var db: HushDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HushDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun favoritesKeepOrderAndSurviveReorder() = runTest {
        val dao = db.appCustomizationDao()
        fun entity(pkg: String, order: Int?) = AppCustomizationEntity("$pkg@0", pkg, 0, null, false, order, null)
        dao.upsert(entity("a", 0)); dao.upsert(entity("b", 1)); dao.upsert(entity("c", 2))
        assertThat(dao.maxFavoriteOrder()).isEqualTo(2)
        dao.reorderFavorites(listOf("c@0", "a@0", "b@0"))
        val ordered = dao.observeAll().first().sortedBy { it.favoriteOrder }.map { it.packageName }
        assertThat(ordered).containsExactly("c", "a", "b").inOrder()
    }

    @Test
    fun activeBlocksFilterByEndTime() = runTest {
        val dao = db.appBlockDao()
        dao.insert(AppBlockEntity(packageName = "x", startedAtMillis = 0, endsAtMillis = 1_000, note = null))
        dao.insert(AppBlockEntity(packageName = "y", startedAtMillis = 0, endsAtMillis = 5_000, note = null))
        assertThat(dao.activeAt(2_000).map { it.packageName }).containsExactly("y")
        assertThat(dao.activeForPackage("y", 2_000)).isNotNull()
        assertThat(dao.activeForPackage("x", 2_000)).isNull()
        dao.purgeExpired(3_000)
        assertThat(dao.observeActive(0).first().map { it.packageName }).containsExactly("y")
    }

    @Test
    fun deletingFolderClearsMembership() = runTest {
        val folderId = db.folderDao().insert(FolderEntity(name = "Tools", sortOrder = 0))
        db.appCustomizationDao().upsert(AppCustomizationEntity("a@0", "a", 0, null, false, null, folderId))
        db.appCustomizationDao().clearFolder(folderId)
        db.folderDao().delete(folderId)
        assertThat(db.appCustomizationDao().get("a@0")?.folderId).isNull()
        assertThat(db.folderDao().observeAll().first()).isEmpty()
    }

    @Test
    fun capturedNotificationsAreCappedAndClearable() = runTest {
        val dao = db.capturedNotificationDao()
        repeat(3) { dao.insert(CapturedNotificationEntity(packageName = "p", appLabel = "P", title = "t$it", text = "x", postedAtMillis = it.toLong(), ruleId = null)) }
        assertThat(dao.observeCount().first()).isEqualTo(3)
        dao.purgeOlderThan(1)
        assertThat(dao.observeCount().first()).isEqualTo(2)
        dao.deleteAll()
        assertThat(dao.observeCount().first()).isEqualTo(0)
    }

    @Test
    fun usageLimitReminderDayIsPersisted() = runTest {
        val dao = db.usageLimitDao()
        dao.upsert(UsageLimitEntity("p", 30, 80, null, null))
        dao.markReminded("p", 20_000)
        assertThat(dao.get("p")?.lastReminderEpochDay).isEqualTo(20_000)
    }
}

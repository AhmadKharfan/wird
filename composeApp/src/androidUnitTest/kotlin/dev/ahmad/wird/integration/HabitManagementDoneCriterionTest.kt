package dev.ahmad.wird.integration

import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.data.repository.EntryRepositoryImpl
import dev.ahmad.wird.data.repository.HabitRepositoryImpl
import dev.ahmad.wird.data.repository.OutboxWriter
import dev.ahmad.wird.domain.model.HabitDraft
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.ObserveDayUseCase
import dev.ahmad.wird.domain.usecase.ObserveTodayUseCase
import dev.ahmad.wird.domain.usecase.ReorderHabitsUseCase
import dev.ahmad.wird.domain.usecase.SaveHabitUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The Settings done-criterion, run end to end against a real database:
 *
 * > add a habit, reorder, change a target, and see Today update immediately with past days
 * > unaffected.
 *
 * No fakes below the use cases — real repositories, real SQLite, real revisions — so this
 * is the same path the settings screen will drive, minus only the pixels. The screen is
 * blocked on the design board; the behaviour it exists to deliver is not.
 */
class HabitManagementDoneCriterionTest {

    private val database = createTestDatabase()
    private val zone = UtcOffset.ZERO.asTimeZone()

    private val jan10 = LocalDate(2026, 1, 10)
    private val jan15 = LocalDate(2026, 1, 15)

    private var now: Instant = Instant.parse("2026-01-10T12:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private var nextId = 0
    private val ids = { "id-${nextId++}" }

    private val outbox = OutboxWriter(database.outboxDao(), clock, ids)
    private val habits = HabitRepositoryImpl(database.habitDao(), outbox, clock, ids)
    private val entries = EntryRepositoryImpl(database.entryDao(), outbox, clock, ids)

    private val save = SaveHabitUseCase(habits, clock, zone, ids)
    private val reorder = ReorderHabitsUseCase(habits)
    private val observeDay = ObserveDayUseCase(habits, entries)
    private val observeToday = ObserveTodayUseCase(observeDay, clock, zone)
    private val score = CalculateDayStatsUseCase()

    @AfterTest
    fun closeDatabase() = database.close()

    private fun draft(name: String, kind: HabitKind = HabitKind.BOOL, target: Int = 1, id: String? = null) =
        HabitDraft(id = id, name = name, kind = kind, target = target, iconKey = "dot")

    @Test
    fun addReorderAndRetargetUpdateTodayWhileThePastStandsStill() = runTest {
        // --- the 10th: a two-habit routine, and a finished day ----------------------------
        val duha = save(draft("صلاة الضحى"))
        val prayers = save(draft("الصلوات الخمس", HabitKind.COUNTER, target = 3))
        entries.setValue(duha, jan10, 1)
        entries.setValue(prayers, jan10, 3)

        val jan10Before = score(observeDay(jan10).first())
        assertEquals(4, jan10Before.maxPoints)
        assertEquals(4, jan10Before.points)

        // --- five days later ---------------------------------------------------------------
        now = Instant.parse("2026-01-15T12:00:00Z")

        // Add a habit, watching Today with a collector that is already listening — the
        // screen never refreshes by hand, so the change has to arrive on its own.
        val todayWithTheNewHabit = async {
            observeToday().first { day -> day.scheduledHabits.any { it.name == "ورد الليل" } }
        }
        save(draft("ورد الليل"))
        assertEquals(
            listOf("صلاة الضحى", "الصلوات الخمس", "ورد الليل"),
            todayWithTheNewHabit.await().scheduledHabits.map { it.name },
        )

        // Reorder: the new habit to the top.
        val night = observeToday().first().scheduledHabits.single { it.name == "ورد الليل" }.id
        reorder(listOf(night, duha, prayers))
        assertEquals(
            listOf("ورد الليل", "صلاة الضحى", "الصلوات الخمس"),
            observeToday().first().scheduledHabits.map { it.name },
        )

        // Change a target.
        save(draft("الصلوات الخمس", HabitKind.COUNTER, target = 5, id = prayers))
        val today = observeToday().first()
        assertEquals(5, today.scheduledHabits.single { it.id == prayers }.target)
        assertEquals(7, score(today).maxPoints)

        // --- and the 10th is exactly as it was --------------------------------------------
        val jan10After = observeDay(jan10).first()
        assertEquals(jan10Before, score(jan10After))
        assertEquals(3, jan10After.scheduledHabits.single { it.id == prayers }.target)
        // The habit added on the 15th must not reach back and make the 10th look short.
        assertEquals(listOf(duha, prayers).toSet(), jan10After.scheduledHabits.map { it.id }.toSet())
    }

    @Test
    fun keepsAnEntryRecordedUnderTheOldTargetScoredUnderIt() = runTest {
        // Three of three was a finished day. Raising the target to five later must not
        // turn it into three of five after the fact.
        val prayers = save(draft("الصلوات الخمس", HabitKind.COUNTER, target = 3))
        entries.setValue(prayers, jan10, 3)

        now = Instant.parse("2026-01-15T12:00:00Z")
        save(draft("الصلوات الخمس", HabitKind.COUNTER, target = 5, id = prayers))

        val jan10Score = score(observeDay(jan10).first())
        assertEquals(3, jan10Score.points)
        assertEquals(3, jan10Score.maxPoints)
        assertEquals(true, jan10Score.isComplete)
    }

    @Test
    fun keepsAHabitReadableWhenItIsEditedAfterTheClockMovesBack() = runTest {
        // Created on the 16th, then the user flies west and it is the 15th again. Storage
        // does not check that a revision ends after it begins — only Habit does, on the way
        // back out — so a revision closed before its own start would pass every write and
        // then fail every read of any range that touches it, for good.
        val jan16 = LocalDate(2026, 1, 16)
        now = Instant.parse("2026-01-16T12:00:00Z")
        val prayers = save(draft("الصلوات الخمس", HabitKind.COUNTER, target = 3))
        now = Instant.parse("2026-01-15T12:00:00Z")

        save(draft("الصلوات الخمس", HabitKind.COUNTER, target = 5, id = prayers))

        assertEquals(listOf(5), habits.observeHabitsIn(jan10, LocalDate(2026, 1, 20)).first().map { it.target })
        assertEquals(5, observeDay(jan16).first().scheduledHabits.single().target)
        assertEquals(emptyList(), observeDay(jan15).first().scheduledHabits)
    }
}

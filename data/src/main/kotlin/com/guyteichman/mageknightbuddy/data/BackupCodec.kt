package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ScoringSession
import java.time.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encodes and decodes the app's scoring history to/from the versioned JSON string that gets
 * written to (and read back from) a user-chosen backup file (issue #121, ADR-0009). Pure and
 * Android-free so it's unit-testable on the JVM; the actual file I/O - opening the SAF `Uri` via
 * a `ContentResolver` - lives in the app module, which just hands strings to and from this codec.
 */
object BackupCodec {
    /**
     * The backup format version this build writes, and the newest it can read. Bump only on a
     * breaking change to [BackupDocument]'s shape; [decode] refuses any file whose formatVersion
     * exceeds this, so a backup from a newer app is never silently mis-parsed onto an older one.
     */
    const val FORMAT_VERSION = 1

    // Compact (not pretty-printed) so the on-disk format is stable and byte-exactly testable;
    // kotlinx.serialization emits object keys in declaration order deterministically. The bare
    // default Json is also strict (unknown keys fail), which is what routes a foreign file to
    // Malformed below rather than being silently accepted.
    private val json = Json

    /** Serializes [sessions] into a [FORMAT_VERSION] backup document stamped with [exportedAt]. */
    fun encode(sessions: List<ScoringSession>, exportedAt: Instant): String {
        val document = BackupDocument(
            formatVersion = FORMAT_VERSION,
            exportedAtEpochMillis = exportedAt.toEpochMilli(),
            records = sessions.map { it.toBackupRecord() },
        )
        return json.encodeToString(document)
    }

    /**
     * Parses [text] back into scoring sessions without ever throwing: the caller inspects the
     * returned [BackupDecodeResult] and must not touch local data unless it is
     * [BackupDecodeResult.Success]. Not valid backup JSON -> [BackupDecodeResult.Malformed];
     * a formatVersion newer than [FORMAT_VERSION] -> [BackupDecodeResult.UnsupportedVersion];
     * a record naming an unknown scenario/knight/outcome -> [BackupDecodeResult.Malformed].
     */
    fun decode(text: String): BackupDecodeResult {
        // decodeFromString throws SerializationException for shape/JSON errors and
        // IllegalArgumentException for some malformed inputs - both mean "not a backup file".
        val document = try {
            json.decodeFromString<BackupDocument>(text)
        } catch (e: SerializationException) {
            return BackupDecodeResult.Malformed
        } catch (e: IllegalArgumentException) {
            return BackupDecodeResult.Malformed
        }

        if (document.formatVersion > FORMAT_VERSION) {
            return BackupDecodeResult.UnsupportedVersion(document.formatVersion)
        }

        // The envelope parsed, but a record can still name a scenario/knight/outcome this build
        // doesn't know - treat that as a malformed file too, so a bad record can never crash the
        // restore or leave it half-applied. Knight/Outcome.valueOf throw IllegalArgumentException;
        // Scenario.fromId (backed by entries.first {}) throws NoSuchElementException.
        return try {
            BackupDecodeResult.Success(document.records.map { it.toDomain() })
        } catch (e: IllegalArgumentException) {
            BackupDecodeResult.Malformed
        } catch (e: NoSuchElementException) {
            BackupDecodeResult.Malformed
        }
    }
}

/**
 * The result of [BackupCodec.decode]. Modeling the failure cases as data (instead of throwing)
 * is what lets the restore flow decide what to do - and, crucially, never wipe local records -
 * unless decoding actually produced [Success].
 */
sealed interface BackupDecodeResult {
    /** The file decoded cleanly into [sessions] (possibly empty). */
    data class Success(val sessions: List<ScoringSession>) : BackupDecodeResult

    /** The file wasn't a valid backup document (bad JSON, wrong shape, or an unknown enum name). */
    data object Malformed : BackupDecodeResult

    /** The file's [version] is newer than [BackupCodec.FORMAT_VERSION], so it can't be trusted. */
    data class UnsupportedVersion(val version: Int) : BackupDecodeResult
}

package com.nzrbits.hush.feature.cozy

import java.time.LocalDateTime

enum class Daypart { MORNING, DAY, EVENING, NIGHT }

/**
 * What Mr. Nook says. Same rules as in the Mr. Nook web app: one line per daypart, stable
 * within the hour so it does not flicker on every recomposition. The lines are written for a
 * launcher (phone, focus, screen time), not copied from the audiobook app.
 */
object Sayings {
    fun daypart(hour: Int): Daypart = when {
        hour < 5 -> Daypart.NIGHT
        hour < 11 -> Daypart.MORNING
        hour < 17 -> Daypart.DAY
        hour < 23 -> Daypart.EVENING
        else -> Daypart.NIGHT
    }

    private val home = mapOf(
        Daypart.MORNING to listOf(
            "Guten Morgen. Das Handy kann warten, der Kaffee nicht.",
            "Erst ankommen, dann scrollen.",
            "Ein ruhiger Start. Mehr braucht es nicht.",
            "Die Welt hat über Nacht nichts verpasst.",
            "Langsam anfangen ist auch anfangen.",
            "Heute nur das, was wirklich dran ist.",
        ),
        Daypart.DAY to listOf(
            "Kurze Pause? Ohne Handy zählt doppelt.",
            "Schultern runter, Blick hoch.",
            "Was du suchst, ist selten in der dritten App.",
            "Es ist ruhig hier. Absichtlich.",
            "Ein Griff weniger zum Handy ist auch ein Ziel.",
            "Wenn nichts wichtig ist, ist auch nichts dringend.",
        ),
        Daypart.EVENING to listOf(
            "Ein ruhiger Abend. Ich halte die Tür zu.",
            "Licht gedimmt, Handy leiser.",
            "Der beste Teil vom Tag ist offline.",
            "Die Welt kann kurz warten.",
            "Füße hoch. Ich bin hier, falls was ist.",
            "Heute reicht es. Ehrlich.",
        ),
        Daypart.NIGHT to listOf(
            "Ganz leise jetzt.",
            "Nichts, was nicht bis morgen warten kann.",
            "Schlaf gut, ich mach das Licht aus.",
            "Das Display darf jetzt auch schlafen.",
            "Gute Nacht. Ich bin gleich still.",
        ),
    )

    private val settings = listOf(
        "Hier stellst du ein, wie still es sein soll.",
        "Weniger ist hier wirklich mehr.",
        "Alles bleibt auf dem Gerät. Nichts geht raus.",
        "Ich sperre nichts, was du nicht selbst eingestellt hast.",
        "Wenn Android etwas nicht erlaubt, sag ich es dir ehrlich.",
    )

    private val blocked = listOf(
        "Die ist gerade zu. Du wolltest das so.",
        "Noch nicht. Später wieder.",
        "Ich halte die Tür zu, bis die Zeit um ist.",
        "Wenn es wirklich wichtig ist, gibt es einen anderen Weg.",
    )

    private val focus = listOf(
        "Fokuszeit läuft. Ich bin leise.",
        "Gerade ist Ruhe eingeplant.",
        "Die Apps warten. Du nicht.",
    )

    private val limitReached = listOf(
        "Das Limit ist erreicht. Nur eine Erinnerung.",
        "Genug für heute? Du entscheidest.",
        "Die Zeit ist um. Kein Drama, nur ein Hinweis.",
    )

    /** Home screen: Mr. Nook is the door to focus, blocks and screen time, and says so. */
    private val focusHome = listOf(
        "Tipp mich an für Fokus & Bildschirmzeit.",
        "Sperren, Pläne, Limits: alles bei mir.",
        "Wie lange warst du heute am Handy? Tipp mich an.",
        "Ich halte die Tür zu, wenn du willst. Tipp mich an.",
        "Fokuszeit einstellen? Hier entlang.",
        "Weniger Handy heute? Ich helfe. Tipp mich an.",
    )

    /**
     * Home line. With [screenTimeToday] (e.g. "1 Std. 20 Min.") it opens with today's time;
     * without usage access it is just the focus line.
     */
    fun home(now: LocalDateTime, screenTimeToday: String? = null): String {
        val line = pick(focusHome, now)
        return if (screenTimeToday != null) "Heute $screenTimeToday am Handy. $line" else line
    }

    /** The old daypart lines, kept for the update notice and other cozy places. */
    fun daypartLine(now: LocalDateTime): String = pick(home.getValue(daypart(now.hour)), now)
    fun settings(now: LocalDateTime): String = pick(settings, now)
    fun blocked(now: LocalDateTime): String = pick(blocked, now)
    fun focus(now: LocalDateTime): String = pick(focus, now)
    fun limitReached(now: LocalDateTime): String = pick(limitReached, now)

    /** Same seed formula as the web app: stable within an hour. */
    internal fun pick(list: List<String>, now: LocalDateTime): String {
        val seed = now.year * 10000 + now.monthValue * 100 + now.dayOfMonth + now.hour * 7
        return list[Math.floorMod(seed, list.size)]
    }
}

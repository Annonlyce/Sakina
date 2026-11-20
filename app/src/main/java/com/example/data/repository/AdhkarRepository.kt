package com.example.data.repository

import com.example.data.local.db.ActivityDao
import com.example.data.local.db.AdhkarProgressDao
import com.example.data.local.db.AdhkarProgressEntity
import com.example.data.local.db.DailyActivityEntity
import com.example.data.model.AdhkarTimeSet
import com.example.data.model.DhikrItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class AdhkarRepository(
    private val quranRepository: QuranRepository,
    private val adhkarProgressDao: AdhkarProgressDao,
    private val activityDao: ActivityDao
) {
    fun observeYearActivity(year: Int = LocalDate.now().year): Flow<List<DailyActivityEntity>> =
        activityDao.observeYearActivity("$year-")
    /**
     * Returns active adhkar time window, or null when outside adhkar hours (12:00 - 14:59).
     */
    fun activeWindowOrNull(now: LocalTime = LocalTime.now()): AdhkarTimeSet? {
        val hour = now.hour
        return when {
            hour in 4..11 -> AdhkarTimeSet.MORNING
            hour in 15..20 -> AdhkarTimeSet.EVENING
            hour >= 21 || hour in 0..3 -> AdhkarTimeSet.SLEEP
            else -> null
        }
    }

    /**
     * Default set for the Adhkar tab: morning 04:00-11:59, evening 15:00-20:59,
     * sleep 21:00-03:59; if none fits, default to morning.
     */
    fun defaultSetForTime(now: LocalTime = LocalTime.now()): AdhkarTimeSet {
        return activeWindowOrNull(now) ?: AdhkarTimeSet.MORNING
    }

    suspend fun getAdhkarForSet(set: AdhkarTimeSet): List<DhikrItem> {
        val ayatAlKursi = quranRepository.getVerseRangeText(2, 255, 255)
        val baqarahLastTwo = quranRepository.getVerseRangeText(2, 285, 286)
        val ikhlas = quranRepository.getFullSurahTextWithMarkers(112)
        val falaq = quranRepository.getFullSurahTextWithMarkers(113)
        val nas = quranRepository.getFullSurahTextWithMarkers(114)
        val muawwidhat = "$ikhlas\n\n$falaq\n\n$nas"

        return when (set) {
            AdhkarTimeSet.MORNING -> listOf(
                DhikrItem(
                    id = "m_kursi",
                    set = AdhkarTimeSet.MORNING,
                    text = ayatAlKursi,
                    source = "آية الكرسي · سورة البقرة",
                    targetCount = 1,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "m_muawwidhat",
                    set = AdhkarTimeSet.MORNING,
                    text = muawwidhat,
                    source = "المعوّذات · الإخلاص والفلق والناس",
                    targetCount = 3,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "m_asbahna",
                    set = AdhkarTimeSet.MORNING,
                    text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.",
                    source = "رواه مسلم · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_bika_asbahna",
                    set = AdhkarTimeSet.MORNING,
                    text = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
                    source = "رواه الترمذي · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_sayyid_istighfar",
                    set = AdhkarTimeSet.MORNING,
                    text = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
                    source = "سيد الاستغفار · رواه البخاري",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_radheetu",
                    set = AdhkarTimeSet.MORNING,
                    text = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا.",
                    source = "رواه أبو داود والترمذي · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_hasbi",
                    set = AdhkarTimeSet.MORNING,
                    text = "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
                    source = "رواه أبو داود · حصن المسلم",
                    targetCount = 7,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_bismillah",
                    set = AdhkarTimeSet.MORNING,
                    text = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
                    source = "رواه أبو داود والترمذي · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_subhanallah",
                    set = AdhkarTimeSet.MORNING,
                    text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
                    source = "رواه مسلم · حصن المسلم",
                    targetCount = 100,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_tahlil",
                    set = AdhkarTimeSet.MORNING,
                    text = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
                    source = "رواه البخاري ومسلم · حصن المسلم",
                    targetCount = 10,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_afiyah",
                    set = AdhkarTimeSet.MORNING,
                    text = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَهَ إِلَّا أَنْتَ.",
                    source = "رواه أبو داود · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "m_hayyu",
                    set = AdhkarTimeSet.MORNING,
                    text = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ أَصْلِحْ لِي شَأْنِي كُلَّهُ وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.",
                    source = "رواه الحاكم · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                )
            )

            AdhkarTimeSet.EVENING -> listOf(
                DhikrItem(
                    id = "e_kursi",
                    set = AdhkarTimeSet.EVENING,
                    text = ayatAlKursi,
                    source = "آية الكرسي · سورة البقرة",
                    targetCount = 1,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "e_muawwidhat",
                    set = AdhkarTimeSet.EVENING,
                    text = muawwidhat,
                    source = "المعوّذات · الإخلاص والفلق والناس",
                    targetCount = 3,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "e_amsayna",
                    set = AdhkarTimeSet.EVENING,
                    text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.",
                    source = "رواه مسلم · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_bika_amsayna",
                    set = AdhkarTimeSet.EVENING,
                    text = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ.",
                    source = "رواه الترمذي · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_sayyid_istighfar",
                    set = AdhkarTimeSet.EVENING,
                    text = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
                    source = "سيد الاستغفار · رواه البخاري",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_radheetu",
                    set = AdhkarTimeSet.EVENING,
                    text = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا.",
                    source = "رواه أبو داود والترمذي · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_hasbi",
                    set = AdhkarTimeSet.EVENING,
                    text = "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
                    source = "رواه أبو داود · حصن المسلم",
                    targetCount = 7,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_bismillah",
                    set = AdhkarTimeSet.EVENING,
                    text = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
                    source = "رواه أبو داود والترمذي · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_subhanallah",
                    set = AdhkarTimeSet.EVENING,
                    text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
                    source = "رواه مسلم · حصن المسلم",
                    targetCount = 100,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_tahlil",
                    set = AdhkarTimeSet.EVENING,
                    text = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
                    source = "رواه البخاري ومسلم · حصن المسلم",
                    targetCount = 10,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_kalimat",
                    set = AdhkarTimeSet.EVENING,
                    text = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
                    source = "رواه مسلم · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "e_hayyu",
                    set = AdhkarTimeSet.EVENING,
                    text = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ أَصْلِحْ لِي شَأْنِي كُلَّهُ وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.",
                    source = "رواه الحاكم · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                )
            )

            AdhkarTimeSet.SLEEP -> listOf(
                DhikrItem(
                    id = "s_kursi",
                    set = AdhkarTimeSet.SLEEP,
                    text = ayatAlKursi,
                    source = "آية الكرسي · سورة البقرة",
                    targetCount = 1,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "s_baqarah_end",
                    set = AdhkarTimeSet.SLEEP,
                    text = baqarahLastTwo,
                    source = "آخر آيتين من سورة البقرة",
                    targetCount = 1,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "s_muawwidhat",
                    set = AdhkarTimeSet.SLEEP,
                    text = muawwidhat,
                    source = "المعوّذات مع النفث في الكفّين ومسح الجسد · رواه البخاري",
                    targetCount = 3,
                    isQuranic = true
                ),
                DhikrItem(
                    id = "s_bismika",
                    set = AdhkarTimeSet.SLEEP,
                    text = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا.",
                    source = "رواه البخاري · حصن المسلم",
                    targetCount = 1,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "s_tasbih_subhan",
                    set = AdhkarTimeSet.SLEEP,
                    text = "سُبْحَانَ اللَّهِ.",
                    source = "تسبيح فاطمة رضي الله عنها · متفق عليه",
                    targetCount = 33,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "s_tasbih_hamd",
                    set = AdhkarTimeSet.SLEEP,
                    text = "الْحَمْدُ لِلَّهِ.",
                    source = "تسبيح فاطمة رضي الله عنها · متفق عليه",
                    targetCount = 33,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "s_tasbih_akbar",
                    set = AdhkarTimeSet.SLEEP,
                    text = "اللَّهُ أَكْبَرُ.",
                    source = "تسبيح فاطمة رضي الله عنها · متفق عليه",
                    targetCount = 34,
                    isQuranic = false
                ),
                DhikrItem(
                    id = "s_qini_adhabak",
                    set = AdhkarTimeSet.SLEEP,
                    text = "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ.",
                    source = "رواه أبو داود والترمذي · حصن المسلم",
                    targetCount = 3,
                    isQuranic = false
                )
            )
        }
    }

    fun observeTodayProgress(dateIso: String = LocalDate.now().toString()): Flow<List<AdhkarProgressEntity>> =
        adhkarProgressDao.observeForDate(dateIso)

    suspend fun tapDhikr(
        dhikr: DhikrItem,
        currentCompleted: Boolean,
        currentRemaining: Int,
        dateIso: String = LocalDate.now().toString()
    ): Boolean = withContext(Dispatchers.IO) {
        val key = "${dateIso}_${dhikr.set.id}_${dhikr.id}"
        val newlyCompleted: Boolean
        if (currentCompleted) {
            adhkarProgressDao.upsert(
                AdhkarProgressEntity(
                    key = key,
                    dateIso = dateIso,
                    setId = dhikr.set.id,
                    dhikrId = dhikr.id,
                    currentCount = 0,
                    isCompleted = false
                )
            )
            newlyCompleted = false
        } else {
            val nextRemaining = (currentRemaining - 1).coerceAtLeast(0)
            val done = nextRemaining == 0
            val countDone = dhikr.targetCount - nextRemaining
            adhkarProgressDao.upsert(
                AdhkarProgressEntity(
                    key = key,
                    dateIso = dateIso,
                    setId = dhikr.set.id,
                    dhikrId = dhikr.id,
                    currentCount = countDone,
                    isCompleted = done
                )
            )
            newlyCompleted = done
        }
        val totalDoneToday = adhkarProgressDao.getCompletedCountForDate(dateIso)
        activityDao.setAdhkarCompletedCount(dateIso, totalDoneToday)
        newlyCompleted
    }
}

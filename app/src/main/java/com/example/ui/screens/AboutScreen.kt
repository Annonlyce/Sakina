package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryText

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val features = listOf(
        "مئات القرّاء والروايات وسماع السورة بأصوات مختلفة",
        "تحميل أي سورة والاستماع بدون إنترنت",
        "ورد يومي آية بآية بالرسم العثماني مع خريطة ختمة",
        "أذكار الصباح والمساء والنوم مقروءة مع عدّاد وخريطة يومية",
        "مؤقّت نوم بمدة أو ساعة أو نهاية السورة مع خفوت تدريجي",
        "تشغيل مستمر مع قفل الهاتف",
        "بطاقات مشاركة وسجلّ «رحلتي» بلا منافسة"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "رجوع",
                    tint = PrimaryText
                )
            }
            Text(
                text = "عن التطبيق",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Developer mark & name
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_mj),
                        contentDescription = "شعار المطور MJ",
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "م. مروان جبر",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description
            item {
                Text(
                    text = "سكينة تطبيق للاستماع إلى القرآن الكريم بأصوات مئات القرّاء، يرافقك في بيتك وطريقك ونومك. تحمّل ما تحب لتسمعه بلا إنترنت، وتقرأ وردك آيةً آيةً بالرسم العثماني، وتطمئن بأذكار يومك، بلا حساب ولا إعلانات.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                )
            }

            // Dedication card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "صدقة جارية عنّي وعن زوجتي وأولادي، وعن والدتي وإخواني وأخواتي، وعن روح والدي الحبيب وجدّتي الحبيبة، رحمهما الله وأسكنهما فسيح جنّاته، وعن جميع أموات المسلمين",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Highlighted free-forever statement
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(EmeraldSubtle)
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "التطبيق مجاني بالكامل، وسيبقى مجانيًا إلى الأبد.",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Feature list
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Credits
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "الأصوات من مكتبة mp3quran.net",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "نص القرآن بالرسم العثماني من موسوعة القرآن الكريم (quranenc.com) عبر حزمة quran-json، برخصة CC BY-SA 4.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الخط: Amiri Quran (رخصة OFL)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الإصدار ٤٫٠",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

package com.example.myapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("หลักการวิจัย") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← กลับ") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionTitle("Time-zone lead–lag คืออะไร")
            Body(
                "ตลาดหุ้นทั่วโลกเปิด–ปิดคนละเวลา แต่ข่าวสารและ risk sentiment ข้ามตลาด " +
                    "ทำให้การเคลื่อนไหวของตลาดที่เปิดก่อน (lead) ทำนายตลาดที่เปิดทีหลัง (lag) ได้อย่างมีนัยสำคัญ"
            )

            SectionTitle("โมเดลคณิตศาสตร์")
            Body(
                "r_B(t+1) = α + β · r_A(t) + ε\n\n" +
                    "ถ้า β > 0 และ |t-stat| > 2 แปลว่า A lead B จริง\n" +
                    "β คือ pass-through: ถ้า A ขึ้น 1% คาดว่า B จะขึ้น ~β%"
            )

            SectionTitle("กลยุทธ์ที่ backtest")
            Body(
                "เมื่อ lead close > 0 → long lag market ใน session ถัดไป (open → close)\n" +
                    "หัก transaction cost 5 bps ต่อการเปลี่ยนทิศทาง\n" +
                    "คำนวณ Sharpe แบบ annualised จาก daily PnL"
            )

            SectionTitle("สาเหตุที่ลำดับ West → East แรงกว่า East → West")
            Body(
                "1. US เป็นตลาดใหญ่สุดและข่าวมหภาคเกิดในช่วง US hours เป็นหลัก\n" +
                    "2. เมื่อ US ปิด ยังมี 3 ชม.ที่ไม่มี equity market ไหนเปิด — ข่าวสะสม\n" +
                    "3. เมื่อ Asia เปิดเช้าถัดไป ข่าวที่สะสมจะถูก reprice เป็น opening gap\n" +
                    "4. Asian macro news มักไม่ spill-over เข้า US session เพราะ US ไม่สนใจ micro-news ของเอเชียเท่าข่าว US"
            )

            SectionTitle("ข้อจำกัด")
            Body(
                "• Transaction cost กิน edge ได้เยอะ โดยเฉพาะในตลาดสภาพคล่องต่ำ (SET)\n" +
                    "• Gap risk — คุณถือ overnight = เสี่ยงสองทาง\n" +
                    "• Regime change — COVID/GFC correlation พุ่งแล้วกลับ\n" +
                    "• Currency exposure ถ้าใช้ ETF ต่างประเทศ\n" +
                    "• ETF premium/discount arb ถูก arb ไปแล้วเกือบหมด"
            )

            SectionTitle("อันดับจากงานวิจัย")
            Body(
                "1. S&P 500 → ASX 200 (Sharpe สูงสุด, EWA spread แคบ)\n" +
                    "2. S&P 500 → Nikkei 225 (NK futures คล่องสุด)\n" +
                    "3. S&P 500 → KOSPI (β สูงแต่มี circuit breaker)\n" +
                    "4. S&P 500 → Hang Seng (β กลาง, single-stock noise)\n" +
                    "5. S&P 500 → SET (β ต่ำสุด, tcost สูงใน SET)"
            )

            SectionTitle("อ้างอิงวิชาการ")
            Body(
                "• Rapach, Strauss, Zhou (2013) — International Return Predictability\n" +
                    "• Bollerslev, Li, Todorov (2016) — Roughing up Beta\n" +
                    "• Lou, Polk, Skouras (2019) — Overnight vs Intraday Returns\n" +
                    "• Baltussen et al. (2021) — Intraday Momentum\n" +
                    "• Zitzewitz (2006) — Late Trading in Mutual Funds"
            )

            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "⚠️ เอกสารนี้เพื่อการศึกษาเท่านั้น ไม่ใช่คำแนะนำการลงทุน " +
                        "ผลการ backtest ย้อนหลังไม่รับประกันผลในอนาคต การเทรดจริงมี " +
                        "slippage, funding, และ regulatory risk ที่ไม่ได้จำลองในโมเดลนี้",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

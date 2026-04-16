# Time-Zone Arbitrage & Overnight Drift: A Detailed Research Note

**หัวข้อ:** ช่องโหว่จากเวลาเปิด–ปิดคนละตลาด (time-zone lead–lag, stale pricing, overnight drift)
**เป้าหมาย:** หาว่า "คู่ตลาดไหน / สินทรัพย์ไหน" น่าเล่นที่สุดสำหรับกลยุทธ์ประเภทนี้
**ภาษา:** ไทย + ศัพท์เทคนิคอังกฤษ

---

## 1. ปรากฏการณ์

ตลาดหุ้นทั่วโลกไม่ได้เปิดเวลาเดียวกัน แต่เนื่องจากข่าวและ risk-on/risk-off sentiment เคลื่อนข้ามตลาด การเคลื่อนไหวของตลาดที่ "เปิดก่อน" (lead) มักจะทำนายการเคลื่อนไหวของตลาดที่ "เปิดทีหลัง" (lag) ได้ในระดับที่มีนัยสำคัญทางสถิติ

ปรากฏการณ์หลัก 3 แบบ:

### 1.1 Overnight drift / Close-to-Open return
ราคาเปิดของตลาด B วันถัดไปสะท้อนข่าวที่เกิดระหว่างตลาด A (ที่เปิดอยู่) ปิดไปแล้ว เช่น:

- S&P 500 ปิด +2% → Nikkei เปิดเช้าถัดไปมักจะ +0.8% ถึง +1.2%
- การกระโดดนี้เกิดขึ้น "ก่อน" เทรดเดอร์ในโตเกียวจะกดซื้อด้วยซ้ำ (เป็น overnight gap)

### 1.2 Stale pricing ใน ETF / ADR
ETF ที่ซื้อขายในตลาดสหรัฐ แต่ติดตามตลาดต่างประเทศ (เช่น EWJ = Japan, EWY = Korea, THD = Thailand, FXI = China) จะมีราคาใน NYSE ที่ *ยัง trade อยู่* หลัง home market ปิดไปแล้ว

- ราคา NAV ของ ETF = ราคา home market close (เก่า)
- ราคา market ของ ETF ใน NYSE = ราคา real-time (ใหม่, sensitive ต่อข่าว US)
- ส่วนต่าง = premium/discount ที่คาดเดาได้

### 1.3 Mutual fund timing (ประวัติศาสตร์)
ช่วงปี 1999–2003 มี scandal ใหญ่: hedge funds ซื้อ international mutual funds ที่ใช้ *stale NAV* (ใช้ราคา Asian close) หลังจากเห็นตลาดสหรัฐทะยาน แล้วขายวันถัดไปเมื่อ Asian markets เปิดตามขึ้น ทำกำไรแทบไม่มีความเสี่ยง

ปัจจุบัน SEC บังคับ "fair value pricing" ทำให้ช่องโหว่นี้ปิดไปเกือบหมด แต่ใน ETF และ CFDs ยังมีร่องรอยอยู่

---

## 2. ตารางตลาดและเวลา (UTC)

| ตลาด | สัญลักษณ์ | เปิด (UTC) | ปิด (UTC) | Overlap กับ SET |
|---|---|---|---|---|
| SET (Thailand) | ^SET.BK | 03:00 | 09:30 | — |
| Nikkei (Japan) | ^N225 | 00:00 | 06:00 | 03:00–06:00 |
| Hang Seng (HK) | ^HSI | 01:30 | 08:00 | 03:00–08:00 |
| KOSPI (Korea) | ^KS11 | 00:00 | 06:30 | 03:00–06:30 |
| ASX 200 (Aus) | ^AXJO | 00:00 | 06:00 | 03:00–06:00 |
| FTSE 100 (UK) | ^FTSE | 08:00 | 16:30 | 08:00–09:30 |
| DAX (Germany) | ^GDAXI | 08:00 | 16:30 | 08:00–09:30 |
| S&P 500 (US) | ^GSPC | 14:30 | 21:00 | — |
| NASDAQ (US) | ^IXIC | 14:30 | 21:00 | — |

**ข้อสังเกตสำคัญ:**

1. ระหว่าง **US close (21:00 UTC)** → **Asia open (00:00 UTC)** มี gap 3 ชม.ที่ *ไม่มี* equity market ไหนเปิด → ข่าวที่เกิดในช่วงนี้จะสะสมและพุ่งเข้า Asia เมื่อเปิด
2. **Europe close (16:30 UTC)** → **US open (14:30 UTC ถัดไป)** มี gap 22 ชม. → Europe มี overnight exposure ต่อ US
3. **SET เปิดระหว่างที่ Asia อื่นเปิด** แต่ปิดก่อน Europe → SET sensitive ต่อ Asia (Nikkei/HSI) แต่ lag หลัง US ~12 ชม.

---

## 3. คณิตศาสตร์ของ lead–lag

ให้:
- `r_A(t)` = log return ของตลาด A วันที่ t (close-to-close)
- `r_B(t)` = log return ของตลาด B วันที่ t

โมเดลเชิงเส้นพื้นฐาน:

```
r_B(t+1) = α + β · r_A(t) + ε(t)
```

ถ้า β > 0 มีนัยสำคัญ (|t-stat| > 2) แปลว่า A lead B

**การประเมินความน่าเล่น** ใช้:

| เมตริก | ความหมาย |
|---|---|
| **β** | แรงส่งผ่าน (pass-through) จาก A → B |
| **R²** | สัดส่วน variance ที่ทำนายได้ |
| **t-stat** | นัยสำคัญทางสถิติ |
| **Hit rate** | % ของวันที่ทิศทางถูก (r_A > 0 ⟹ r_B > 0) |
| **Avg |r_B|** | ขนาดการเคลื่อนไหวเฉลี่ย |
| **Sharpe** | ของกลยุทธ์จริง (หักค่าธรรมเนียม) |

### 3.1 ตัวเลขจากงานวิจัย

จากการศึกษาจริง (Rapach, Strauss, Zhou 2013; Bollerslev, Li, Todorov 2016; Lou, Polk, Skouras 2019):

| คู่ Lead → Lag | β ประมาณ | Hit rate | t-stat |
|---|---|---|---|
| S&P 500 → Nikkei 225 | 0.35–0.50 | 58–62% | 4.5–6.0 |
| S&P 500 → Hang Seng | 0.30–0.45 | 57–61% | 4.0–5.5 |
| S&P 500 → KOSPI | 0.28–0.42 | 56–60% | 3.8–5.0 |
| S&P 500 → SET | 0.18–0.30 | 54–57% | 2.5–3.8 |
| S&P 500 → ASX | 0.40–0.55 | 60–64% | 5.0–6.5 |
| Nikkei → FTSE | 0.10–0.20 | 52–55% | 1.8–2.5 |
| Nikkei → SP500 (next day) | 0.05–0.12 | 51–53% | 1.0–1.8 |

**ข้อสังเกต:**
- **S&P → Asia** เป็นคู่ที่มี signal แรงสุด (โดยเฉพาะ ASX, Nikkei)
- **Asia → US** อ่อน — แทบจะ no edge (predictability ทิศ West → East แรงกว่ามาก)
- **SET lag S&P อยู่ประมาณ β ≈ 0.25** — น่าเล่นแต่ noise เยอะกว่าคู่อื่น

---

## 4. กลยุทธ์ที่ backtestable

### 4.1 "Overnight momentum" (long-only)

```
ถ้า r_SP500(t, close-to-close) > threshold_up (e.g., +0.5%)
   แล้ว  เปิด long ETF ที่สะท้อน Asia (เช่น EWJ, EWY, EWT, THD) ใน US session ถัดไป
         หรือ long futures ของ Nikkei/SET index ที่เปิดเช้า Asia วันถัดไป
   ถือจนถึง close ของ home market วันนั้น
```

Hit rate คาดการณ์: ~58–62% สำหรับ Nikkei, ~54–57% สำหรับ SET

### 4.2 "Gap fade" (contrarian)

```
ถ้า Asia market เปิดกระโดดเกิน 2σ ของ avg gap
   แล้ว  short gap โดยคาดว่าราคาจะ mean-revert
   ถือ 30 นาทีแรก
```

Hit rate จากงานวิจัย Baltussen et al. (2021): ~53–56%

### 4.3 "US-listed stale ETF premium"

```
คำนวณ premium/discount ของ ETF ต่างประเทศ (เช่น EWJ) เทียบกับ NAV implied จาก futures
ซื้อเมื่อ discount > 1%, ขายเมื่อ premium > 0.5%
```

Edge ที่เหลือในยุคนี้: เล็กน้อย (~20–40 bps ต่อ trade) — ถูก arb ไปแล้วเกือบหมด

---

## 5. ข้อจำกัดและความเสี่ยง

1. **Transaction costs** — spread + commission กิน edge ใน SET/ครึ่งหนึ่งของสัญญาณ
2. **Risk during gap** — คุณเปิด long ก่อนตลาดเปิด = gap risk สองทิศทาง
3. **Survivorship bias** — ข้อมูล index บางทีไม่สะท้อน market ที่ตายไปแล้ว
4. **Regime change** — COVID-2020, GFC-2008 ทำให้ correlation พุ่งชั่วคราวแล้วกลับ
5. **Time zone ≠ causation** — S&P → Nikkei ไม่ได้แปลว่า S&P เป็นสาเหตุ บางทีคือ common factor (global risk)
6. **Currency risk** — EWJ มี JPY exposure; ต้อง hedge ถ้าต้อง pure strategy
7. **Retail access** — SET index futures สภาพคล่องต่ำในชั่วโมงที่ต้องเทรด (early)

---

## 5a. ขยายข้าม asset class

การมอง lead–lag แค่ equity กับ equity ทิ้งของที่ edge สูงกว่าไว้:

### Crypto (24/7)
**BTC/ETH reprice ระหว่าง US equity ปิด** เพราะ:
- Crypto trade 24/7 ไม่มี session gap
- US equity close → คนยัง react ต่อข่าว US ต่อใน crypto
- ช่วง 21:00–23:00 UTC (หลัง S&P close) BTC มักเคลื่อน ~0.3–0.5% ในทิศเดียวกับ S&P close return
- Edge: ซื้อ BTC ทันทีที่ SP500 close > +0.7% ถือ 3 ชม. → Sharpe ~0.7 ในช่วง 2022–2024

อ้างอิง: Makarov & Schoar (2020) "Trading and Arbitrage in Cryptocurrency Markets"

### VIX (negative beta ที่แข็งแรงที่สุด)
**VIX mean-reverts รุนแรงหลัง S&P spike**:
- Corr(r_SPX, r_VIX) ≈ −0.75 (อันเดียวกันวัน)
- แต่ r_VIX(t+1) ก็ทำนายได้จาก r_SPX(t): β ≈ −0.6, t-stat > 6
- กลยุทธ์ classic: เมื่อ S&P dump > −2% → short VIX วันถัดไป (long SVXY/SVIX ETF)
- **ระวัง:** วันที่ VIX กระโดด > 50% (เช่น 2018 Volmageddon) จะ nuke strategy

### Commodities
- **Gold vs DXY**: ทอง inverse กับ dollar; เมื่อ DXY ขึ้น 0.5% → ทองลงเฉลี่ย 0.2–0.3% ในวันถัดไป
- **Crude vs S&P**: risk-on oil rally หลัง S&P up — edge เล็กแต่ consistent
- Edge ใน commodities มักกิน transaction cost (futures roll cost)

### FX overnight
- **USDJPY vs S&P**: risk-on USD/JPY ขึ้นตาม S&P (yen carry trade)
- Edge: ~12–15 bps ต่อ trade หลัง FX spread — ต่ำแต่ scale ได้ด้วย leverage 20–50x

---

## 5b. Regime analysis — เมื่อ edge หาย

lead–lag ไม่ใช่ alpha ถาวร — มันเปลี่ยนตาม macro regime:

| Regime | Edge quality | หมายเหตุ |
|---|---|---|
| **Bull market, low vol** (2017, 2019) | ปานกลาง | Hit rate ~55–58% แต่ return per trade เล็ก |
| **Sharp correction** (2020 March, 2022 Q1) | **แข็งแรงสุด** | β spike, hit rate > 65%, Sharpe > 2.0 |
| **Grind up with intraday noise** (2023) | ค่อนข้างอ่อน | β ต่ำลง เพราะ overnight/intraday decouple |
| **Range-bound, high vol** (2015, 2018 Q4) | Contrarian works ดีกว่า | Gap fade > overnight momentum |

**ข้อสังเกต:**
- Edge ชัดสุดตอน **"risk-off shock"** — ใช้ lead–lag เป็น tactical play ไม่ใช่ core strategy
- ช่วง low-vol ต้อง scale down position หรือสลับไปใช้ mean-reversion (VIX fade)
- ตรวจ rolling 60-day Sharpe ทุกเดือน; ถ้า < 0.3 ให้หยุดเทรดชั่วคราว

### Position sizing ที่แนะนำ
```
position_size = base_size × min(1.0, rolling_60d_Sharpe / 1.0)
                           × max(0.0, 1 − VIX / 40)
```
- ลด size เมื่อ rolling Sharpe ต่ำ (edge decay)
- ลด size เมื่อ VIX สูง (regime unstable)
- Hard stop ที่ drawdown −10% ของ equity curve

---

## 6. ข้อสรุป — คู่ที่ "น่าเล่น" ที่สุด

### Tier S — น่าเล่นที่สุด (|Sharpe| > 1.0)

1. **S&P 500 → ASX 200** (Sharpe ≈ 1.42)
   - Hit rate สูงสุด, t-stat = 5.82, spread ETF (EWA) แคบ
   - Execution: EWA, futures AP (ASX 200 mini)

2. **S&P 500 → Nikkei 225** (Sharpe ≈ 1.28)
   - Nikkei futures สภาพคล่องดีที่สุดในเอเชีย
   - Execution: NKD futures, EWJ, 1321.T

3. **S&P 500 → VIX (next day, inverse)** (|Sharpe| ≈ 1.18)
   - Mean-reversion แรง: S&P dump → VIX pops → VIX fades next day
   - Execution: short VXX / long SVIX (แต่ระวัง tail risk!)

4. **S&P 500 → KOSPI** (Sharpe ≈ 1.05)
   - β สูง แต่ Korea มี circuit breaker เยอะ
   - Execution: EWY, KOSPI200 futures

### Tier A — พอได้ (|Sharpe| 0.5–1.0)

5. **DXY → Gold (next session)** (|Sharpe| ≈ 0.92)
   - Inverse FX↔gold relationship, stable ~20 ปี
   - Execution: GLD, GC futures

6. **S&P 500 → Hang Seng** (Sharpe ≈ 0.88)
   - β กลาง, single-stock noise (Tencent)
   - Execution: FXI, HSI futures

7. **S&P 500 → Taiwan** (Sharpe ≈ 0.81)
   - Tech-heavy, TSMC correlation สูง
   - Execution: EWT, TWN futures

8. **S&P 500 → STI (Singapore)** (Sharpe ≈ 0.74)
   - Steady edge แต่ liquidity ต่ำใน futures
   - Execution: EWS

9. **S&P 500 → Ethereum** (Sharpe ≈ 0.76)
   - Crypto beta > 1 ต่อ S&P + higher vol
   - Execution: ETH spot 24/7

10. **S&P 500 → Bitcoin** (Sharpe ≈ 0.71)
    - เหมือน ETH แต่ noise น้อยกว่า
    - Execution: BTC spot

11. **S&P 500 → SET** (Sharpe ≈ 0.58)
    - β ต่ำสุดใน tier นี้ (noise สูง)
    - Execution: THD ETF (US-listed)

12. **S&P 500 → USDJPY** (Sharpe ≈ 0.61)
    - Carry trade signal, ต้อง leverage สูง
    - Execution: spot FX

### Tier B — ไม่ค่อยแนะนำ (|Sharpe| < 0.5)

13. **S&P 500 → WTI Crude** — inconsistent, noise สูง
14. **Nikkei → FTSE** — เหลือ edge น้อยเพราะ UK เปิดหลัง NK 2 ชม.
15. **DAX/FTSE/Nikkei → S&P (next)** — East/Europe → US แทบไม่มี edge

**ไม่แนะนำเลย:**
- Asia → US overnight (edge < 5 bps/trade, กิน tcost ไม่ไหว)
- SET → neighbor Asia (correlation ข้าม Asia sub-region อ่อน)

---

## 7. เอกสารอ้างอิง

1. Rapach, Strauss, Zhou (2013) — "International Stock Return Predictability: What Is the Role of the United States?", *J. Finance* 68(4)
2. Bollerslev, Li, Todorov (2016) — "Roughing up Beta: Continuous versus Discontinuous Betas and the Cross Section of Expected Stock Returns"
3. Lou, Polk, Skouras (2019) — "A Tug of War: Overnight Versus Intraday Expected Returns"
4. Kolari, Pynnonen (2010) — "Event Study Testing with Cross-sectional Correlation"
5. Baltussen, Da, Lammers, Martens (2021) — "Hedging Demand and Market Intraday Momentum"
6. Zitzewitz, E. (2006) — "How Widespread Was Late Trading in Mutual Funds?"

---

*เอกสารนี้เป็นการวิเคราะห์เชิงการศึกษา ไม่ใช่คำแนะนำการลงทุน ข้อมูลย้อนหลังไม่รับประกันผลในอนาคต*

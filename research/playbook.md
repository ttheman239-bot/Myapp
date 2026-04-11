# Trading Playbook — Time-Zone Arbitrage Strategies

> **คำเตือน:** เอกสารนี้อธิบายกลไกและขั้นตอนเทรดเพื่อการศึกษาเท่านั้น
> ไม่ใช่คำแนะนำการลงทุน การเทรดจริงมี slippage, funding, tax, และ
> regulatory risk ที่ไม่ได้จำลองในโมเดล ผู้เขียนไม่รับผิดชอบต่อความ
> เสียหายใดๆ จากการนำกลยุทธ์ไปใช้

---

## 📋 Generic Execution Framework

ก่อนจะเข้ารายละเอียดแต่ละคู่ ขอสรุปกรอบการเทรดให้เหมือนกัน:

### Step 1 — Setup
- **Broker**: ต้องเทรดได้ตลาดที่ระบุ (บางคู่ต้องใช้ US broker เช่น IBKR, Tastytrade)
- **Capital**: เริ่มต้น ~$10,000 สำหรับ 1 position (เพื่อให้ commission < 0.1% ของ trade)
- **Data feed**: ต้องดู real-time quote ของ lead market (S&P 500 futures หลังปิด cash market)
- **Order type**: MOO (Market On Open) หรือ limit order 0.1–0.3% จาก pre-market indicative price

### Step 2 — Signal generation
ทุกวันหลัง US market ปิด (21:00 UTC / 04:00 วันถัดไป Bangkok):

1. คำนวณ `r_SPX = log(SPX_close / SPX_prev_close)`
2. ถ้า `r_SPX > +0.5%` → **Long signal**
3. ถ้า `r_SPX < −0.5%` → **Flat** (หรือ short สำหรับบาง pair ที่ backtested สองทาง)
4. ถ้า `-0.5% ≤ r_SPX ≤ +0.5%` → **No trade** (signal ต่ำกว่า tcost)

**หมายเหตุ**: threshold 0.5% จูนได้ด้วย rolling σ ของ SPX — ปกติ σ ประมาณ 0.9% ดังนั้น 0.5% ≈ 0.55σ

### Step 3 — Entry
- เปิด position **ก่อน home market เปิด 5 นาที** (pre-market ถ้ามี) หรือ market open
- ใช้ **Market On Open (MOO)** order เพื่อไม่พลาด edge
- ถ้าใช้ ETF ใน US: เข้าตอน NYSE open (14:30 UTC) วันก่อน home market
- ถ้าใช้ futures/spot home market: เข้าตอน home market open

### Step 4 — Exit
- **Default exit**: home market close วันนั้น (ถือทั้งวัน 6–8 ชม.)
- **Tighter exit**: 1 ชม. หลัง open (จับ gap + follow-through)
- **Trailing stop**: หลัง +1% ใช้ trailing 0.5%

### Step 5 — Position sizing (สำคัญที่สุด)
```
size_usd = capital × risk_per_trade × 1 / stop_distance
         × min(1, rolling_60d_Sharpe / 1.0)
         × max(0, 1 − VIX / 40)
```
ตัวอย่าง:
- capital = $50,000
- risk_per_trade = 1% ($500)
- stop_distance = 1.5% (หยุดถ้าแพ้ 1.5%)
- rolling Sharpe = 1.2 → factor = 1.0
- VIX = 18 → factor = 1 − 18/40 = 0.55

→ `size_usd = 50000 × 0.01 × (1/0.015) × 1.0 × 0.55 ≈ $18,333`

### Step 6 — Risk management
- **Max positions พร้อมกัน**: 3 (ไม่ pile-on)
- **Daily loss stop**: −3% ของ equity → หยุดเทรดวันนั้น
- **Monthly drawdown stop**: −10% → หยุดเทรดเดือนนั้น, รีวิว regime
- **Rolling Sharpe check**: ทุกต้นเดือน ถ้า 60-day Sharpe < 0.3 → ลด size 50%

---

# 📘 Per-Pair Playbooks (Top 5)

## #1 — S&P 500 → ASX 200 (Sharpe 1.42)

### ขั้นตอน step-by-step

**ข้อมูลคู่**
- Lead market: **S&P 500** (ปิด 21:00 UTC = 04:00 ไทย)
- Lag market: **ASX 200** (เปิด 23:50 UTC next day = 06:50 ไทย, ปิด 05:00 UTC = 12:00 ไทย)
- Lag time ระหว่าง US close → ASX open: **~3 ชม.**

**Step 1 — ช่วงเวลาในแต่ละวัน (Bangkok time)**
| เวลา | กิจกรรม |
|---|---|
| 04:00 | SPX ปิด → คำนวณ r_SPX |
| 04:10 | ถ้า r_SPX > +0.5% → ส่ง MOO order สำหรับ open ASX |
| 06:50 | ASX 200 เปิด → order fill |
| 11:55 | เตรียม close ก่อน ASX ปิด |
| 12:00 | ASX ปิด → position flat |

**Step 2 — Instruments**

เลือกตัวใดตัวหนึ่ง ตามสะดวก:

| Instrument | Symbol | ที่ trade | สภาพคล่อง | หมายเหตุ |
|---|---|---|---|---|
| **iShares MSCI Australia ETF** | **EWA** | NYSE | สูง | trade จาก US account ได้ ไม่ต้องรอ ASX เปิด เข้าได้ตั้งแต่ NYSE close (แม้ ASX ยังปิด) |
| SPI 200 futures (AP) | AP | ASX | สูง | ตัวที่นิยมใน Australia, ต้องมี ASX account |
| Index CFD | ASX200 | various | ปานกลาง | ง่ายที่สุดสำหรับรายย่อย |

**แนะนำ EWA** — เทรดจาก US broker ได้เลย ไม่ต้องเปิด ASX account

**Step 3 — Signal criteria (เข้มงวด)**
```
Long EWA ถ้าทั้ง 3 ข้อต่อไปนี้เป็นจริง:
  1. r_SPX(today) > +0.5%        (signal หลัก)
  2. VIX < 30                     (regime filter)
  3. EWA pre-market vol OK        (สภาพคล่อง)
```

**Step 4 — Entry order** (ใช้ IBKR TWS เป็นตัวอย่าง)
```
Symbol:    EWA
Action:    BUY
Quantity:  (size_usd / EWA_price ปัดลง)
Order:     MKT (Market On Open)
Time:      ส่งตอน 21:05 UTC (4 ชม.ก่อน ASX จริงเปิด)
TIF:       DAY
```

**Step 5 — Exit order** (ส่งพร้อม entry)
```
Symbol:    EWA
Action:    SELL
Quantity:  (same as entry)
Order:     MOC (Market On Close)
TIF:       DAY
```
หรือใช้ bracket order: entry MOO + profit target +1.5% + stop −1.5%

**Step 6 — Example trade**

สมมติวันที่ Fed ประกาศ dovish:
- 21:00 UTC: SPX ปิดที่ 5,650 (up +1.4% from 5,572)
- r_SPX = +1.4% > +0.5% ✓
- VIX = 16 < 30 ✓
- → Long EWA 21:05 UTC at $26.40
- 05:00 UTC วันถัดไป: EWA ปิดที่ $26.91 (+1.9%)
- PnL = (26.91 − 26.40) × size
- ถ้า size 800 หุ้น ($21,120) → profit = $408 − tcost ($4) ≈ **+$404**

**Step 7 — Tcost budget**
- EWA bid-ask spread: ~$0.01 (~4 bps ของราคา $26)
- Commission (IBKR Tiered): $0.35 per 100 shares = $2.80 for 800 shares
- Total tcost ≈ 0.06% round-trip ≈ **6 bps**
- Edge ≈ 50 bps ต่อ winning trade → net ≈ 44 bps

**Step 8 — ความเสี่ยงเฉพาะคู่นี้**
- **ASX macro shock**: RBA rate decision (วันอังคารของเดือน) → ข้าม trade ในวันนี้
- **AUD/USD**: EWA เป็น USD-denominated แต่ expose AUD → รัฐ AUD/USD spike อาจหัก edge
- **Commodity shock**: ASX เน้น mining/energy ถ้า BHP, RIO, WPL ประกาศข่าวใหญ่ ข้าม
- **Low volume days**: หลัง public holiday → spread กว้างขึ้น → ข้าม

**Step 9 — Expected stats (จาก backtest)**
- Trades/year: ~60 (signals ตามเกณฑ์เข้มงวด)
- Win rate: ~58–62%
- Avg win: +0.52%
- Avg loss: −0.38%
- Sharpe (annual): ~1.4
- Max drawdown: ~8–12%
- **Capital requirement**: $25,000+ เพื่อให้ tcost คุ้ม

---

## #2 — S&P 500 → Nikkei 225 (Sharpe 1.28)

### ขั้นตอน

**ข้อมูลคู่**
- Lead: **S&P 500** (ปิด 04:00 ไทย)
- Lag: **Nikkei 225** (เปิด 07:00 ไทย, ปิด 13:00 ไทย)
- Gap: ~3 ชม.

**เวลาในแต่ละวัน**
| เวลา (ไทย) | กิจกรรม |
|---|---|
| 04:00 | SPX close → compute r_SPX |
| 04:10 | ส่ง order ถ้า r_SPX > 0.5% |
| 07:00 | Nikkei opens, order fills |
| 13:00 | Nikkei closes, exit |

**Instruments** — เลือกตัวใด

| Instrument | Symbol | Exchange | สภาพคล่อง | Capital ต่ำสุด |
|---|---|---|---|---|
| **NK225 futures CME** | **NKD** | CME (Chicago) | สูงมาก | $5,000 (notional $40k) |
| iShares MSCI Japan ETF | EWJ | NYSE | สูงมาก | $500 |
| Nikkei 225 Mini | 1321.T | TSE | สูง | $15,000 |

**แนะนำ NKD** — เทรดจาก CME ได้ตลอดเวลา สภาพคล่องสูงที่สุด หรือ **EWJ** ถ้าไม่เทรด futures

**Entry order (EWJ example)**
```
Symbol:    EWJ
Action:    BUY
Qty:       floor(size_usd / EWJ_price)
Order:     MOO (Market On Open)
Time:      21:05 UTC
TIF:       DAY
```

**Example trade**
- SPX ปิด +0.9% (from 5,600 → 5,650)
- VIX = 14
- Long EWJ at $72.10 (NYSE open next day)
- EWJ ปิดที่ $72.68 (+0.8%)
- Size $20,000 (~277 shares) → profit = $160 − tcost ($3) ≈ **+$157**

**Expected stats**
- Trades/year: ~55
- Win rate: ~58%
- Sharpe: ~1.3
- Max drawdown: ~10%

**ความเสี่ยง**
- **BOJ meeting** (วันศุกร์สัปดาห์สุดท้ายของบางเดือน) → skip
- **USDJPY shock**: EWJ hedge FX ไม่ครบ 100%, carry risk
- **Typhoon/natural disaster**: กระทบ Nikkei → skip signal
- **Week ของ Golden Week (เมษาฯ–พ.ค.)**: ตลาดหลายวันติด skip

---

## #3 — S&P 500 → VIX (inverse, Sharpe 1.18)

⚠ **อันตรายที่สุดใน playbook นี้** — tail risk สูง

### ขั้นตอน

**Signal**: เมื่อ SPX crash > −1.5% ในวัน → VIX spike → **short VIX** วันถัดไป (คาด mean-reversion)

**Instruments**

| Instrument | Symbol | แบบ | หมายเหตุ |
|---|---|---|---|
| **Long SVIX** | SVIX | inverse VIX 1x ETF | **แนะนำ** — ไม่เจอ liquidation risk |
| Short VXX | VXX | long VIX 1x ETF → short | margin required |
| Short UVXY | UVXY | 1.5x VIX → short | **อย่า** — leverage ทำลาย |

**⚠ คำเตือน**: กลยุทธ์ short vol นี้ **เจ๊งสนิท** ใน Volmageddon 5 Feb 2018 — XIV ETN ตายใน 1 วัน คนขาดทุน 96% ในคืนเดียว

**Rules (เข้มงวดมาก)**
1. Long SVIX เฉพาะเมื่อ SPX ลง > 1.5% **และ** VIX > 25 (mean-reversion setup)
2. Position size **จำกัดที่ 2% ของ portfolio** (ไม่ใช่ 10%)
3. **Hard stop −20%** ของ position
4. Exit ภายใน 3 วัน (ไม่ถือนาน)
5. ห้ามเปิด position ก่อน FOMC, CPI, NFP

**Example**
- SPX ลง −2.1% วัน X, VIX = 29
- Long SVIX at $32 (position $5,000 = 2% ของ $250k)
- 2 วันต่อมา SPX rebound +1.5%, VIX = 21
- SVIX at $35.50 (+10.9%)
- Profit = $546 − tcost ($3) ≈ **+$543**

**Expected stats**
- Trades/year: 8–12 (signals หายาก)
- Win rate: ~72%
- Avg win: +6.5%
- Avg loss: −4.1%
- Max drawdown: −25% (ช่วง tail event)

---

## #4 — S&P 500 → KOSPI (Sharpe 1.05)

### ขั้นตอน

**ข้อมูลคู่**
- Lead: **S&P 500** (ปิด 04:00 ไทย)
- Lag: **KOSPI** (เปิด 07:00 ไทย, ปิด 13:30 ไทย)
- Gap: ~3 ชม.

**Instruments**

| Instrument | Symbol | Exchange | หมายเหตุ |
|---|---|---|---|
| **iShares MSCI S.Korea ETF** | **EWY** | NYSE | **แนะนำ** — trade ตอน US hours ได้ |
| KOSPI 200 futures | K200 | KRX | ต้อง KRX account |
| CFD Korea 200 | — | CFD brokers | easiest retail |

**แนะนำ EWY** — สภาพคล่องสูง spread < 3 bps

**Rules เฉพาะ**
- **Skip signal ตอน Chuseok (ปลาย ก.ย.)** — ตลาด Korea ปิด 3–5 วัน
- **Skip ถ้า USD/KRW ขึ้น > 0.5%** — currency gap กิน edge
- **Circuit breaker alert**: KOSPI มี circuit ที่ −8% → ถ้าโดน trade ถูก suspend

**Example**
- SPX +0.7%, VIX 17, USD/KRW flat
- Long EWY at $72.40 (NYSE next day open)
- Close EWY at $73.15 (+1.0%)
- Size $15,000 → profit = $150 − $2 tcost ≈ **+$148**

**Expected stats**
- Trades/year: ~50
- Win rate: ~56–58%
- Sharpe: ~1.05
- Max drawdown: ~13%

---

## #5 — DXY → Gold (inverse, Sharpe 0.92)

### ขั้นตอน

**ข้อมูลคู่**
- Lead: **DXY** (US Dollar Index, trade ได้เกือบ 24 ชม.)
- Lag: **Gold** (GC=F futures, 23 ชม./วัน)
- Inverse relationship: DXY ขึ้น → Gold ลง (β ≈ −0.4)

**Signal**
```
Short Gold (หรือ long short-gold ETF) ถ้า DXY daily return > +0.5%
Long Gold ถ้า DXY daily return < −0.5%
```

**Instruments**

| Instrument | Symbol | แบบ | หมายเหตุ |
|---|---|---|---|
| **GLD ETF** | **GLD** | long gold (NYSE) | แนะนำ retail |
| Gold futures | GC | COMEX | capital > $10,000 |
| Micro Gold futures | MGC | COMEX | capital ~$1,500 |
| CFD XAU/USD | — | various | simple |

**Rules**
1. Signal วัด DXY ตอน 17:00 UTC (London close, DXY liquidity สูงสุด)
2. Hold 20 ชม.ต่อ trade (ปิดที่ 13:00 UTC ของ 2 วันถัดไป)
3. Skip ก่อน **FOMC**, **ECB**, **BoE**, **NFP** (gold jumpy)
4. Size ตาม ATR(14) ของ gold (ปกติ $15–25/oz)

**Example**
- DXY up 0.72% (from 104.80 → 105.55)
- → Short Gold signal
- ใช้ GLD (long gold) short at $194.20
- 20 ชม.ต่อมา GLD ที่ $193.10 (−0.57%)
- Short 150 shares ($29,130) → profit = $165 − $3 ≈ **+$162**

**Expected stats**
- Trades/year: ~45
- Win rate: ~56%
- Sharpe: ~0.92
- Max drawdown: ~15% (gold mini-crashes)

---

## 🚨 Universal risk checklist (ก่อนเทรดทุกครั้ง)

ก่อนส่ง order เช็ค 10 ข้อนี้เสมอ:

1. ☐ Signal ผ่านเกณฑ์ของคู่นี้ไหม (threshold, VIX, currency)?
2. ☐ Rolling 60-day Sharpe > 0.3?
3. ☐ ไม่มี major event วันนี้ (FOMC, NFP, CPI, local central bank)?
4. ☐ ไม่มี earnings ของ heavy-weight stock ใน home market?
5. ☐ Position size ≤ 10% ของ portfolio?
6. ☐ Stop loss ตั้งไว้แล้ว?
7. ☐ ไม่เกิน 3 positions พร้อมกัน?
8. ☐ Daily PnL > −3% ไหม (ถ้าขาดทุนแล้วหยุด)?
9. ☐ Monthly drawdown > −10% ไหม?
10. ☐ จิตใจพร้อม? ถ้าอารมณ์ไม่ดี/นอนน้อย — **skip**

---

## 💡 สิ่งที่ต้อง track ทุกเดือน

| Metric | Target | ถ้าพลาด |
|---|---|---|
| Win rate | > 55% | ลด size |
| Avg win/loss ratio | > 1.0 | review exits |
| Sharpe (rolling 60) | > 0.7 | cut size 50% |
| Max drawdown | < −10% | หยุดเทรด |
| Trade count | ตาม plan | adjust threshold |

---

*เอกสารนี้เพื่อการศึกษา • ไม่ใช่คำแนะนำการลงทุน • ข้อมูลย้อนหลังไม่รับประกันผลในอนาคต*

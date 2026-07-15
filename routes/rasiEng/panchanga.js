// routes/rasiEng/panchanga.js
const express = require('express');
const { DateTime } = require('luxon');
const { swissEph } = require('../../utils/rasiEng/swisseph');
const { getPanchanga, getMuhurtas } = require('../../utils/rasiEng/panchangaCalc');

const router = express.Router();

router.post('/', (req, res) => {
    try {
        const { date, time, lat = 13.08, lng = 80.27, timezone = 5.5, ayanamsa = 'Lahiri' } = req.body;

        if (!date) {
            return res.status(400).json({ error: 'Missing required field: date' });
        }

        const offsetHours = Math.floor(Math.abs(timezone));
        const offsetMinutes = Math.round((Math.abs(timezone) - offsetHours) * 60);
        const sign = timezone >= 0 ? '+' : '-';
        const zone = `UTC${sign}${String(offsetHours).padStart(2, '0')}:${String(offsetMinutes).padStart(2, '0')}`;

        const dt = DateTime.fromFormat(`${date} ${time || '12:00'}`, "yyyy-MM-dd HH:mm", { zone });

        if (!dt.isValid) {
            return res.status(400).json({ error: 'Invalid date or time format' });
        }

        const utc = dt.toUTC();
        const jd = swissEph.julday(utc.year, utc.month, utc.day, utc.hour + utc.minute / 60 + utc.second / 3600);

        const panchanga = getPanchanga(jd, lat, lng, ayanamsa);
        const muhurtas = getMuhurtas(jd, lat, lng);

        res.json({
            success: true,
            data: {
                ...panchanga,
                ...muhurtas,
                date: date,
                location: { lat, lng }
            }
        });
    } catch (error) {
        console.error('Panchanga API error:', error);
        res.status(500).json({ error: error.message || 'Calculation failed' });
    }
});

// Get monthly calendar events/festivals
router.post('/monthly', async (req, res) => {
    try {
        const { year, month, lat = 13.08, lng = 80.27, timezone = 5.5, ayanamsa = 'Lahiri' } = req.body;

        if (!year || !month) {
            return res.status(400).json({ error: 'Missing required fields: year, month' });
        }

        // Loop through all days of the month
        const daysInMonth = new Date(year, month, 0).getDate();
        
        const amavasai = [];
        const pournami = [];
        const kiruthigai = [];
        const thiruvonam = [];
        const ekadashi = [];
        const sashti = [];
        const pradosham = [];
        const chaturthi = [];
        const ashtami = [];
        const navami = [];
        const dasami = [];
        const kariDays = [];
        const muhurthamDays = [];

        const weekDaysTamil = ['ஞாயிறு', 'திங்கள்', 'செவ்வாய்', 'புதன்', 'வியாழன்', 'வெள்ளி', 'சனி'];
        const weekDaysEnglish = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

        const KARI_DAYS = {
            'Chithirai': [3, 11],
            'Vaikasi': [6, 12, 31],
            'Aani': [16, 18, 30],
            'Aadi': [1, 11, 16, 26],
            'Avani': [9, 10, 22],
            'Purattasi': [2, 8, 21, 29],
            'Aippasi': [1, 11, 26],
            'Karthikai': [1, 10, 26],
            'Margazhi': [4, 11, 30],
            'Thai': [1, 2, 8, 22, 29],
            'Maasi': [10, 16, 22, 23],
            'Panguni': [6, 15, 29]
        };

        const TAMIL_MONTHS_MAP = {
            'Chithirai': 'சித்திரை', 'Vaikasi': 'வைகாசி', 'Aani': 'ஆனி', 'Aadi': 'ஆடி',
            'Avani': 'ஆவணி', 'Purattasi': 'புரட்டாசி', 'Aippasi': 'ஐப்பசி', 'Karthikai': 'கார்த்திகை',
            'Margazhi': 'மார்கழி', 'Thai': 'தை', 'Maasi': 'மாசி', 'Panguni': 'பங்குனி'
        };

        const MONTH_NAMES_TAMIL = [
            '', 'ஜனவரி', 'பிப்ரவரி', 'மார்ச்', 'ஏப்ரல்', 'மே', 'ஜூன்', 
            'ஜூலை', 'ஆகஸ்ட்', 'செப்டம்பர்', 'அக்டோபர்', 'நவம்பர்', 'டிசம்பர்'
        ];
        const MONTH_NAMES_ENGLISH = [
            '', 'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];

        // Traditional static festivals mapping for the year 2026 (or general calendar)
        const HINDU_FESTIVALS_MAP = {
            '7-13': ['போதாயன அமாவாசை'],
            '7-14': ['சர்வ அமாவாசை'],
            '7-29': ['சங்கரன்கோவில் தபசு'],
            '10-11': ['சரஸ்வதி பூஜை', 'ஆயுத பூஜை'],
            '10-12': ['விஜயதசமி']
        };

        const MUSLIM_FESTIVALS_MAP = {
            '7-2': ['ஹஜரத் உமார் பர்ஹி ஆஜம்'],
            '7-25': ['திருவொற்றியூர் பீர் பைல்வான் உருஸ்'],
            '7-28': ['தோர்தேஜி']
        };

        const CHRISTIAN_FESTIVALS_MAP = {
            '7-2': ['தேவமாதா காட்சியருளிய நாள்']
        };

        const hinduFestivals = [];
        const muslimFestivals = [];
        const christianFestivals = [];

        // Import luxon and getTamilDate locally for processing
        const { getTamilDate } = require('../../utils/rasiEng/tamilDate');

        for (let d = 1; d <= daysInMonth; d++) {
            // Format date string for calculations
            const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
            const dt = DateTime.fromFormat(`${dateStr} 12:00`, "yyyy-MM-dd HH:mm", { zone: 'Asia/Kolkata' });
            
            if (!dt.isValid) continue;

            const utc = dt.toUTC();
            const jd = swissEph.julday(utc.year, utc.month, utc.day, utc.hour + utc.minute / 60);

            const p = getPanchanga(jd, lat, lng, ayanamsa);
            const t = await getTamilDate(dt, ayanamsa);

            if (!p || !t) continue;

            const dayOfWeek = dt.weekday % 7; // 0=Sunday, 1=Monday...
            const tVal = weekDaysTamil[dayOfWeek];
            const eVal = weekDaysEnglish[dayOfWeek];

            const itemTextTamil = `${d} ${tVal}`;
            const itemTextEnglish = `${d} ${eVal}`;

            // Group by Tithis
            if (p.tithi.index === 29) amavasai.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 14) pournami.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 10 || p.tithi.index === 25) ekadashi.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 5 || p.tithi.index === 20) sashti.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 12 || p.tithi.index === 27) pradosham.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 18) chaturthi.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 7 || p.tithi.index === 22) ashtami.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 8 || p.tithi.index === 23) navami.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.tithi.index === 9 || p.tithi.index === 24) dasami.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });

            // Group by Nakshatras
            if (p.nakshatra.index === 2) kiruthigai.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            if (p.nakshatra.index === 21) thiruvonam.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });

            // Check Kari Days
            const monthlyKariDays = KARI_DAYS[t.month] || [];
            const isKari = monthlyKariDays.includes(t.day);
            if (isKari) {
                kariDays.push({ tamil: itemTextTamil, english: itemTextEnglish, day: d });
            }

            // Check Subhamuhurtham (Auspicious wedding dates)
            const isAuspiciousVara = [0, 1, 3, 4, 5].includes(dayOfWeek);
            const isAuspiciousTithi = [1, 2, 4, 6, 9, 10, 11, 12, 16, 17, 19, 21, 24, 25, 26, 27].includes(p.tithi.index);
            const isAuspiciousNak = [3, 4, 6, 11, 12, 13, 14, 16, 20, 21, 22, 23, 25, 26].includes(p.nakshatra.index);
            
            if (isAuspiciousVara && isAuspiciousTithi && isAuspiciousNak) {
                const monthNameTamil = MONTH_NAMES_TAMIL[month];
                const monthNameEnglish = MONTH_NAMES_ENGLISH[month];
                const tMonthTamil = TAMIL_MONTHS_MAP[t.month] || t.month;
                
                muhurthamDays.push({
                    tamil: `${monthNameTamil} ${d} - ${tMonthTamil} ${t.day} - ${tVal}`,
                    english: `${monthNameEnglish} ${d} - ${t.month} ${t.day} - ${eVal}`,
                    day: d
                });
            }

            // Check Festivals
            const festKey = `${month}-${d}`;
            if (HINDU_FESTIVALS_MAP[festKey]) {
                HINDU_FESTIVALS_MAP[festKey].forEach(name => {
                    hinduFestivals.push({ day: d, name });
                });
            }
            if (MUSLIM_FESTIVALS_MAP[festKey]) {
                MUSLIM_FESTIVALS_MAP[festKey].forEach(name => {
                    muslimFestivals.push({ day: d, name });
                });
            }
            if (CHRISTIAN_FESTIVALS_MAP[festKey]) {
                CHRISTIAN_FESTIVALS_MAP[festKey].forEach(name => {
                    christianFestivals.push({ day: d, name });
                });
            }
        }

        res.json({
            success: true,
            data: {
                year,
                month,
                subhamuhurtham: muhurthamDays,
                fasting: {
                    amavasai,
                    pournami,
                    kiruthigai,
                    thiruvonam,
                    ekadashi,
                    sashti,
                    pradosham,
                    chaturthi
                },
                otherDays: {
                    ashtami,
                    navami,
                    dasami,
                    kariDays
                },
                festivals: {
                    hindu: hinduFestivals,
                    muslim: muslimFestivals,
                    christian: christianFestivals
                }
            }
        });
    } catch (error) {
        console.error('Monthly Panchanga error:', error);
        res.status(500).json({ error: error.message || 'Calculation failed' });
    }
});

module.exports = router;

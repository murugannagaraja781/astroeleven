// routes/rasiEng/charts.js
const express = require('express');
const { DateTime } = require('luxon');
const { swissEph } = require('../../utils/rasiEng/swisseph');
const { getPlanetsWithDetails, getHouseCusps, getKPDetails, getPlanetHouse, getNavamsaSign } = require('../../utils/rasiEng/calculations');
const { getKPSignificators } = require('../../utils/rasiEng/kpCalculations');
const { getVimshottariDasha, getFullVimshottariDashaTree } = require('../../utils/rasiEng/dashaCalculations');
const { getPanchanga, getMuhurtas } = require('../../utils/rasiEng/panchangaCalc');
const { getTamilDate } = require('../../utils/rasiEng/tamilDate');

const router = express.Router();

// Helper function to format longitude as degrees/minutes/seconds
function formatLongitude(longitude) {
    const degInSign = (longitude % 30 + 30) % 30;
    const deg = Math.floor(degInSign);
    const minFloat = (degInSign - deg) * 60;
    const min = Math.floor(minFloat);
    const sec = Math.round((minFloat - min) * 60);
    return `${deg}° ${min}' ${sec}"`;
}

// Tamil names mapping
const TAMIL_PLANET_NAMES = {
    'Sun': 'சூரியன்',
    'Moon': 'சந்திரன்',
    'Mars': 'செவ்வாய்',
    'Mercury': 'புதன்',
    'Jupiter': 'குரு',
    'Venus': 'சுக்கிரன்',
    'Saturn': 'சனி',
    'Rahu': 'ராகு',
    'Ketu': 'கேது',
    'Ascendant': 'லக்னம்',
    'Mandi': 'மாந்தி'
};

const TAMIL_SIGN_NAMES = {
    'Aries': 'மேஷம்',
    'Taurus': 'ரிஷபம்',
    'Gemini': 'மிதுனம்',
    'Cancer': 'கடகம்',
    'Leo': 'சிம்மம்',
    'Virgo': 'கன்னி',
    'Libra': 'துலாம்',
    'Scorpio': 'விருச்சிகம்',
    'Sagittarius': 'தனுசு',
    'Capricorn': 'மகரம்',
    'Aquarius': 'கும்பம்',
    'Pisces': 'மீனம்'
};

// Classical Tamil Lagna Subhar / Paavar / Badhaka / Maraka Rules (பராசர முறை)
const LAGNA_RULES = {
    0: { // Aries (மேஷம்) - Chara
        subhar: "செவ்வாய், சூரியன், குரு",
        paavar: "புதன், சுக்கிரன், சனி",
        samar: "சந்திரன்",
        badhakathipathi: "சனி",
        badhaka: "சனி",
        marakathipathi: "சுக்கிரன், புதன்",
        maraka: "சுக்கிரன், புதன்"
    },
    1: { // Taurus (ரிஷபம்) - Sthira
        subhar: "சுக்கிரன், புதன், சனி",
        paavar: "செவ்வாய், சந்திரன், குரு",
        samar: "சூரியன்",
        badhakathipathi: "சனி",
        badhaka: "சனி",
        marakathipathi: "செவ்வாய், குரு",
        maraka: "செவ்வாய், குரு"
    },
    2: { // Gemini (மிதுனம்) - Ubhaya
        subhar: "புதன், சுக்கிரன், சனி",
        paavar: "செவ்வாய், சூரியன், குரு",
        samar: "சந்திரன்",
        badhakathipathi: "குரு",
        badhaka: "குரு",
        marakathipathi: "சந்திரன், குரு",
        maraka: "சந்திரன், குரு"
    },
    3: { // Cancer (கடகம்) - Chara
        subhar: "சந்திரன், செவ்வாய், குரு",
        paavar: "புதன், சுக்கிரன், சனி",
        samar: "சூரியன்",
        badhakathipathi: "சுக்கிரன்",
        badhaka: "சுக்கிரன்",
        marakathipathi: "சனி, புதன்",
        maraka: "சனி, புதன்"
    },
    4: { // Leo (சிம்மம்) - Sthira
        subhar: "சூரியன், செவ்வாய், குரு",
        paavar: "சுக்கிரன், சனி",
        samar: "சந்திரன், புதன்",
        badhakathipathi: "செவ்வாய்",
        badhaka: "செவ்வாய்",
        marakathipathi: "புதன், சனி",
        maraka: "புதன், சனி"
    },
    5: { // Virgo (கன்னி) - Ubhaya
        subhar: "புதன், சுக்கிரன், சனி",
        paavar: "சூரியன், செவ்வாய், குரு",
        samar: "சந்திரன்",
        badhakathipathi: "குரு",
        badhaka: "குரு",
        marakathipathi: "செவ்வாய், சூரியன்",
        maraka: "செவ்வாய், சூரியன்"
    },
    6: { // Libra (துலாம்) - Chara
        subhar: "சுக்கிரன், சனி, புதன்",
        paavar: "செவ்வாய், குரு, சூரியன்",
        samar: "சந்திரன்",
        badhakathipathi: "சூரியன்",
        badhaka: "சூரியன்",
        marakathipathi: "செவ்வாய், குரு",
        maraka: "செவ்வாய், குரு"
    },
    7: { // Scorpio (விருச்சிகம்) - Sthira
        subhar: "செவ்வாய், குரு, சூரியன்",
        paavar: "சனி, சுக்கிரன், புதன்",
        samar: "சந்திரன்",
        badhakathipathi: "சந்திரன்",
        badhaka: "சந்திரன்",
        marakathipathi: "சுக்கிரன், புதன்",
        maraka: "சுக்கிரன், புதன்"
    },
    8: { // Sagittarius (தனுசு) - Ubhaya
        subhar: "குரு, செவ்வாய், சூரியன்",
        paavar: "சந்திரன், சுக்கிரன், புதன்",
        samar: "சனி",
        badhakathipathi: "புதன்",
        badhaka: "புதன்",
        marakathipathi: "சனி, புதன்",
        maraka: "சனி, புதன்"
    },
    9: { // Capricorn (மகரம்) - Chara
        subhar: "சனி, சுக்கிரன், புதன்",
        paavar: "சூரியன், சந்திரன், செவ்வாய்",
        samar: "குரு",
        badhakathipathi: "செவ்வாய்",
        badhaka: "செவ்வாய்",
        marakathipathi: "சூரியன், சந்திரன்",
        maraka: "சூரியன், சந்திரன்"
    },
    10: { // Aquarius (கும்பம்) - Sthira
        subhar: "சனி, புதன், சுக்கிரன்",
        paavar: "சூரியன், சந்திரன், செவ்வாய்",
        samar: "குரு",
        badhakathipathi: "சுக்கிரன்",
        badhaka: "சுக்கிரன்",
        marakathipathi: "சூரியன், செவ்வாய்",
        maraka: "சூரியன், செவ்வாய்"
    },
    11: { // Pisces (மீனம்) - Ubhaya
        subhar: "குரு, சந்திரன், செவ்வாய்",
        paavar: "சூரியன், புதன், சுக்கிரன், சனி",
        samar: "-",
        badhakathipathi: "புதன்",
        badhaka: "புதன்",
        marakathipathi: "புதன், சுக்கிரன், சனி",
        maraka: "புதன், சுக்கிரன், சனி"
    }
};

// Get complete chart data in one call
router.post('/full', async (req, res) => {
    try {
        const {
            date = DateTime.now().setZone('UTC+5.5').toFormat('yyyy-MM-dd'),
            time = '12:00',
            lat = 13.0827,
            lng = 80.2707,
            timezone = 5.5,
            ayanamsa = 'Lahiri',
            name = '',
            pob = '',
            city = ''
        } = req.body;

        const offsetHours = Math.floor(Math.abs(timezone));
        const offsetMinutes = Math.round((Math.abs(timezone) - offsetHours) * 60);
        const sign = timezone >= 0 ? '+' : '-';
        const zone = `UTC${sign}${String(offsetHours).padStart(2, '0')}:${String(offsetMinutes).padStart(2, '0')}`;

        const dt = DateTime.fromFormat(`${date} ${time}`, "yyyy-MM-dd HH:mm", { zone });

        if (!dt.isValid) {
            return res.status(400).json({ error: 'Invalid date or time format' });
        }

        const utc = dt.toUTC();
        const jd = swissEph.julday(utc.year, utc.month, utc.day, utc.hour + utc.minute / 60 + utc.second / 3600);

        // PARALLEL CALCULATION: Run all independent tasks simultaneously
        const [houses, panchanga, muhurtas, tamilDateData] = await Promise.all([
            getHouseCusps(jd, lat, lng, 'Placidus', ayanamsa),
            getPanchanga(jd, lat, lng, ayanamsa),
            getMuhurtas(jd, lat, lng),
            getTamilDate(dt, ayanamsa)
        ]);

        // Map planets to include degreeFormatted as expected by App
        const rawPlanets = getPlanetsWithDetails(jd, houses.cusps, ayanamsa);
        const sun = rawPlanets.find(p => p.name === 'Sun');

        const planets = rawPlanets.map(p => {
            // Combustion logic (within ~8.5 degrees of Sun, excluding Moon/Rahu/Ketu)
            let isCombust = false;
            if (sun && p.name !== 'Sun' && p.name !== 'Moon' && p.name !== 'Rahu' && p.name !== 'Ketu') {
                const diff = Math.abs(p.longitude - sun.longitude);
                isCombust = diff < 8.5 || diff > (360 - 8.5);
            }

            return {
                ...p,
                isCombust,
                degreeFormatted: formatLongitude(p.longitude)
            };
        });

        // 10. Classical Tamil Mandi / Gulika Calculation
        let mandiSignIndex = 0;
        let mandiSignName = "Aries";
        let mandiLon = 0;
        let mandiNakName = "Ashwini";
        let mandiNakPada = 1;
        let mandiKp = { signLord: "Mars", starLord: "Ketu", subLord: "Ketu" };
        let mandiHouse = 1;

        try {
            const sunriseJd = swissEph.getSunrise(jd, lat, lng);
            const sunsetJd = swissEph.getSunset(jd, lat, lng);
            const dateInfo = swissEph.revjul(jd);
            const jsDate = new Date(dateInfo.year, dateInfo.month - 1, dateInfo.day);
            let varaIndex = jsDate.getDay(); // 0: Sun, 1: Mon, ... 6: Sat

            const dayParts = [6, 5, 4, 3, 2, 1, 0];
            const nightParts = [4, 3, 2, 1, 0, 6, 5];

            let mandiJd = jd;
            if (jd < sunriseJd) {
                // Birth before sunrise belongs to previous day's astrological vara
                varaIndex = (varaIndex + 6) % 7;
                const prevSunsetJd = swissEph.getSunset(jd - 1, lat, lng);
                const nightDuration = sunriseJd - prevSunsetJd;
                const partIndex = nightParts[varaIndex];
                mandiJd = prevSunsetJd + (partIndex * (nightDuration / 8));
            } else if (jd >= sunriseJd && jd < sunsetJd) {
                // Day birth
                const dayDuration = sunsetJd - sunriseJd;
                const partIndex = dayParts[varaIndex];
                mandiJd = sunriseJd + (partIndex * (dayDuration / 8));
            } else {
                // Night birth after sunset
                const nextSunriseJd = swissEph.getSunrise(jd + 1, lat, lng);
                const nightDuration = nextSunriseJd - sunsetJd;
                const partIndex = nightParts[varaIndex];
                mandiJd = sunsetJd + (partIndex * (nightDuration / 8));
            }

            const mandiHouses = swissEph.getHouses(mandiJd, lat, lng, 'Placidus', ayanamsa);
            mandiLon = mandiHouses.ascendant;
            const mandiSign = swissEph.getSign(mandiLon);
            const mandiNak = swissEph.getNakshatra(mandiLon);
            mandiKp = getKPDetails(mandiLon);
            mandiHouse = getPlanetHouse(mandiLon, houses.cusps);

            mandiSignIndex = mandiSign.index;
            mandiSignName = mandiSign.name;
            mandiNakName = mandiNak.name;
            mandiNakPada = mandiNak.pada;
        } catch (mandiErr) {
            console.error('[Charts] Mandi calculation fallback:', mandiErr.message);
            mandiSignIndex = (planets[0] && typeof planets[0].signIndex === 'number') ? planets[0].signIndex : 0;
            mandiSignName = (planets[0] && planets[0].signName) ? planets[0].signName : "Aries";
        }

        planets.push({
            name: "Mandi",
            tamilName: "மாந்தி",
            symbol: "மா",
            longitude: mandiLon,
            signName: mandiSignName,
            signIndex: mandiSignIndex,
            house: mandiHouse,
            nakshatra: mandiNakName,
            nakshatraName: mandiNakName,
            nakshatraPada: mandiNakPada,
            isRetrograde: false,
            isCombust: false,
            degreeFormatted: formatLongitude(mandiLon),
            signLord: mandiKp.signLord,
            starLord: mandiKp.starLord,
            subLord: mandiKp.subLord
        });

        // 10b. Include Ascendant in planets list for direct mobile/web chart access
        if (houses && houses.ascendantDetails) {
            planets.unshift({
                id: 100,
                name: "Ascendant",
                longitude: houses.ascendant,
                latitude: 0,
                distance: 1,
                speed: 0,
                isRetrograde: false,
                sign: houses.ascendantDetails.signName,
                signName: houses.ascendantDetails.signName,
                signIndex: typeof houses.ascendant === 'number' ? Math.floor(houses.ascendant / 30) : 0,
                house: 1,
                nakshatra: houses.ascendantDetails.nakshatra || "Ashwini",
                nakshatraName: houses.ascendantDetails.nakshatra || "Ashwini",
                nakshatraIndex: houses.ascendantDetails.nakshatraIndex || 0,
                nakshatraPada: houses.ascendantDetails.nakshatraPada || 1,
                signLord: houses.ascendantDetails.signLord || "Mars",
                starLord: houses.ascendantDetails.starLord || "Ketu",
                subLord: houses.ascendantDetails.subLord || "Ketu",
                subSubLord: houses.ascendantDetails.subSubLord || "",
                subSubSubLord: houses.ascendantDetails.subSubSubLord || "",
                isCombust: false,
                degreeFormatted: houses.ascendantDetails.degreeFormatted || formatLongitude(houses.ascendant)
            });
        }

        // 11. Dasha Logic
        const moon = planets.find(p => p.name === 'Moon');
        const moonLon = moon ? moon.longitude : 0;
        const detailedDasha = getFullVimshottariDashaTree(moonLon, dt);

        // 12. Lagna Subhar / Paavar / Badhaka / Maraka
        const lagnaIndex = typeof houses.ascendant === 'number' ? Math.floor(houses.ascendant / 30) % 12 : 0;
        const lagnaRules = LAGNA_RULES[lagnaIndex] || {
            subhar: "செவ்வாய், சூரியன், குரு",
            paavar: "புதன், சுக்கிரன், சனி",
            samar: "சந்திரன்",
            badhaka: "சனி",
            badhakathipathi: "சனி",
            maraka: "சுக்கிரன், புதன்",
            marakathipathi: "சுக்கிரன், புதன்"
        };

        // 13. Current Running Dasha & Bhukti with Star Lords & Date Ranges
        const nowLuxon = DateTime.now();
        const moonSignName = moon ? moon.signName : 'Aries';
        const moonSignTamil = TAMIL_SIGN_NAMES[moonSignName] || moonSignName;
        const ascSignName = houses.ascendantDetails?.signName || 'Aries';
        const ascSignTamil = TAMIL_SIGN_NAMES[ascSignName] || ascSignName;
        const moonNakName = moon ? (moon.nakshatraName || moon.nakshatra) : 'Ashwini';

        let currentDashaInfo = null;
        if (Array.isArray(detailedDasha) && detailedDasha.length > 0) {
            // Find currently running Mahadasha
            const curM = detailedDasha.find(d => {
                const s = DateTime.fromISO(d.start);
                const e = DateTime.fromISO(d.end);
                return s <= nowLuxon && e >= nowLuxon;
            }) || detailedDasha[0];

            // Find currently running Bhukti
            const curB = (curM.subPeriods && Array.isArray(curM.subPeriods)) ? curM.subPeriods.find(b => {
                const s = DateTime.fromISO(b.start);
                const e = DateTime.fromISO(b.end);
                return s <= nowLuxon && e >= nowLuxon;
            }) || curM.subPeriods[0] : null;

            // Star Lords of Dasa Lord and Bhukti Lord
            const dasaPlanetObj = planets.find(p => p.name.toLowerCase() === curM.lord.toLowerCase());
            const bhuktiPlanetObj = curB ? planets.find(p => p.name.toLowerCase() === curB.lord.toLowerCase()) : null;

            const dasaStarLord = dasaPlanetObj?.starLord || 'Unknown';
            const bhuktiStarLord = bhuktiPlanetObj?.starLord || 'Unknown';

            const formatDisplayDate = (isoStr) => {
                if (!isoStr) return '';
                try {
                    return DateTime.fromISO(isoStr).toFormat('dd/MM/yyyy');
                } catch (_) {
                    return String(isoStr).slice(0, 10);
                }
            };

            const dasaStartFormatted = formatDisplayDate(curM.start);
            const dasaEndFormatted = formatDisplayDate(curM.end);
            const bhuktiStartFormatted = curB ? formatDisplayDate(curB.start) : '';
            const bhuktiEndFormatted = curB ? formatDisplayDate(curB.end) : '';

            const dasaTamil = TAMIL_PLANET_NAMES[curM.lord] || curM.lord;
            const bhuktiTamil = curB ? (TAMIL_PLANET_NAMES[curB.lord] || curB.lord) : '';
            const dasaStarLordTamil = TAMIL_PLANET_NAMES[dasaStarLord] || dasaStarLord;
            const dashaStarLordTamil = dasaStarLordTamil;
            const bhuktiStarLordTamil = TAMIL_PLANET_NAMES[bhuktiStarLord] || bhuktiStarLord;

            currentDashaInfo = {
                dasaLord: curM.lord,
                dashaLord: curM.lord,
                dasaLordTamil: dasaTamil,
                dashaLordTamil: dasaTamil,
                dasaStarLord,
                dashaStarLord: dasaStarLord,
                dasaStarLordTamil,
                dashaStarLordTamil,
                dasaStartDate: dasaStartFormatted,
                dashaStartDate: dasaStartFormatted,
                dasaEndDate: dasaEndFormatted,
                dashaEndDate: dasaEndFormatted,
                dasaDisplayText: `${dasaTamil} திசை (${dasaStartFormatted} To ${dasaEndFormatted})`,
                dashaDisplay: `${dasaTamil} திசை (${dasaStartFormatted} To ${dasaEndFormatted})`,
                dasaStarDisplayText: `திசாநாதன் நின்ற நட்சத்திர அதிபதி: ${dasaStarLordTamil}`,

                bhuktiLord: curB ? curB.lord : '',
                bhuktiLordTamil: bhuktiTamil,
                bhuktiStarLord,
                bhuktiStarLordTamil,
                bhuktiStartDate: bhuktiStartFormatted,
                bhuktiEndDate: bhuktiEndFormatted,
                bhuktiDisplayText: `${bhuktiTamil} புத்தி (${bhuktiStartFormatted} To ${bhuktiEndFormatted})`,
                bhuktiDisplay: `${bhuktiTamil} புத்தி (${bhuktiStartFormatted} To ${bhuktiEndFormatted})`,
                bhuktiStarDisplayText: `புத்திநாதன் நின்ற நட்சத்திர அதிபதி: ${bhuktiStarLordTamil}`
            };
        }

        const lagnaRoles = {
            lagna: ascSignName,
            lagnaTamil: ascSignTamil,
            subhar: lagnaRules.subhar,
            paavar: lagnaRules.paavar,
            badhaka: lagnaRules.badhaka || lagnaRules.badhakathipathi,
            badhakathipathi: lagnaRules.badhakathipathi,
            maraka: lagnaRules.maraka || lagnaRules.marakathipathi,
            marakathipathi: lagnaRules.marakathipathi,
            samar: lagnaRules.samar || "-"
        };

        // Transits (Could also be parallelized if needed)
        const now = DateTime.now().toUTC();
        const transitJD = swissEph.julday(now.year, now.month, now.day, now.hour + now.minute / 60);
        const rawTransits = swissEph.getAllPlanets(transitJD, ayanamsa);
        const transits = rawTransits.map(t => {
            const s = swissEph.getSign(t.longitude);
            return {
                name: t.name,
                signName: s.name,
                isRetrograde: t.isRetrograde
            };
        });

        const navamsaPlanets = planets.map(p => {
            return {
                name: p.name,
                signName: getNavamsaSign(p.longitude)
            };
        });
        if (houses && houses.ascendant !== undefined && !navamsaPlanets.some(p => p.name === 'Ascendant')) {
            navamsaPlanets.push({
                name: "Ascendant",
                signName: getNavamsaSign(houses.ascendant)
            });
        }

        const clientBirthDistrict = pob || city || 'Tamil Nadu';
        const formattedTob = dt.toFormat('hh:mm a');

        const chartData = {
            planets,
            houses,
            panchanga: { ...panchanga, ...muhurtas },
            dasha: detailedDasha,
            transits,
            tamilDate: tamilDateData,
            navamsa: { planets: navamsaPlanets },
            clientInfo: {
                name: name || 'Client',
                dob: dt.toFormat('dd/MM/yyyy'),
                tob: formattedTob,
                tobFormatted: formattedTob,
                pob: clientBirthDistrict,
                rasi: moonSignTamil,
                lagna: ascSignTamil,
                nakshatra: moonNakName
            },
            currentDashaInfo,
            lagnaRoles: lagnaRoles
        };

        res.json({
            success: true,
            version: "v5.6",
            data: chartData
        });
    } catch (error) {
        console.error('Charts Full API error:', error);
        res.status(500).json({ error: error.message || 'Calculation failed' });
    }
});

// Quick chart (planets and houses only)
router.post('/quick', (req, res) => {
    try {
        const { date, time, lat, lng, timezone = 5.5, ayanamsa = 'Lahiri' } = req.body;

        if (!date || !time || lat === undefined || lng === undefined) {
            return res.status(400).json({ error: 'Missing required fields: date, time, lat, lng' });
        }

        const offsetHours = Math.floor(Math.abs(timezone));
        const offsetMinutes = Math.round((Math.abs(timezone) - offsetHours) * 60);
        const sign = timezone >= 0 ? '+' : '-';
        const zone = `UTC${sign}${String(offsetHours).padStart(2, '0')}:${String(offsetMinutes).padStart(2, '0')}`;

        const dt = DateTime.fromFormat(`${date} ${time}`, "yyyy-MM-dd HH:mm", { zone });

        if (!dt.isValid) {
            return res.status(400).json({ error: 'Invalid date or time format' });
        }

        const utc = dt.toUTC();
        const jd = swissEph.julday(utc.year, utc.month, utc.day, utc.hour + utc.minute / 60 + utc.second / 3600);

        const houses = getHouseCusps(jd, lat, lng, 'Placidus', ayanamsa);
        const planets = getPlanetsWithDetails(jd, houses.cusps, ayanamsa);

        res.json({
            success: true,
            data: {
                planets,
                houses
            }
        });
    } catch (error) {
        console.error('Charts Quick API error:', error);
        res.status(500).json({ error: error.message || 'Calculation failed' });
    }
});

module.exports = router;

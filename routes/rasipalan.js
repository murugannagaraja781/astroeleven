const express = require('express');
const router = express.Router();
const { DateTime } = require('luxon');
const { fetchDailyHoroscope } = require('../utils/rasiEng/horoscopeData');

// Mapping for canonical rasi names if needed, but we'll try to stick to what the data provides
// or what the app previously used if possible.
const SIGN_NAME_MAP = {
    "Aries": "Mesham",
    "Taurus": "Rishabam",
    "Gemini": "Mithunam",
    "Cancer": "Kadagam",
    "Leo": "Simmam",
    "Virgo": "Kanni",
    "Libra": "Thulaam",
    "Scorpio": "Viruchigam",
    "Sagittarius": "Dhanusu",
    "Capricorn": "Magaram",
    "Aquarius": "Kumbam",
    "Pisces": "Meenam"
};

const RASI_CLASSIFICATIONS = [
    { subhargal: "செவ்வாய், சூரியன், குரு", paabargal: "புதன், சுக்கிரன், சனி", maaragar: "சுக்கிரன், புதன்" }, // Aries (மேஷம்)
    { subhargal: "சுக்கிரன், புதன், சனி", paabargal: "செவ்வாய், சந்திரன், குரு", maaragar: "செவ்வாய், குரு" }, // Taurus (ரிஷபம்)
    { subhargal: "புதன், சுக்கிரன், சனி", paabargal: "செவ்வாய், சூரியன், குரு", maaragar: "சந்திரன், குரு" }, // Gemini (மிதுனம்)
    { subhargal: "சந்திரன், செவ்வாய், குரு", paabargal: "புதன், சுக்கிரன், சனி", maaragar: "சனி, புதன்" }, // Cancer (கடகம்)
    { subhargal: "சூரியன், செவ்வாய், குரு", paabargal: "சுக்கிரன், சனி", maaragar: "புதன், சனி" }, // Leo (சிம்மம்)
    { subhargal: "புதன், சுக்கிரன், சனி", paabargal: "சூரியன், செவ்வாய், குரு", maaragar: "செவ்வாய், குரு" }, // Virgo (கன்னி)
    { subhargal: "சுக்கிரன், சனி, புதன்", paabargal: "செவ்வாய், குரு, சூரியன்", maaragar: "செவ்வாய், குரு" }, // Libra (துலாம்)
    { subhargal: "செவ்வாய், குரு, சூரியன்", paabargal: "சனி, சுக்கிரன், புதன்", maaragar: "சுக்கிரன், புதன்" }, // Scorpio (விருச்சிகம்)
    { subhargal: "குரு, செவ்வாய், சூரியன்", paabargal: "சந்திரன், சுக்கிரன், புதன்", maaragar: "சனி, புதன்" }, // Sagittarius (தனுசு)
    { subhargal: "சனி, சுக்கிரன், புதன்", paabargal: "சூரியன், சந்திரன், செவ்வாய்", maaragar: "சூரியன், சந்திரன்" }, // Capricorn (மகரம்)
    { subhargal: "சனி, புதன், சுக்கிரன்", paabargal: "சூரியன், சந்திரன், செவ்வாய்", maaragar: "சூரியன், செவ்வாய்" }, // Aquarius (கும்பம்)
    { subhargal: "குரு, சந்திரன், செவ்வாய்", paabargal: "சூரியன், புதன், சுக்கிரன், சனி", maaragar: "புதன், சுக்கிரன், சனி" } // Pisces (மீனம்)
];

router.get('/', async (req, res) => {
    try {
        const today = DateTime.now().setZone('Asia/Kolkata').toFormat('yyyy-MM-dd');
        console.log(`Fetching Rasipalan data for: ${today}`);

        const externalData = await fetchDailyHoroscope(today);

        if (externalData && Array.isArray(externalData)) {
            // Map external data to our app's expected format (RasipalanModel.kt)
            const mappedData = externalData.map((item, index) => {
                // Better guard against "undefined" strings or missing values
                const isValidStr = (v) => v && v !== 'undefined' && v !== 'null';
                
                const predictionTa = (isValidStr(item.prediction_ta) ? item.prediction_ta : null) || 
                                     (isValidStr(item.forecast_ta) ? item.forecast_ta : null) || 
                                     (isValidStr(item.horoscope_ta) ? item.horoscope_ta : null) || 
                                     (isValidStr(item.horoscope) ? item.horoscope : null) || 
                                     "இன்றைய நாள் சிறப்பாக இருக்கும்.";
                                     
                const predictionEn = (isValidStr(item.prediction_en) ? item.prediction_en : null) || 
                                     (isValidStr(item.forecast_en) ? item.forecast_en : null) || 
                                     (isValidStr(item.horoscope_en) ? item.horoscope_en : null) || 
                                     (isValidStr(item.horoscope) ? item.horoscope : null) || 
                                     "Today will be a great day.";

                const classification = RASI_CLASSIFICATIONS[index] || { subhargal: "", paabargal: "", maaragar: "" };

                return {
                    signId: index + 1,
                    signNameEn: item.sign_en || SIGN_NAME_MAP[Object.keys(SIGN_NAME_MAP)[index]] || "Sign",
                    signNameTa: item.sign_ta || "ராசி",
                    date: today,
                    prediction: {
                        ta: predictionTa,
                        en: predictionEn
                    },
                    details: {
                        career: (isValidStr(item.career_ta) ? item.career_ta : null) || predictionTa,
                        finance: (isValidStr(item.finance_ta) ? item.finance_ta : null) || predictionTa,
                        health: (isValidStr(item.health_ta) ? item.health_ta : null) || predictionTa
                    },
                    lucky: {
                        number: item.lucky_number || item.lucky_num || "-",
                        color: {
                            ta: item.lucky_color_ta || item.lucky_color || "-",
                            en: item.lucky_color_en || item.lucky_color || "-"
                        },
                        luckyTime: item.lucky_time || item.amrita_time || item.amirtha_time || "-",
                        unluckyTime: item.unlucky_time || item.rahukalam || item.rahu_kal_ta || "-"
                    },
                    subhargal: classification.subhargal,
                    paabargal: classification.paabargal,
                    maaragar: classification.maaragar
                };
            });

            console.log("Fetched and mapped Rasi Engine horoscope data for Android successfully.");
            // Android app expects the array directly
            return res.json(mappedData);
        } else {
            console.error("Failed to fetch horoscope data from Rasi Engine source.");
            return res.status(500).json([]);
        }
    } catch (error) {
        console.error("Error in Rasipalan route:", error.message);
        return res.status(500).json([]);
    }
});

module.exports = router;

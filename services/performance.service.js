const mongoose = require('../utils/mongoose-mysql');
const pool = mongoose.pool;
const User = require('../models/User');

/**
 * Astrologer Performance & Income Monitoring Service
 * Tracks 12 Core Astrological Performance & Financial Metrics:
 * 1. Astrologer Name
 * 2. Total Number of Clients (Unique Clients)
 * 3. Daily Completed Calls
 * 4. Daily Completed Chats
 * 5. Total Call Duration (minutes)
 * 6. Total Chat Duration (minutes)
 * 7. Total Consultation Earnings (Total Charged)
 * 8. Astrologer Commission Amount (Total Earned)
 * 9. Platform Revenue (Consultation Earnings - Commission)
 * 10. Daily Earnings (Today's Commission)
 * 11. Weekly Earnings (Last 7 Days Commission)
 * 12. Monthly Earnings (Last 30 Days Commission)
 */

function getTimeBoundaries(days) {
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const weekStart = todayStart - (6 * 24 * 60 * 60 * 1000);
    const monthStart = todayStart - (29 * 24 * 60 * 60 * 1000);

    let periodStart = 0;
    const strDays = String(days).toLowerCase();
    if (strDays === '1' || strDays === 'today' || strDays === 'daily') {
        periodStart = todayStart;
    } else if (strDays === '7' || strDays === 'week' || strDays === 'weekly') {
        periodStart = weekStart;
    } else if (strDays === '30' || strDays === 'month' || strDays === 'monthly') {
        periodStart = monthStart;
    } else if (strDays === 'all' || strDays === '0') {
        periodStart = 0;
    } else {
        const num = parseInt(days);
        if (!isNaN(num) && num > 0) {
            periodStart = Date.now() - (num * 24 * 60 * 60 * 1000);
        }
    }

    return { todayStart, weekStart, monthStart, periodStart };
}

const getAllAstrologersPerformance = async (days = '30') => {
    try {
        const { todayStart, weekStart, monthStart, periodStart } = getTimeBoundaries(days);

        // Fetch all registered astrologers
        const astros = await User.find({ role: 'astrologer' }, 'userId name phone image ratePerMinute').lean();
        const astroMap = new Map();
        astros.forEach(a => astroMap.set(a.userId, a));

        const astroIds = Array.from(astroMap.keys());
        if (astroIds.length === 0) {
            return { ok: true, list: [], summary: { totalAstros: 0, totalConsultationRevenue: 0, totalPlatformRevenue: 0, totalCommission: 0 } };
        }

        // Comprehensive 12-Metric Aggregation SQL Query
        const sql = `
            SELECT 
                astrologerId,
                COUNT(DISTINCT clientId) AS totalUniqueClients,
                -- Daily Completed Calls & Chats (Today)
                SUM(CASE WHEN startTime >= ? AND type != 'chat' THEN 1 ELSE 0 END) AS dailyCompletedCalls,
                SUM(CASE WHEN startTime >= ? AND type = 'chat' THEN 1 ELSE 0 END) AS dailyCompletedChats,
                -- Calls & Chats in the Filtered Period
                SUM(CASE WHEN type != 'chat' THEN 1 ELSE 0 END) AS periodCompletedCalls,
                SUM(CASE WHEN type = 'chat' THEN 1 ELSE 0 END) AS periodCompletedChats,
                COUNT(*) AS periodTotalSessions,
                -- Durations in Minutes (Converted from seconds / milliseconds safely)
                IFNULL(SUM(CASE WHEN type != 'chat' THEN (CASE WHEN duration >= 1000 THEN duration / 60000 ELSE duration / 60 END) ELSE 0 END), 0) AS totalCallDurationMins,
                IFNULL(SUM(CASE WHEN type = 'chat' THEN (CASE WHEN duration >= 1000 THEN duration / 60000 ELSE duration / 60 END) ELSE 0 END), 0) AS totalChatDurationMins,
                IFNULL(SUM(CASE WHEN duration >= 1000 THEN duration / 60000 ELSE duration / 60 END), 0) AS totalDurationMins,
                -- Consultation Revenue, Commission, and Platform Revenue
                IFNULL(SUM(totalCharged), 0) AS totalConsultationEarnings,
                IFNULL(SUM(totalEarned), 0) AS astrologerCommissionAmount,
                IFNULL(SUM(totalCharged - totalEarned), 0) AS platformRevenue,
                -- Daily, Weekly, Monthly Breakdown Earnings
                IFNULL(SUM(CASE WHEN startTime >= ? THEN totalEarned ELSE 0 END), 0) AS dailyEarnings,
                IFNULL(SUM(CASE WHEN startTime >= ? THEN totalEarned ELSE 0 END), 0) AS weeklyEarnings,
                IFNULL(SUM(CASE WHEN startTime >= ? THEN totalEarned ELSE 0 END), 0) AS monthlyEarnings
            FROM sessions
            WHERE status = 'ended' AND startTime >= ?
            GROUP BY astrologerId
            ORDER BY totalConsultationEarnings DESC
        `;

        const [rows] = await pool.execute(sql, [todayStart, todayStart, todayStart, weekStart, monthStart, periodStart]);

        let totalRevenueSum = 0;
        let totalCommissionSum = 0;
        let totalPlatformSum = 0;
        let totalMinutesSum = 0;

        const list = rows.map((r, idx) => {
            const astro = astroMap.get(r.astrologerId) || {};
            const rev = Number(r.totalConsultationEarnings || 0);
            const comm = Number(r.astrologerCommissionAmount || 0);
            const plat = Number(r.platformRevenue || 0);
            const mins = Number(r.totalDurationMins || 0);

            totalRevenueSum += rev;
            totalCommissionSum += comm;
            totalPlatformSum += plat;
            totalMinutesSum += mins;

            return {
                rank: idx + 1,
                astrologerId: r.astrologerId,
                name: astro.name || 'Astrologer',
                phone: astro.phone || 'N/A',
                image: astro.image || '',
                ratePerMinute: astro.ratePerMinute || 0,
                // 12 Core Metrics
                totalClients: Number(r.totalUniqueClients || 0),
                dailyCompletedCalls: Number(r.dailyCompletedCalls || 0),
                dailyCompletedChats: Number(r.dailyCompletedChats || 0),
                periodCompletedCalls: Number(r.periodCompletedCalls || 0),
                periodCompletedChats: Number(r.periodCompletedChats || 0),
                totalCallDurationMins: Number(r.totalCallDurationMins || 0).toFixed(1),
                totalChatDurationMins: Number(r.totalChatDurationMins || 0).toFixed(1),
                totalDurationMins: mins.toFixed(1),
                totalConsultationEarnings: rev.toFixed(2),
                astrologerCommissionAmount: comm.toFixed(2),
                platformRevenue: plat.toFixed(2),
                dailyEarnings: Number(r.dailyEarnings || 0).toFixed(2),
                weeklyEarnings: Number(r.weeklyEarnings || 0).toFixed(2),
                monthlyEarnings: Number(r.monthlyEarnings || 0).toFixed(2),
                sessionCount: Number(r.periodTotalSessions || 0)
            };
        });

        // Add astrologers with 0 sessions if needed so admin sees all active astrologers
        astroIds.forEach(id => {
            if (!list.some(item => item.astrologerId === id)) {
                const astro = astroMap.get(id) || {};
                list.push({
                    rank: list.length + 1,
                    astrologerId: id,
                    name: astro.name || 'Astrologer',
                    phone: astro.phone || 'N/A',
                    image: astro.image || '',
                    ratePerMinute: astro.ratePerMinute || 0,
                    totalClients: 0,
                    dailyCompletedCalls: 0,
                    dailyCompletedChats: 0,
                    periodCompletedCalls: 0,
                    periodCompletedChats: 0,
                    totalCallDurationMins: '0.0',
                    totalChatDurationMins: '0.0',
                    totalDurationMins: '0.0',
                    totalConsultationEarnings: '0.00',
                    astrologerCommissionAmount: '0.00',
                    platformRevenue: '0.00',
                    dailyEarnings: '0.00',
                    weeklyEarnings: '0.00',
                    monthlyEarnings: '0.00',
                    sessionCount: 0
                });
            }
        });

        return {
            ok: true,
            period: days,
            list,
            summary: {
                totalAstros: list.length,
                totalConsultationRevenue: totalRevenueSum.toFixed(2),
                totalPlatformRevenue: totalPlatformSum.toFixed(2),
                totalCommission: totalCommissionSum.toFixed(2),
                totalMinutes: totalMinutesSum.toFixed(1)
            }
        };
    } catch (error) {
        console.error('[PerformanceService] All Astros Error:', error);
        return { ok: false, error: error.message };
    }
};

const getAstrologerPerformance = async (astrologerId, days = '30') => {
    try {
        const { todayStart, weekStart, monthStart, periodStart } = getTimeBoundaries(days);

        const astro = await User.findOne({ userId: astrologerId }, 'userId name phone image ratePerMinute email').lean();
        if (!astro) {
            return { ok: false, error: 'Astrologer not found' };
        }

        // 1. Metric Aggregations
        const sql = `
            SELECT 
                COUNT(DISTINCT clientId) AS totalUniqueClients,
                SUM(CASE WHEN startTime >= ? AND type != 'chat' THEN 1 ELSE 0 END) AS dailyCompletedCalls,
                SUM(CASE WHEN startTime >= ? AND type = 'chat' THEN 1 ELSE 0 END) AS dailyCompletedChats,
                SUM(CASE WHEN type != 'chat' THEN 1 ELSE 0 END) AS totalCalls,
                SUM(CASE WHEN type = 'chat' THEN 1 ELSE 0 END) AS totalChats,
                COUNT(*) AS totalSessions,
                IFNULL(SUM(CASE WHEN type != 'chat' THEN (CASE WHEN duration >= 1000 THEN duration / 60000 ELSE duration / 60 END) ELSE 0 END), 0) AS totalCallDurationMins,
                IFNULL(SUM(CASE WHEN type = 'chat' THEN (CASE WHEN duration >= 1000 THEN duration / 60000 ELSE duration / 60 END) ELSE 0 END), 0) AS totalChatDurationMins,
                IFNULL(SUM(CASE WHEN duration >= 1000 THEN duration / 60000 ELSE duration / 60 END), 0) AS totalDurationMins,
                IFNULL(SUM(totalCharged), 0) AS totalConsultationEarnings,
                IFNULL(SUM(totalEarned), 0) AS astrologerCommissionAmount,
                IFNULL(SUM(totalCharged - totalEarned), 0) AS platformRevenue,
                IFNULL(SUM(CASE WHEN startTime >= ? THEN totalEarned ELSE 0 END), 0) AS dailyEarnings,
                IFNULL(SUM(CASE WHEN startTime >= ? THEN totalEarned ELSE 0 END), 0) AS weeklyEarnings,
                IFNULL(SUM(CASE WHEN startTime >= ? THEN totalEarned ELSE 0 END), 0) AS monthlyEarnings
            FROM sessions
            WHERE astrologerId = ? AND status = 'ended' AND startTime >= ?
        `;

        const [metricRows] = await pool.execute(sql, [todayStart, todayStart, todayStart, weekStart, monthStart, astrologerId, periodStart]);
        const m = metricRows[0] || {};

        // 2. Status Breakdown (for response rate chart)
        const [visRows] = await pool.execute(
            `SELECT status AS _id, COUNT(*) AS count FROM sessions WHERE astrologerId = ? AND startTime >= ? GROUP BY status`,
            [astrologerId, periodStart]
        );
        const callBreakdown = {};
        let totalReqs = 0;
        visRows.forEach(r => {
            callBreakdown[r._id] = Number(r.count);
            totalReqs += Number(r.count);
        });

        // 3. Recent 15 Consultations with Client details
        const recentSql = `
            SELECT sessionId, clientId, type, duration, startTime, endTime, totalCharged, totalEarned
            FROM sessions
            WHERE astrologerId = ? AND status = 'ended'
            ORDER BY startTime DESC
            LIMIT 15
        `;
        const [recentRows] = await pool.execute(recentSql, [astrologerId]);

        // Enrich client names
        const clientIds = [...new Set(recentRows.map(r => r.clientId).filter(Boolean))];
        let clientMap = new Map();
        if (clientIds.length > 0) {
            const clients = await User.find({ userId: { $in: clientIds } }, 'userId name phone').lean();
            clients.forEach(c => clientMap.set(c.userId, c));
        }

        const recentSessions = recentRows.map(s => {
            const client = clientMap.get(s.clientId) || {};
            const mins = s.duration ? (s.duration >= 1000 ? s.duration / 60000 : s.duration / 60) : 0;
            return {
                sessionId: s.sessionId,
                clientName: client.name || 'Client',
                clientPhone: client.phone || 'N/A',
                type: s.type || 'call',
                durationMinutes: mins.toFixed(1),
                totalCharged: Number(s.totalCharged || 0).toFixed(2),
                totalEarned: Number(s.totalEarned || 0).toFixed(2),
                date: s.startTime ? new Date(s.startTime).toLocaleString('en-IN') : ''
            };
        });

        const totalChargedNum = Number(m.totalConsultationEarnings || 0);
        const totalEarnedNum = Number(m.astrologerCommissionAmount || 0);
        const platformRevenueNum = Number(m.platformRevenue || 0);

        return {
            ok: true,
            astrologer: {
                userId: astro.userId,
                name: astro.name,
                phone: astro.phone,
                email: astro.email,
                image: astro.image,
                ratePerMinute: astro.ratePerMinute
            },
            period: days,
            metrics: {
                astrologerName: astro.name,
                totalClients: Number(m.totalUniqueClients || 0),
                dailyCompletedCalls: Number(m.dailyCompletedCalls || 0),
                dailyCompletedChats: Number(m.dailyCompletedChats || 0),
                totalCalls: Number(m.totalCalls || 0),
                totalChats: Number(m.totalChats || 0),
                totalSessions: Number(m.totalSessions || 0),
                totalCallDurationMins: Number(m.totalCallDurationMins || 0).toFixed(1),
                totalChatDurationMins: Number(m.totalChatDurationMins || 0).toFixed(1),
                totalDurationMins: Number(m.totalDurationMins || 0).toFixed(1),
                totalConsultationEarnings: totalChargedNum.toFixed(2),
                astrologerCommissionAmount: totalEarnedNum.toFixed(2),
                platformRevenue: platformRevenueNum.toFixed(2),
                dailyEarnings: Number(m.dailyEarnings || 0).toFixed(2),
                weeklyEarnings: Number(m.weeklyEarnings || 0).toFixed(2),
                monthlyEarnings: Number(m.monthlyEarnings || 0).toFixed(2),
                workDepth: {
                    avgSessionDuration: m.totalSessions ? (Number(m.totalDurationMins || 0) / Number(m.totalSessions)).toFixed(1) : '0.0',
                    totalMinutes: Number(m.totalDurationMins || 0).toFixed(0),
                    responseRate: totalReqs > 0 ? (((callBreakdown['ended'] || 0) / totalReqs) * 100).toFixed(1) + '%' : '0%'
                },
                earnings: {
                    totalPayout: totalEarnedNum.toFixed(2),
                    totalRevenue: totalChargedNum.toFixed(2),
                    adminShare: platformRevenueNum.toFixed(2)
                },
                callBreakdown
            },
            recentSessions
        };
    } catch (error) {
        console.error('[PerformanceService] Single Astro Error:', error);
        return { ok: false, error: error.message };
    }
};

module.exports = {
    getAstrologerPerformance,
    getAllAstrologersPerformance
};

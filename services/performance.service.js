const mongoose = require('../utils/mongoose-mysql');
const pool = mongoose.pool;
const User = require('../models/User');

/**
 * Astrologer Performance Service (MySQL Edition)
 * Tracks work depth, earnings, and engagement metrics
 */

const getAstrologerPerformance = async (astrologerId, days = 30) => {
    try {
        const startTimeLimit = Date.now() - (days * 24 * 60 * 60 * 1000);

        // 1. Core Session Metrics SQL
        const sessionSql = `
            SELECT 
                IFNULL(SUM(duration / 60), 0) AS totalMinutes,
                IFNULL(AVG(duration / 60), 0) AS avgMinutes,
                IFNULL(SUM(totalEarned), 0) AS totalEarned,
                IFNULL(SUM(totalCharged), 0) AS totalCharged,
                COUNT(*) AS sessionCount,
                SUM(CASE WHEN type = 'unlimited' THEN 1 ELSE 0 END) AS unlimitedCount,
                SUM(CASE WHEN type != 'unlimited' THEN 1 ELSE 0 END) AS slabCount
            FROM sessions
            WHERE astrologerId = ? AND status = 'ended' AND startTime >= ?
        `;
        const [sessionRows] = await pool.execute(sessionSql, [astrologerId, startTimeLimit]);
        const overview = sessionRows[0] || {
            totalMinutes: 0,
            avgMinutes: 0,
            totalEarned: 0,
            totalCharged: 0,
            sessionCount: 0,
            unlimitedCount: 0,
            slabCount: 0
        };

        // Ensure numbers
        overview.totalMinutes = Number(overview.totalMinutes);
        overview.avgMinutes = Number(overview.avgMinutes);
        overview.totalEarned = Number(overview.totalEarned);
        overview.totalCharged = Number(overview.totalCharged);
        overview.sessionCount = Number(overview.sessionCount);
        overview.unlimitedCount = Number(overview.unlimitedCount);
        overview.slabCount = Number(overview.slabCount);

        // 2. Retention Rate (Repeat Clients) SQL
        const retentionSql = `
            SELECT 
                COUNT(DISTINCT clientId) AS totalUniqueClients,
                SUM(CASE WHEN sessionCount > 1 THEN 1 ELSE 0 END) AS repeatClients
            FROM (
                SELECT clientId, COUNT(*) AS sessionCount
                FROM sessions
                WHERE astrologerId = ? AND status = 'ended' AND startTime >= ?
                GROUP BY clientId
            ) AS client_sessions
        `;
        const [retentionRows] = await pool.execute(retentionSql, [astrologerId, startTimeLimit]);
        const retention = retentionRows[0] || { totalUniqueClients: 0, repeatClients: 0 };
        retention.totalUniqueClients = Number(retention.totalUniqueClients);
        retention.repeatClients = Number(retention.repeatClients);

        // 3. Visibility / Response Rate SQL
        const visibilitySql = `
            SELECT status AS _id, COUNT(*) AS count
            FROM sessions
            WHERE astrologerId = ? AND startTime >= ?
            GROUP BY status
        `;
        const [visibilityRows] = await pool.execute(visibilitySql, [astrologerId, startTimeLimit]);
        
        const visibilityMap = {};
        let totalRequests = 0;
        visibilityRows.forEach(s => {
            visibilityMap[s._id] = Number(s.count);
            totalRequests += Number(s.count);
        });

        // Calculate final percentages
        const retentionRate = retention.totalUniqueClients > 0 
            ? ((retention.repeatClients / retention.totalUniqueClients) * 100).toFixed(1) 
            : 0;

        const responseRate = totalRequests > 0 
            ? (((visibilityMap['ended'] || 0) / totalRequests) * 100).toFixed(1) 
            : 0;

        return {
            ok: true,
            astrologerId,
            periodDays: days,
            metrics: {
                workDepth: {
                    avgSessionDuration: overview.avgMinutes.toFixed(1),
                    totalMinutes: overview.totalMinutes.toFixed(0),
                    retentionRate: retentionRate + "%",
                    responseRate: responseRate + "%",
                    sessionCount: overview.sessionCount
                },
                earnings: {
                    totalPayout: overview.totalEarned.toFixed(2),
                    totalRevenue: overview.totalCharged.toFixed(2),
                    adminShare: (overview.totalCharged - overview.totalEarned).toFixed(2),
                    slabEarnings: (overview.totalEarned * (overview.slabCount / (overview.sessionCount || 1))).toFixed(2),
                    unlimitedEarnings: (overview.totalEarned * (overview.unlimitedCount / (overview.sessionCount || 1))).toFixed(2)
                },
                callBreakdown: visibilityMap
            }
        };

    } catch (error) {
        console.error("[PerformanceService] Error:", error);
        return { ok: false, error: error.message };
    }
};

const getAllAstrologersPerformance = async (days = 30) => {
    try {
        const startTimeLimit = Date.now() - (days * 24 * 60 * 60 * 1000);
        
        // Find all astrologers
        const astros = await User.find({ role: 'astrologer' }, 'userId name image');
        const astroIds = astros.map(a => a.userId);

        if (astroIds.length === 0) {
            return { ok: true, list: [] };
        }

        // Use parameterized IN query
        const resultsSql = `
            SELECT 
                astrologerId AS _id,
                IFNULL(SUM(duration / 60), 0) AS totalMinutes,
                IFNULL(SUM(totalEarned), 0) AS totalEarned,
                COUNT(*) AS sessionCount,
                COUNT(DISTINCT clientId) AS uniqueClientsCount
            FROM sessions
            WHERE astrologerId IN (${astroIds.map(() => '?').join(', ')}) AND status = 'ended' AND startTime >= ?
            GROUP BY astrologerId
            ORDER BY totalEarned DESC
        `;
        const [results] = await pool.execute(resultsSql, [...astroIds, startTimeLimit]);

        const performanceData = results.map(r => {
            const user = astros.find(a => a.userId === r._id);
            return {
                userId: r._id,
                name: user ? user.name : 'Unknown',
                image: user ? user.image : '',
                totalMinutes: Number(r.totalMinutes).toFixed(1),
                totalEarned: Number(r.totalEarned).toFixed(2),
                sessionCount: Number(r.sessionCount),
                clientCount: Number(r.uniqueClientsCount)
            };
        });

        return { ok: true, list: performanceData };

    } catch (error) {
        console.error("[PerformanceService] All Astros Error:", error);
        return { ok: false, error: error.message };
    }
};

module.exports = {
    getAstrologerPerformance,
    getAllAstrologersPerformance
};

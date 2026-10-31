const User = require('../models/User');
const Session = require('../models/Session');
const PairMonth = require('../models/PairMonth');
const BillingLedger = require('../models/BillingLedger');
const crypto = require('crypto');
const { activeSessions, userActiveSession, sessionDisconnectTimeouts } = require('./socketStore');
const { broadcastAstroUpdate } = require('./astrologer.service');

let SLAB_RATES = {
    1: 0.30,
    2: 0.35,
    3: 0.40,
    4: 0.50
};

let io;
const setIo = (ioInstance) => { io = ioInstance; };

async function syncWalletBalance(userId) {
    try {
        const user = await User.findOne({ userId });
        if (!user) return null;
        return Number(user.walletBalance || 0);
    } catch (e) {
        console.error('[Billing] syncWalletBalance error:', e.message);
    }
    return null;
}

function getRateForSession(astro, sessionType) {
    if (!astro) return 10;
    const type = (sessionType || '').toLowerCase();
    let rate = 0;

    if (type === 'chat') {
        rate = Number(astro.chatPrice || 0);
    } else if (type === 'audio' || type === 'call' || type === 'voice') {
        rate = Number(astro.callPrice || 0);
    } else if (type === 'video') {
        rate = Number(astro.videoPrice || 0);
    }

    // Fallback if specific rate is not set (> 0)
    if (!rate || rate <= 0) {
        if (astro.price && Number(astro.price) > 0) {
            rate = Number(astro.price);
        } else if (astro.ratePerMinute && Number(astro.ratePerMinute) > 0) {
            rate = Number(astro.ratePerMinute);
        } else {
            if (type === 'video') rate = 20;
            else if (type === 'audio' || type === 'call' || type === 'voice') rate = 15;
            else rate = 15;
        }
    }
    return Math.round(rate);
}

async function processBillingCharge(sessionId, durationSeconds, minuteIndex, type) {
    try {
        const session = await Session.findOne({ sessionId });
        if (!session) return { success: false, reason: 'session_not_found' };

        const astro = await User.findOne({ userId: session.astrologerId });
        if (!astro) return { success: false, reason: 'astro_not_found' };

        const client = await User.findOne({ userId: session.clientId });
        if (!client) return { success: false, reason: 'client_not_found' };

        const pricePerMin = getRateForSession(astro, session.type);

        const activeSess = activeSessions.get(sessionId);
        const currentSlab = activeSess?.currentSlab || (minuteIndex <= 5 ? 1 : minuteIndex <= 10 ? 2 : minuteIndex <= 15 ? 3 : 4);
        const rate = (SLAB_RATES && SLAB_RATES[currentSlab] !== undefined) ? Number(SLAB_RATES[currentSlab]) : 0.30;

        const amountToCharge = pricePerMin;
        const astroShare = Math.round((amountToCharge * rate) * 100) / 100;
        const adminShare = Math.round((amountToCharge - astroShare) * 100) / 100;

        let reason = `slab_${currentSlab}`;
        if (type === 'first_60_full') {
            reason = 'first_60';
        } else if (type === 'early_exit') {
            reason = 'first_60_min_charge';
        } else if (type === 'fraction') {
            reason = 'fraction_roundup';
        }

        console.log(`[Billing] Session ${sessionId} | Min: ${minuteIndex} | Type: ${type} | Slab: ${currentSlab} (${(rate * 100).toFixed(0)}%) | Rate: ₹${pricePerMin} | Astro: ₹${astroShare} | Admin: ₹${adminShare}`);

        const totalToDeduct = amountToCharge;
        const freshClient = await User.findOne({ userId: client.userId });
        const actualBalance = freshClient ? Number(freshClient.walletBalance || 0) : Number(client.walletBalance || 0);

        if (actualBalance >= totalToDeduct) {
            // Atomic decrement in MySQL with $gte guard to prevent negative balance
            const deductRes = await User.updateOne(
                { userId: client.userId, walletBalance: { $gte: totalToDeduct } },
                { $inc: { walletBalance: -totalToDeduct } }
            );

            if (deductRes && deductRes.modifiedCount === 0) {
                const latestClient = await User.findOne({ userId: client.userId });
                const latestBal = latestClient ? Number(latestClient.walletBalance || 0) : 0;
                console.warn(`[Billing] Concurrency guard prevented overdraft for ${client.userId}. Balance: ₹${latestBal}`);
                if (io) io.to(client.userId).emit('wallet-update', { balance: latestBal });
                return {
                    success: false,
                    reason: 'insufficient_balance',
                    remainingBalance: latestBal,
                    pricePerMin
                };
            }

            // Fetch fresh balance after atomic decrement for exact accuracy
            const freshAfterDeduct = await User.findOne({ userId: client.userId });
            const newClientBalance = freshAfterDeduct ? Number(freshAfterDeduct.walletBalance || 0) : Math.round((actualBalance - totalToDeduct) * 100) / 100;
            client.walletBalance = newClientBalance;

            let newAstroBalance = Number(astro.walletBalance || 0);
            let newAstroEarnings = Number(astro.totalEarnings || 0);
            if (astroShare > 0) {
                await User.updateOne({ userId: astro.userId }, {
                    $inc: {
                        walletBalance: astroShare,
                        totalEarnings: astroShare
                    }
                });
                const freshAstro = await User.findOne({ userId: astro.userId });
                newAstroBalance = freshAstro ? Number(freshAstro.walletBalance || 0) : Math.round((newAstroBalance + astroShare) * 100) / 100;
                newAstroEarnings = freshAstro ? Number(freshAstro.totalEarnings || 0) : Math.round((newAstroEarnings + astroShare) * 100) / 100;
                astro.walletBalance = newAstroBalance;
                astro.totalEarnings = newAstroEarnings;
            }

            // Create ledger entry with explicit valid timestamp
            await BillingLedger.create({
                billingId: crypto.randomUUID(),
                sessionId,
                minuteIndex,
                chargedToClient: amountToCharge,
                creditedToAstrologer: astroShare,
                adminAmount: adminShare,
                reason,
                createdAt: new Date()
            }).catch(e => console.error('[Billing] Ledger Creation Error:', e.message));

            if (activeSess) {
                activeSess.totalDeducted = Math.round(((activeSess.totalDeducted || 0) + totalToDeduct) * 100) / 100;
                activeSess.totalEarned = Math.round(((activeSess.totalEarned || 0) + astroShare) * 100) / 100;
            }

            if (io) {
                io.to(client.userId).emit('wallet-update', { balance: newClientBalance });
                io.to(astro.userId).emit('wallet-update', { balance: newAstroBalance, totalEarnings: newAstroEarnings });
            }

            return {
                success: true,
                remainingBalance: newClientBalance,
                pricePerMin,
                charged: amountToCharge,
                astroShare,
                adminShare
            };
        } else {
            console.warn(`[Billing] Insufficient balance for ${client.userId}. Required: ₹${totalToDeduct}, Has: ₹${actualBalance}`);
            // Keep unspent balance intact
            if (io) {
                io.to(client.userId).emit('wallet-update', { balance: actualBalance });
            }
            return {
                success: false,
                reason: 'insufficient_balance',
                remainingBalance: actualBalance,
                pricePerMin
            };
        }
    } catch (e) {
        console.error('processBillingCharge error:', e);
        return { success: false, reason: 'error', error: e.message };
    }
}

async function endSessionRecord(sessionId, broadcastAstroUpdate, endReason = 'ended') {
    let s = activeSessions.get(sessionId);
    if (s) {
        if (s._isEnding) {
            console.log(`[Billing][endSessionRecord] Session ${sessionId} already ending. Skipping duplicate call.`);
            return;
        }
        s._isEnding = true;
        if (endReason && endReason !== 'ended') {
            s.endReason = endReason;
        }
    } else {
        const dbSess = await Session.findOne({ sessionId });
        if (!dbSess || dbSess.status === 'ended') return;
        s = {
            sessionId,
            type: dbSess.type,
            users: [dbSess.fromUserId || dbSess.clientId, dbSess.toUserId || dbSess.astrologerId].filter(Boolean),
            clientId: dbSess.clientId || dbSess.fromUserId,
            astrologerId: dbSess.astrologerId || dbSess.toUserId,
            actualBillingStart: dbSess.actualBillingStart,
            totalDeducted: dbSess.totalCharged || 0,
            totalEarned: dbSess.totalEarned || 0,
            elapsedBillableSeconds: dbSess.duration ? Math.round(dbSess.duration / 1000) : 0,
            lastBilledMinute: Math.ceil((dbSess.totalCharged || 0) / (dbSess.price || 10)),
            endReason: endReason || 'ended',
            isFromDb: true
        };
    }

    const endTime = Date.now();
    let billableSeconds = s.elapsedBillableSeconds || 0;

    const isRestartRecovery = s.isFromDb && (endReason === 'server_restart' || s.endReason === 'server_restart');

    // Accurate wall-clock duration calculation
    // BUT IF RECOVERING STRANDED DB SESSION ON BOOT: Do NOT bill for downtime while server was offline!
    if (isRestartRecovery) {
        billableSeconds = s.elapsedBillableSeconds || ((s.lastBilledMinute || 1) * 60);
        console.log(`[Billing][endSessionRecord] Reconciled stranded session ${sessionId} on restart. Duration capped at ${billableSeconds}s.`);
    } else if (s.actualBillingStart && s.actualBillingStart > 0) {
        const wallClockSeconds = Math.max(0, Math.floor((endTime - s.actualBillingStart) / 1000));
        billableSeconds = Math.max(billableSeconds, wallClockSeconds);
    }
    s.elapsedBillableSeconds = billableSeconds;

    console.log(`[Billing][endSessionRecord] sessionId=${sessionId}, reason=${s.endReason || endReason}, billableSeconds=${billableSeconds}, totalDeducted=${s.totalDeducted || 0}, totalEarned=${s.totalEarned || 0}`);

    const isInsufficient = (s.endReason === 'insufficient_balance' || endReason === 'insufficient_balance' || s.endReason === 'insufficient_funds' || endReason === 'insufficient_funds');

    // If session was connected/active but never billed minute 1, charge minute 1 minimum (unless ended due to insufficient_balance or restart recovery)
    if (billableSeconds > 0 && (!s.lastBilledMinute || s.lastBilledMinute < 1) && !isInsufficient && !isRestartRecovery) {
        const res = await processBillingCharge(sessionId, billableSeconds, 1, 'early_exit');
        if (res?.success) {
            s.lastBilledMinute = 1;
            if (!activeSessions.has(sessionId)) {
                s.totalDeducted = (s.totalDeducted || 0) + (res.charged || 0);
                s.totalEarned = (s.totalEarned || 0) + (res.astroShare || 0);
            }
        }
    } else if (billableSeconds > 60 && !isInsufficient && !isRestartRecovery) {
        // Ensure any subsequent entered minute that wasn't billed by ticker gets billed
        const lastBilled = s.lastBilledMinute || 1;
        const totalMinutesDue = Math.ceil(billableSeconds / 60);
        if (totalMinutesDue > lastBilled) {
            for (let m = lastBilled + 1; m <= totalMinutesDue; m++) {
                const isFraction = (m === totalMinutesDue && (billableSeconds % 60) !== 0);
                const chargeType = isFraction ? 'fraction' : 'slab';
                const res = await processBillingCharge(sessionId, 60, m, chargeType);
                if (res?.success) {
                    s.lastBilledMinute = m;
                    if (!activeSessions.has(sessionId)) {
                        s.totalDeducted = (s.totalDeducted || 0) + (res.charged || 0);
                        s.totalEarned = (s.totalEarned || 0) + (res.astroShare || 0);
                    }
                } else {
                    break;
                }
            }
        }
    }

    // Now update Session in DB with accurate final deducted and earned totals
    await Session.updateOne({ sessionId }, {
        endTime,
        duration: billableSeconds * 1000,
        totalEarned: s.totalEarned || 0,
        totalCharged: s.totalDeducted || 0,
        status: 'ended'
    });

    if (s.clientId && billableSeconds > 0) {
        await User.updateOne({ userId: s.clientId }, { isNewUser: false });
    }

    if (s.pairMonthId) {
        await PairMonth.updateOne(
            { _id: s.pairMonthId },
            { $inc: { slabLockedAt: billableSeconds } }
        );
    }

    activeSessions.delete(sessionId);
    if (s.users) {
        s.users.forEach((u) => {
            if (userActiveSession.get(u) === sessionId) {
                userActiveSession.delete(u);
            }
            if (sessionDisconnectTimeouts.has(u)) {
                clearTimeout(sessionDisconnectTimeouts.get(u));
                sessionDisconnectTimeouts.delete(u);
            }
        });
    }

    const finalReason = s.endReason || endReason || 'ended';
    let clientWalletBal = null;
    if (s.clientId) {
        try {
            const clientDoc = await User.findOne({ userId: s.clientId }).select('walletBalance');
            if (clientDoc) {
                clientWalletBal = Number(clientDoc.walletBalance || 0);
            }
        } catch (e) {
            console.error('[BillingService] Failed to fetch final client wallet balance:', e);
        }
    }

    const payload = {
        sessionId,
        reason: finalReason,
        summary: {
            deducted: s.totalDeducted || 0,
            earned: s.totalEarned || 0,
            duration: billableSeconds,
            remainingBalance: clientWalletBal !== null ? clientWalletBal : undefined,
            reason: finalReason
        }
    };

    if (io) {
        if (s.clientId) {
            io.to(s.clientId).emit('session-ended', payload);
            if (clientWalletBal !== null) {
                io.to(s.clientId).emit('wallet-update', { balance: clientWalletBal });
            }
        }
        if (s.astrologerId) io.to(s.astrologerId).emit('session-ended', payload);
        if (s.users && Array.isArray(s.users)) {
            s.users.forEach(u => io.to(u).emit('session-ended', payload));
        }
    }

    if (s.astrologerId) {
        require('./presence.service').setBusy(s.astrologerId, false, io);
    } else {
        User.updateMany({ userId: { $in: s.users }, role: 'astrologer' }, { isBusy: false })
            .then(() => { if (broadcastAstroUpdate) broadcastAstroUpdate(); })
            .catch(e => console.error('Error clearing busy:', e));
    }
}

module.exports = {
    getRateForSession,
    processBillingCharge,
    endSessionRecord,
    setIo,
    syncWalletBalance,
    setSlabRates: (rates) => {
        if (rates && typeof rates === 'object') {
            SLAB_RATES = rates;
            console.log('[BillingService] Updated active SLAB_RATES:', SLAB_RATES);
        }
    }
};

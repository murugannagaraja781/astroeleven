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

async function processBillingCharge(sessionId, durationSeconds, minuteIndex, type) {
    try {
        const session = await Session.findOne({ sessionId });
        if (!session) return { success: false, reason: 'session_not_found' };

        const astro = await User.findOne({ userId: session.astrologerId });
        if (!astro) return { success: false, reason: 'astro_not_found' };

        const client = await User.findOne({ userId: session.clientId });
        if (!client) return { success: false, reason: 'client_not_found' };

        let pricePerMin = 10;
        if (astro.price && astro.price > 0) {
            pricePerMin = parseInt(astro.price);
        } else {
            if (session.type === 'audio') pricePerMin = 15;
            else if (session.type === 'video') pricePerMin = 20;
        }

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
            const newClientBalance = Math.round((actualBalance - totalToDeduct) * 100) / 100;
            await User.updateOne({ userId: client.userId }, { walletBalance: newClientBalance });
            client.walletBalance = newClientBalance;

            let newAstroBalance = Number(astro.walletBalance || 0);
            let newAstroEarnings = Number(astro.totalEarnings || 0);
            if (astroShare > 0) {
                newAstroBalance = Math.round((newAstroBalance + astroShare) * 100) / 100;
                newAstroEarnings = Math.round((newAstroEarnings + astroShare) * 100) / 100;
                await User.updateOne({ userId: astro.userId }, {
                    walletBalance: newAstroBalance,
                    totalEarnings: newAstroEarnings
                });
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

async function endSessionRecord(sessionId, broadcastAstroUpdate) {
    let s = activeSessions.get(sessionId);
    // If session is not in memory (e.g. multi-process or server restarted), load from DB
    if (!s) {
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
            lastBilledMinute: Math.ceil((dbSess.totalCharged || 0) / (dbSess.price || 10))
        };
    }

    const endTime = Date.now();
    let billableSeconds = s.elapsedBillableSeconds || 0;
    // Accurate wall-clock duration calculation
    if (s.actualBillingStart && s.actualBillingStart > 0) {
        const wallClockSeconds = Math.max(0, Math.floor((endTime - s.actualBillingStart) / 1000));
        billableSeconds = Math.max(billableSeconds, wallClockSeconds);
    }
    s.elapsedBillableSeconds = billableSeconds;

    console.log(`[Billing][endSessionRecord] sessionId=${sessionId}, billableSeconds=${billableSeconds}, totalDeducted=${s.totalDeducted || 0}, totalEarned=${s.totalEarned || 0}`);

    // If session was connected/active but never billed minute 1, charge minute 1 minimum
    if (billableSeconds > 0 && (!s.lastBilledMinute || s.lastBilledMinute < 1)) {
        const res = await processBillingCharge(sessionId, billableSeconds, 1, 'early_exit');
        s.lastBilledMinute = 1;
        if (!activeSessions.has(sessionId) && res?.success) {
            s.totalDeducted = (s.totalDeducted || 0) + (res.charged || 0);
            s.totalEarned = (s.totalEarned || 0) + (res.astroShare || 0);
        }
    } else if (billableSeconds > 60) {
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

    const payload = {
        reason: 'ended',
        summary: {
            deducted: s.totalDeducted || 0,
            earned: s.totalEarned || 0,
            duration: billableSeconds
        }
    };

    if (io) {
        if (s.clientId) io.to(s.clientId).emit('session-ended', payload);
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

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

const API_BRIDGE_URL = 'https://astroeleven.com/api_bridge.php';
const API_BRIDGE_SECRET = 'Astro ElevenNodeBridge2026';

async function syncWalletBalance(userId) {
    try {
        const user = await User.findOne({ userId });
        if (!user) return null;

        // Always return MongoDB balance for test users
        if (user.phone === '9000000000' || user.phone === '919000000000' || user.phone === '+919000000000') {
            console.log(`[Billing] syncWalletBalance: Using local balance for test user ${user.phone}: ${user.walletBalance}`);
            return user.walletBalance;
        }

        // Try API Bridge for real users
        try {
            const response = await fetch(API_BRIDGE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'x-bridge-secret': API_BRIDGE_SECRET },
                body: JSON.stringify({ action: 'balance', userId })
            });
            const data = await response.json();

            if (response.ok && data.success) {
                const bridgeBalance = parseFloat(data.balance) || 0;
                // SAFETY CHECK: Don't trust bridge balance of 0 if MongoDB has a higher value
                // This prevents wiping wallet when bridge returns 0 for unknown users
                if (bridgeBalance === 0 && user.walletBalance > 0) {
                    console.warn(`[Billing] syncWalletBalance: Bridge returned 0 but MongoDB has ${user.walletBalance} for ${userId}. Using MongoDB balance.`);
                    return user.walletBalance;
                }
                // Update MongoDB only if bridge returned a positive or matching value
                if (bridgeBalance > 0) {
                    await User.updateOne({ userId }, { walletBalance: bridgeBalance });
                    return bridgeBalance;
                }
            }
        } catch (bridgeErr) {
            console.error('[Billing] syncWalletBalance bridge error:', bridgeErr.message);
        }

        // Fallback: return MongoDB balance (do NOT update to 0)
        console.log(`[Billing] syncWalletBalance: Using MongoDB fallback balance ${user.walletBalance} for ${userId}`);
        return user.walletBalance;
    } catch (e) {
        console.error('[Billing] syncWalletBalance error:', e.message);
    }
    return null;
}

async function processBillingCharge(sessionId, durationSeconds, minuteIndex, type) {
    try {
        const session = await Session.findOne({ sessionId });
        if (!session) return;

        const astro = await User.findOne({ userId: session.astrologerId });
        if (!astro) return;

        const client = await User.findOne({ userId: session.clientId });
        if (!client) return;

        let pricePerMin = 10;
        if (astro.price && astro.price > 0) {
            pricePerMin = parseInt(astro.price);
        } else {
            if (session.type === 'audio') pricePerMin = 15;
            if (session.type === 'video') pricePerMin = 20;
        }

        console.log(`[Billing] Session ${sessionId} | Type: ${session.type} | Price: ${pricePerMin}/min | Minute: ${minuteIndex}`);

        let amountToCharge = 0;
        let astroShare = 0;
        let adminShare = 0;
        let reason = '';

        if (type === 'first_60_full') {
            amountToCharge = pricePerMin;
            adminShare = amountToCharge;
            astroShare = 0;
            reason = 'first_60';
        } else if (type === 'early_exit') {
            amountToCharge = pricePerMin;
            adminShare = amountToCharge;
            astroShare = 0;
            reason = 'first_60_min_charge';
        } else if (type === 'slab') {
            const activeSess = activeSessions.get(sessionId);
            const currentSlab = activeSess?.currentSlab || 3;
            const rate = SLAB_RATES[currentSlab] || 0.30;

            amountToCharge = pricePerMin;
            astroShare = amountToCharge * rate;
            adminShare = amountToCharge - astroShare;
            reason = `slab_${currentSlab}`;
        } else if (type === 'fraction') {
            amountToCharge = pricePerMin;
            adminShare = amountToCharge;
            astroShare = 0;
            reason = 'fraction_roundup';
        } else {
            return;
        }

        const totalToDeduct = amountToCharge;
        
        // Re-fetch client from MySQL to get the most up-to-date local balance
        const freshClient = await User.findOne({ userId: client.userId });
        const actualBalance = freshClient ? freshClient.walletBalance : client.walletBalance;
        console.log(`[Billing] Using local database balance: ${actualBalance} for user ${client.userId} (Required: ${totalToDeduct})`);

        if (actualBalance >= totalToDeduct) {
            let mainDeduct = totalToDeduct;

            // Deduct locally first so the change is instantly reflected on the app and next tick
            client.walletBalance = actualBalance - mainDeduct;
            await client.save();
            console.log(`[Billing] Deducted ${mainDeduct} locally from ${client.userId}. New balance: ${client.walletBalance}`);

            // Propagate balance deduction to the PHP WordPress MySQL database asynchronously
            const isTestUser = client && (client.phone === '9000000000' || client.phone === '919000000000' || client.phone === '+919000000000');
            if (!isTestUser) {
                fetch(API_BRIDGE_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'x-bridge-secret': API_BRIDGE_SECRET },
                    body: JSON.stringify({
                        action: 'deduct',
                        userId: client.userId,
                        amount: mainDeduct,
                        astrologerId: astro.userId,
                        orderId: sessionId
                    })
                }).then(async response => {
                    const data = await response.json();
                    if (response.ok && data.success) {
                        // Sync client balance to match any updated adjustments from API bridge response
                        if (data.newBalance !== undefined) {
                            await User.updateOne({ userId: client.userId }, { walletBalance: data.newBalance });
                        }
                    } else {
                        console.error('[Billing] API Bridge Sync Deduction Error:', data);
                    }
                }).catch(err => {
                    console.error('[Billing] API Bridge Sync Network Error:', err.message);
                });
            }

            // Mark deduction as completed successfully
            const deductionSuccess = true;
            if (deductionSuccess) {

                if (astroShare > 0) {
                    astro.walletBalance += astroShare;
                    astro.totalEarnings = (astro.totalEarnings || 0) + astroShare;
                    await astro.save();
                }

                // --- Record in BillingLedger ---
                await BillingLedger.create({
                    billingId: crypto.randomUUID(),
                    sessionId,
                    minuteIndex,
                    chargedToClient: amountToCharge,
                    creditedToAstrologer: astroShare,
                    adminAmount: adminShare,
                    reason
                }).catch(e => console.error('[Billing] Ledger Creation Error:', e.message));

                // Update running totals in activeSession map
                const s = activeSessions.get(sessionId);
                if (s) {
                    s.totalDeducted = (s.totalDeducted || 0) + totalToDeduct;
                    s.totalEarned = (s.totalEarned || 0) + astroShare;
                }

                // Notify Wallets
                if (io) {
                    io.to(client.userId).emit('wallet-update', { balance: client.walletBalance });
                    io.to(astro.userId).emit('wallet-update', { balance: astro.walletBalance });
                }
            }
        } else {
            console.warn(`[Billing] Insufficient balance for ${client.userId}. Required: ${totalToDeduct}, Has: ${actualBalance}`);
        }
    } catch (e) {
        console.error('processBillingCharge error:', e);
    }
}

async function endSessionRecord(sessionId, broadcastAstroUpdate) {
    const s = activeSessions.get(sessionId);
    if (!s) return;

    const endTime = Date.now();
    const billableSeconds = s.elapsedBillableSeconds || 0;
    console.log(`[Billing][endSessionRecord] sessionId=${sessionId}, billableSeconds=${billableSeconds}`);

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

    if (billableSeconds > 0 && billableSeconds <= 60) {
        await processBillingCharge(sessionId, billableSeconds, 1, 'early_exit');
    } else if (billableSeconds > 60) {
        const lastBilled = s.lastBilledMinute || 1;
        const totalMinutes = Math.ceil(billableSeconds / 60);

        if (totalMinutes > lastBilled) {
            for (let i = lastBilled + 1; i <= totalMinutes; i++) {
                const isFraction = (i === totalMinutes && (billableSeconds % 60) !== 0);
                const billingType = isFraction ? 'fraction' : 'slab';
                await processBillingCharge(sessionId, 60, i, billingType);
            }
        }
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
    setSlabRates: (rates) => { SLAB_RATES = rates; }
};

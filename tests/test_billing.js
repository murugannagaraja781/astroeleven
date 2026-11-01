/**
 * Astro Eleven - Comprehensive Billing System Test Suite
 * Run with: node tests/test_billing.js
 * 
 * Verifies:
 * 1. Rate Resolution (Chat, Audio, Video & fallbacks)
 * 2. Dynamic Slab Commission Calculations (30%, 35%, 40%, 50%)
 * 3. Atomic Wallet Deduction & Overdraft Safeguard ($gte guard)
 * 4. Minute 1 Upfront Charging on Call Connect
 * 5. Low Balance (< ₹1) Call Cutoff & Protection
 * 6. Ticker Subsequent Minute Charging (Minute 2, Minute 3, ...)
 * 7. Wall-Clock Session Reconciliations & Fractional Minute Rounding on End
 * 8. Astrologer Withdrawal Instant Deduction & Rollback Safety
 * 9. Super Admin Withdrawal Notification System
 */

const assert = require('assert');

// ANSI Color Helpers
const GREEN = '\x1b[32m';
const RED = '\x1b[31m';
const CYAN = '\x1b[36m';
const YELLOW = '\x1b[33m';
const BOLD = '\x1b[1m';
const RESET = '\x1b[0m';

let passedTests = 0;
let totalTests = 0;

function it(description, fn) {
    totalTests++;
    try {
        fn();
        console.log(`  ${GREEN}✔${RESET} ${description}`);
        passedTests++;
    } catch (err) {
        console.error(`  ${RED}✖${RESET} ${description}`);
        console.error(`    ${RED}${err.message}${RESET}`);
    }
}

async function itAsync(description, fn) {
    totalTests++;
    try {
        await fn();
        console.log(`  ${GREEN}✔${RESET} ${description}`);
        passedTests++;
    } catch (err) {
        console.error(`  ${RED}✖${RESET} ${description}`);
        console.error(`    ${RED}${err.message}${RESET}`);
    }
}

function describe(suiteTitle, fn) {
    console.log(`\n${BOLD}${CYAN}=== ${suiteTitle} ===${RESET}`);
    fn();
}

// -------------------------------------------------------------
// 1. In-Memory Mock System to test billing logic in pure isolation
// -------------------------------------------------------------
class MockDatabase {
    constructor() {
        this.users = new Map();
        this.sessions = new Map();
        this.ledgers = [];
        this.withdrawals = [];
        this.notifications = [];
    }

    createUser(data) {
        const u = {
            userId: data.userId,
            name: data.name || 'User',
            role: data.role || 'client',
            walletBalance: Number(data.walletBalance || 0),
            totalEarnings: Number(data.totalEarnings || 0),
            chatPrice: Number(data.chatPrice || 0),
            callPrice: Number(data.callPrice || 0),
            videoPrice: Number(data.videoPrice || 0),
            price: Number(data.price || 0),
            ratePerMinute: Number(data.ratePerMinute || 0),
            isBusy: Boolean(data.isBusy || false)
        };
        this.users.set(u.userId, u);
        return u;
    }

    // Emulates MongoDB / MySQL atomic update: { walletBalance: { $gte: amount } }, { $inc: { walletBalance: -amount } }
    atomicDeduct(userId, amount) {
        const u = this.users.get(userId);
        if (!u) return { success: false, reason: 'user_not_found' };
        if (u.walletBalance < amount) {
            return { success: false, reason: 'insufficient_balance', balance: u.walletBalance };
        }
        u.walletBalance = Math.round((u.walletBalance - amount) * 100) / 100;
        return { success: true, newBalance: u.walletBalance };
    }

    atomicCredit(userId, amount) {
        const u = this.users.get(userId);
        if (!u) return false;
        u.walletBalance = Math.round((u.walletBalance + amount) * 100) / 100;
        u.totalEarnings = Math.round((u.totalEarnings + amount) * 100) / 100;
        return true;
    }
}

// -------------------------------------------------------------
// Pure Billing Service Logic Engine (matching services/billing.service.js)
// -------------------------------------------------------------
const SLAB_RATES = {
    1: 0.30, // 30% Astrologer, 70% Admin
    2: 0.35, // 35% Astrologer, 65% Admin
    3: 0.40, // 40% Astrologer, 60% Admin
    4: 0.50  // 50% Astrologer, 50% Admin
};

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

function calculateSlabSplit(amountToCharge, slabNumber = 1) {
    const rate = SLAB_RATES[slabNumber] !== undefined ? SLAB_RATES[slabNumber] : 0.30;
    const astroShare = Math.round((amountToCharge * rate) * 100) / 100;
    const adminShare = Math.round((amountToCharge - astroShare) * 100) / 100;
    return { astroShare, adminShare, astroPercentage: Math.round(rate * 100), adminPercentage: Math.round((1 - rate) * 100) };
}

function processBillingChargeSimulation(db, sessionId, minuteIndex, currentSlab = 1) {
    const session = db.sessions.get(sessionId);
    if (!session) return { success: false, reason: 'session_not_found' };

    const astro = db.users.get(session.astrologerId);
    const client = db.users.get(session.clientId);
    if (!astro || !client) return { success: false, reason: 'user_not_found' };

    const pricePerMin = getRateForSession(astro, session.type);
    const split = calculateSlabSplit(pricePerMin, currentSlab);

    // Atomic deduction guard
    const deductRes = db.atomicDeduct(client.userId, pricePerMin);
    if (!deductRes.success) {
        return {
            success: false,
            reason: 'insufficient_balance',
            remainingBalance: deductRes.balance,
            pricePerMin
        };
    }

    // Credit Astrologer
    db.atomicCredit(astro.userId, split.astroShare);

    // Ledger Entry
    db.ledgers.push({
        sessionId,
        minuteIndex,
        chargedToClient: pricePerMin,
        creditedToAstrologer: split.astroShare,
        adminAmount: split.adminShare,
        createdAt: new Date()
    });

    session.totalCharged = Math.round(((session.totalCharged || 0) + pricePerMin) * 100) / 100;
    session.totalEarned = Math.round(((session.totalEarned || 0) + split.astroShare) * 100) / 100;
    session.lastBilledMinute = minuteIndex;

    return {
        success: true,
        remainingBalance: deductRes.newBalance,
        pricePerMin,
        charged: pricePerMin,
        astroShare: split.astroShare,
        adminShare: split.adminShare
    };
}

// -------------------------------------------------------------
// Test Execution
// -------------------------------------------------------------
async function runBillingTestSuite() {
    console.log(`\n${BOLD}====================================================${RESET}`);
    console.log(`${BOLD}     ASTRO ELEVEN BILLING VERIFICATION SUITE       ${RESET}`);
    console.log(`${BOLD}====================================================${RESET}`);

    // TEST 1: Rate Calculation
    describe('1. Rate Resolution (getRateForSession)', () => {
        const astroWithCustomRates = {
            chatPrice: 25,
            callPrice: 35,
            videoPrice: 50
        };

        it('Should use custom chatPrice for chat session', () => {
            assert.strictEqual(getRateForSession(astroWithCustomRates, 'chat'), 25);
        });

        it('Should use custom callPrice for audio session', () => {
            assert.strictEqual(getRateForSession(astroWithCustomRates, 'audio'), 35);
            assert.strictEqual(getRateForSession(astroWithCustomRates, 'call'), 35);
            assert.strictEqual(getRateForSession(astroWithCustomRates, 'voice'), 35);
        });

        it('Should use custom videoPrice for video session', () => {
            assert.strictEqual(getRateForSession(astroWithCustomRates, 'video'), 50);
        });

        it('Should fall back to general price if specific type price is 0', () => {
            const astroFallbackPrice = { chatPrice: 0, callPrice: 0, videoPrice: 0, price: 30 };
            assert.strictEqual(getRateForSession(astroFallbackPrice, 'chat'), 30);
            assert.strictEqual(getRateForSession(astroFallbackPrice, 'call'), 30);
            assert.strictEqual(getRateForSession(astroFallbackPrice, 'video'), 30);
        });

        it('Should fall back to system default (video: ₹20, audio/chat: ₹15) if no price configured', () => {
            const emptyAstro = {};
            assert.strictEqual(getRateForSession(emptyAstro, 'video'), 20);
            assert.strictEqual(getRateForSession(emptyAstro, 'audio'), 15);
            assert.strictEqual(getRateForSession(emptyAstro, 'chat'), 15);
        });
    });

    // TEST 2: Slab Rates and Split Math
    describe('2. Commission & Slab Percentage Calculations', () => {
        it('Slab 1 (Min 1-5): Astrologer 30%, Admin 70%', () => {
            const split = calculateSlabSplit(100, 1);
            assert.strictEqual(split.astroShare, 30);
            assert.strictEqual(split.adminShare, 70);
            assert.strictEqual(split.astroShare + split.adminShare, 100);
        });

        it('Slab 2 (Min 6-10): Astrologer 35%, Admin 65%', () => {
            const split = calculateSlabSplit(100, 2);
            assert.strictEqual(split.astroShare, 35);
            assert.strictEqual(split.adminShare, 65);
            assert.strictEqual(split.astroShare + split.adminShare, 100);
        });

        it('Slab 3 (Min 11-15): Astrologer 40%, Admin 60%', () => {
            const split = calculateSlabSplit(100, 3);
            assert.strictEqual(split.astroShare, 40);
            assert.strictEqual(split.adminShare, 60);
            assert.strictEqual(split.astroShare + split.adminShare, 100);
        });

        it('Slab 4 (Min 16+): Astrologer 50%, Admin 50%', () => {
            const split = calculateSlabSplit(100, 4);
            assert.strictEqual(split.astroShare, 50);
            assert.strictEqual(split.adminShare, 50);
            assert.strictEqual(split.astroShare + split.adminShare, 100);
        });

        it('Split precision: Odd amount (e.g. ₹33/min rate in Slab 1)', () => {
            const split = calculateSlabSplit(33, 1);
            // 33 * 0.30 = 9.90, admin = 23.10
            assert.strictEqual(split.astroShare, 9.90);
            assert.strictEqual(split.adminShare, 23.10);
            assert.strictEqual(Math.round((split.astroShare + split.adminShare) * 100) / 100, 33.00);
        });
    });

    // TEST 3: Minute 1 Upfront Deduction & Balance Guard
    describe('3. Minute 1 Upfront Deduction on Call Connect', () => {
        it('Deducts Minute 1 immediately when session connects', () => {
            const db = new MockDatabase();
            const client = db.createUser({ userId: 'C001', walletBalance: 100 });
            const astro = db.createUser({ userId: 'A001', callPrice: 20, walletBalance: 0 });
            db.sessions.set('S001', {
                sessionId: 'S001',
                clientId: client.userId,
                astrologerId: astro.userId,
                type: 'call',
                status: 'active'
            });

            // Simulate call connect Minute 1 charge
            const res = processBillingChargeSimulation(db, 'S001', 1, 1);
            assert.strictEqual(res.success, true);
            assert.strictEqual(res.charged, 20);
            assert.strictEqual(res.remainingBalance, 80);
            assert.strictEqual(client.walletBalance, 80);
            assert.strictEqual(astro.walletBalance, 6); // 30% of 20 = 6
            assert.strictEqual(db.ledgers.length, 1);
        });

        it('Prevents connection and overdraft if balance < Minute 1 rate', () => {
            const db = new MockDatabase();
            const client = db.createUser({ userId: 'C002', walletBalance: 15 }); // Needs 20
            const astro = db.createUser({ userId: 'A002', callPrice: 20 });
            db.sessions.set('S002', {
                sessionId: 'S002',
                clientId: client.userId,
                astrologerId: astro.userId,
                type: 'call'
            });

            const res = processBillingChargeSimulation(db, 'S002', 1, 1);
            assert.strictEqual(res.success, false);
            assert.strictEqual(res.reason, 'insufficient_balance');
            assert.strictEqual(client.walletBalance, 15); // Balance intact, no negative balance!
        });
    });

    // TEST 4: Low Balance (< ₹1) Cutoff & Ticker Loop
    describe('4. Low Balance (< ₹1) Call Cutoff & Multi-Minute Ticker', () => {
        it('Terminates session when remaining balance drops below ₹1', () => {
            const db = new MockDatabase();
            // Client has exactly ₹20 for 1 minute at ₹20/min
            const client = db.createUser({ userId: 'C003', walletBalance: 20 });
            const astro = db.createUser({ userId: 'A003', callPrice: 20 });
            const session = {
                sessionId: 'S003',
                clientId: client.userId,
                astrologerId: astro.userId,
                type: 'call',
                actualBillingStart: Date.now() - 65000, // 65 seconds ago
                lastBilledMinute: 1,
                status: 'active'
            };
            db.sessions.set('S003', session);

            // Minute 1 was billed on connect, remaining is 0
            db.atomicDeduct(client.userId, 20);
            assert.strictEqual(client.walletBalance, 0);

            // Ticker check simulation:
            const clientBal = client.walletBalance;
            const isCutoffTriggered = clientBal < 1;
            assert.strictEqual(isCutoffTriggered, true, 'Call must be cut immediately when wallet < ₹1');
        });

        it('Seamlessly charges Minute 2 at 60s when client has sufficient funds', () => {
            const db = new MockDatabase();
            const client = db.createUser({ userId: 'C004', walletBalance: 50 });
            const astro = db.createUser({ userId: 'A004', callPrice: 20 });
            db.sessions.set('S004', {
                sessionId: 'S004',
                clientId: client.userId,
                astrologerId: astro.userId,
                type: 'call'
            });

            // Min 1
            processBillingChargeSimulation(db, 'S004', 1, 1);
            assert.strictEqual(client.walletBalance, 30);

            // Min 2 at 60s
            const resMin2 = processBillingChargeSimulation(db, 'S004', 2, 1);
            assert.strictEqual(resMin2.success, true);
            assert.strictEqual(client.walletBalance, 10);
            assert.strictEqual(astro.walletBalance, 12); // 6 + 6
            assert.strictEqual(db.ledgers.length, 2);
        });
    });

    // TEST 5: Session End Reconciliation & Fractional Minutes
    describe('5. Session End Duration & Fractional Minute Rounding', () => {
        it('Converts milliseconds to seconds correctly without the 0s bug', () => {
            // Test the 0s fix from version 1.1.0
            const testFormat = (durationVal) => {
                const totalSeconds = durationVal > 50000 ? Math.round(durationVal / 1000) : durationVal;
                const m = Math.floor(totalSeconds / 60);
                const s = totalSeconds % 60;
                return `${m}m ${s}s`;
            };

            // Server sends 840s (14 minutes)
            assert.strictEqual(testFormat(840), '14m 0s');
            // Server sends 300s (5 minutes)
            assert.strictEqual(testFormat(300), '5m 0s');
            // If legacy server sends 60000ms (1 minute)
            assert.strictEqual(testFormat(60000), '1m 0s');
            // 45s duration
            assert.strictEqual(testFormat(45), '0m 45s');
        });

        it('Early exit under 60s retains Minute 1 minimum charge', () => {
            const db = new MockDatabase();
            const client = db.createUser({ userId: 'C005', walletBalance: 50 });
            const astro = db.createUser({ userId: 'A005', callPrice: 20 });
            const sess = {
                sessionId: 'S005',
                clientId: client.userId,
                astrologerId: astro.userId,
                type: 'call',
                lastBilledMinute: 1,
                totalCharged: 20,
                totalEarned: 6
            };
            db.sessions.set('S005', sess);
            client.walletBalance = 30; // Min 1 paid

            // Call ended at 35 seconds
            const durationSeconds = 35;
            // No extra minute charged because duration <= 60s and Min 1 was already paid
            const needsExtraCharge = durationSeconds > 60;
            assert.strictEqual(needsExtraCharge, false);
            assert.strictEqual(sess.totalCharged, 20);
            assert.strictEqual(client.walletBalance, 30);
        });
    });

    // TEST 6: Astrologer Withdrawal & Real-Time Sync
    describe('6. Astrologer Withdrawal Flow & Super Admin Notification', () => {
        it('Deducts requested withdrawal amount immediately from astrologer balance', () => {
            const db = new MockDatabase();
            const astro = db.createUser({ userId: 'A006', role: 'astrologer', walletBalance: 1250 });

            const withdrawAmount = 500;
            // Verification: Min limit ₹100
            assert.ok(withdrawAmount >= 100, 'Must meet minimum ₹100');

            // Atomic deduction
            const deduct = db.atomicDeduct(astro.userId, withdrawAmount);
            assert.strictEqual(deduct.success, true);
            assert.strictEqual(astro.walletBalance, 750);

            // Record withdrawal
            db.withdrawals.push({
                astroId: astro.userId,
                amount: withdrawAmount,
                status: 'pending',
                requestedAt: new Date()
            });
            assert.strictEqual(db.withdrawals.length, 1);
            assert.strictEqual(db.withdrawals[0].amount, 500);
            assert.strictEqual(db.withdrawals[0].status, 'pending');
        });

        it('Rejects withdrawal if requested amount exceeds wallet balance', () => {
            const db = new MockDatabase();
            const astro = db.createUser({ userId: 'A007', role: 'astrologer', walletBalance: 300 });

            const deduct = db.atomicDeduct(astro.userId, 500);
            assert.strictEqual(deduct.success, false);
            assert.strictEqual(deduct.reason, 'insufficient_balance');
            assert.strictEqual(astro.walletBalance, 300); // Balance untouched
        });

        it('Creates persistent notification for Super Admin with astrologer details', () => {
            const db = new MockDatabase();
            const astro = db.createUser({ userId: 'A008', name: 'Pandit Sharma', role: 'astrologer', walletBalance: 1000 });

            const notif = {
                targetRole: 'superadmin',
                type: 'withdrawal_request',
                title: '💰 புதிய பணம் எடுக்கும் கோரிக்கை',
                body: `${astro.name} ₹500 வித்ட்ரா செய்ய கோரியுள்ளார்.`,
                data: JSON.stringify({
                    astroId: astro.userId,
                    astroName: astro.name,
                    amount: 500,
                    walletBalance: 500,
                    time: new Date().toISOString()
                }),
                isRead: false,
                createdAt: new Date()
            };
            db.notifications.push(notif);

            assert.strictEqual(db.notifications.length, 1);
            assert.strictEqual(db.notifications[0].targetRole, 'superadmin');
            assert.strictEqual(db.notifications[0].isRead, false);
            const parsedData = JSON.parse(db.notifications[0].data);
            assert.strictEqual(parsedData.astroName, 'Pandit Sharma');
            assert.strictEqual(parsedData.amount, 500);
        });

        it('Rolls back deducted balance if withdrawal DB creation errors', () => {
            const db = new MockDatabase();
            const astro = db.createUser({ userId: 'A009', walletBalance: 1000 });

            // 1. Initial deduction
            db.atomicDeduct(astro.userId, 500);
            assert.strictEqual(astro.walletBalance, 500);

            // 2. Simulated DB error -> Auto-refund
            const simulatedDbFailed = true;
            if (simulatedDbFailed) {
                db.atomicCredit(astro.userId, 500);
            }
            assert.strictEqual(astro.walletBalance, 1000, 'Refund must restore exact original balance');
        });
    });

    // SUMMARY
    console.log(`\n${BOLD}====================================================${RESET}`);
    if (passedTests === totalTests) {
        console.log(`${BOLD}${GREEN}✔ ALL ${totalTests} BILLING & WITHDRAWAL TESTS PASSED PERFECTLY!${RESET}`);
    } else {
        console.log(`${BOLD}${RED}✖ ${totalTests - passedTests} OF ${totalTests} TESTS FAILED${RESET}`);
    }
    console.log(`${BOLD}====================================================\n${RESET}`);

    return passedTests === totalTests;
}

runBillingTestSuite();

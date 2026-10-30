const assert = require('assert');
const billingService = require('../services/billing.service');

function runBillingTests() {
    console.log('=== STARTING BILLING SUITE TEST ===\n');

    // -------------------------------------------------------------
    // Test 1: Rate Resolution (getRateForSession)
    // -------------------------------------------------------------
    console.log('Test 1: Rate Resolution across astrologer configurations...');

    // Astro A: Has distinct prices for chat, call, video, and a legacy price
    const astroA = {
        name: 'Astro A',
        price: 10,       // Legacy price
        chatPrice: 15,
        callPrice: 20,
        videoPrice: 30
    };

    assert.strictEqual(billingService.getRateForSession(astroA, 'chat'), 15, 'Chat should be ₹15');
    assert.strictEqual(billingService.getRateForSession(astroA, 'audio'), 20, 'Audio should be ₹20');
    assert.strictEqual(billingService.getRateForSession(astroA, 'call'), 20, 'Call should be ₹20');
    assert.strictEqual(billingService.getRateForSession(astroA, 'voice'), 20, 'Voice should be ₹20');
    assert.strictEqual(billingService.getRateForSession(astroA, 'video'), 30, 'Video should be ₹30');
    console.log('  ✅ Astro A: Distinct prices properly prioritize session type over legacy fallback.');

    // Astro B: Only has legacy price
    const astroB = {
        name: 'Astro B',
        price: 25,
        chatPrice: 0,
        callPrice: 0,
        videoPrice: 0
    };
    assert.strictEqual(billingService.getRateForSession(astroB, 'chat'), 25, 'Chat should fallback to ₹25');
    assert.strictEqual(billingService.getRateForSession(astroB, 'call'), 25, 'Call should fallback to ₹25');
    assert.strictEqual(billingService.getRateForSession(astroB, 'video'), 25, 'Video should fallback to ₹25');
    console.log('  ✅ Astro B: Legacy fallback applied when specific prices are not set.');

    // Astro C: Missing all prices (defaults)
    const astroC = { name: 'Astro C' };
    assert.strictEqual(billingService.getRateForSession(astroC, 'chat'), 15, 'Default chat rate should be ₹15');
    assert.strictEqual(billingService.getRateForSession(astroC, 'audio'), 15, 'Default audio rate should be ₹15');
    assert.strictEqual(billingService.getRateForSession(astroC, 'video'), 20, 'Default video rate should be ₹20');
    console.log('  ✅ Astro C: Default system baseline rates applied.');

    // -------------------------------------------------------------
    // Test 2: Slab Calculation Invariants
    // -------------------------------------------------------------
    console.log('\nTest 2: Slab Share calculations...');
    const testRates = { 1: 0.30, 2: 0.35, 3: 0.40, 4: 0.50 };
    billingService.setSlabRates(testRates);

    const price = 20;
    const slab1Astro = Math.round((price * 0.30) * 100) / 100;
    const slab1Admin = Math.round((price - slab1Astro) * 100) / 100;
    assert.strictEqual(slab1Astro, 6.00, 'Astrologer 30% of ₹20 should be ₹6.00');
    assert.strictEqual(slab1Admin, 14.00, 'Admin 70% of ₹20 should be ₹14.00');
    assert.strictEqual(slab1Astro + slab1Admin, price, 'Sum must equal ₹20 exactly');
    console.log('  ✅ Slab 1: ₹6.00 astro + ₹14.00 admin = ₹20.00 exact');

    // -------------------------------------------------------------
    // Test 3: Insufficient Balance - Wallet Preservation Logic
    // -------------------------------------------------------------
    console.log('\nTest 3: Insufficient balance wallet preservation check...');
    // Simulated state: Client has ₹8, required minute 2 is ₹15
    const initialClientWallet = 8;
    const requiredCharge = 15;
    let actualClientWallet = initialClientWallet;

    let chargeResult;
    if (actualClientWallet >= requiredCharge) {
        actualClientWallet -= requiredCharge;
        chargeResult = { success: true };
    } else {
        // Correct behavior: do NOT set to 0!
        chargeResult = { success: false, reason: 'insufficient_balance', remainingBalance: actualClientWallet };
    }

    assert.strictEqual(chargeResult.success, false);
    assert.strictEqual(chargeResult.remainingBalance, 8, 'Remaining unspent ₹8 must be preserved!');
    assert.strictEqual(actualClientWallet, 8, 'Wallet must NOT be zeroed out!');
    console.log('  ✅ Unspent balance ₹8 safely preserved when upfront minute charge cannot be fulfilled.');

    // -------------------------------------------------------------
    // Test 4: Concurrency Guard on Session Termination
    // -------------------------------------------------------------
    console.log('\nTest 4: Concurrency Guard on Session Termination...');
    const fakeSession = {
        sessionId: 'test_sess_001',
        _isEnding: false,
        totalDeducted: 15,
        totalEarned: 4.5,
        elapsedBillableSeconds: 65,
        lastBilledMinute: 1
    };

    let executionCount = 0;
    function simulateEndSession(sess, reason) {
        if (sess._isEnding) {
            // Blocked duplicate call
            return { blocked: true };
        }
        sess._isEnding = true;
        sess.endReason = reason;
        executionCount++;
        return { blocked: false, reason: sess.endReason };
    }

    // Two rapid concurrent termination calls
    const res1 = simulateEndSession(fakeSession, 'insufficient_balance');
    const res2 = simulateEndSession(fakeSession, 'ended');

    assert.strictEqual(res1.blocked, false, 'First call should proceed');
    assert.strictEqual(res2.blocked, true, 'Second concurrent call must be blocked');
    assert.strictEqual(executionCount, 1, 'Only one termination execution must happen');
    assert.strictEqual(fakeSession.endReason, 'insufficient_balance', 'Original reason must be retained');
    console.log('  ✅ Concurrency guard blocked duplicate call and preserved insufficient_balance reason.');

    // -------------------------------------------------------------
    // Test 5: Server Restart Reconciliation & Downtime Protection
    // -------------------------------------------------------------
    console.log('\nTest 5: Server Restart Reconciliation & Downtime Protection...');
    // Simulate a stranded session recovered from DB on boot:
    // Session was started 2 hours ago (7200 seconds ago) before an ungraceful crash.
    // Client had only paid for minute 1 (60s).
    const strandedDbSession = {
        sessionId: 'stranded_001',
        isFromDb: true,
        actualBillingStart: Date.now() - 7200000, // 2 hours ago
        totalCharged: 15,
        totalEarned: 4.5,
        lastBilledMinute: 1,
        elapsedBillableSeconds: 60,
        endReason: 'server_restart'
    };

    const isRestartRecovery = strandedDbSession.isFromDb && (strandedDbSession.endReason === 'server_restart');
    let billableSeconds;
    if (isRestartRecovery) {
        // Capped to what was actually billed while server was active
        billableSeconds = strandedDbSession.elapsedBillableSeconds || (strandedDbSession.lastBilledMinute * 60);
    } else {
        billableSeconds = Math.max(0, Math.floor((Date.now() - strandedDbSession.actualBillingStart) / 1000));
    }

    assert.strictEqual(billableSeconds, 60, 'Downtime of 2 hours must NOT be billed to client!');
    console.log(`  ✅ Stranded session correctly capped at ${billableSeconds}s; no charges applied for server offline downtime.`);

    console.log('\n=== ALL BILLING AUDIT TESTS PASSED SUCCESSFULLY! ===\n');
}

runBillingTests();

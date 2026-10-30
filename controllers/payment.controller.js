const crypto = require('crypto');
const User = require('../models/User');
const Payment = require('../models/Payment');
const { paymentTokens, userSockets } = require('../services/socketStore');
const phonepeConfig = require('../config/phonepe');

exports.createToken = async (req, res) => {
    try {
        const { userId, amount, couponCode } = req.body;
        if (!userId || !amount) return res.json({ ok: false, error: 'Missing userId or amount' });
        if (amount < 1) return res.json({ ok: false, error: 'Minimum amount is ₹1' });

        const user = await User.findOne({ userId });
        if (!user) return res.json({ ok: false, error: 'User not found' });

        const baseAmount = parseFloat(amount);
        const gstAmount = baseAmount * 0.18;
        const totalAmount = baseAmount + gstAmount;
        const token = crypto.randomBytes(32).toString('hex');

        paymentTokens.set(token, {
            userId, baseAmount, gstAmount, amount: totalAmount,
            couponCode: couponCode || "", createdAt: Date.now(),
            used: false, userName: user.name, userPhone: user.phone
        });

        console.log(`Payment Token Created: ${token.substring(0, 8)}... for ${user.name} amount ₹${amount}`);
        res.json({ ok: true, token });
    } catch (e) {
        console.error(e);
        res.json({ ok: false, error: 'Failed' });
    }
};

exports.verifyToken = async (req, res) => {
    const { token } = req.query;
    if (!token) return res.json({ valid: false, error: 'Token required' });

    const tokenData = paymentTokens.get(token);
    if (!tokenData) return res.json({ valid: false, error: 'Invalid token' });

    const expiryTime = 10 * 60 * 1000;
    if (Date.now() - tokenData.createdAt > expiryTime) {
        paymentTokens.delete(token);
        return res.json({ valid: false, error: 'Token expired' });
    }

    res.json({
        valid: true,
        amount: Math.round(tokenData.amount || tokenData.baseAmount || 0),
        baseAmount: Math.round(tokenData.baseAmount || 0),
        gstAmount: Math.round(tokenData.gstAmount || 0),
        userName: tokenData.userName || "Astro User",
        expiresIn: Math.max(0, Math.floor((expiryTime - (Date.now() - tokenData.createdAt)) / 1000))
    });
};

exports.validateCoupon = async (req, res) => {
    const { couponCode, amount } = req.body;
    if (!couponCode || !amount) return res.json({ ok: false, error: 'Missing code or amount' });

    const code = couponCode.toUpperCase().trim();
    const baseAmount = parseFloat(amount);

    if (code === 'WELCOME50') {
        return res.json({
            ok: true, bonus: baseAmount * 0.50,
            message: 'WELCOME50 Applied! 50% Bonus added to Super Wallet.'
        });
    }
    return res.json({ ok: false, error: 'Invalid coupon code' });
};

exports.createPayment = async (req, res) => {
    try {
        let { userId, amount, isApp, token, isSuperWallet, offerPercentage, couponCode } = req.body;
        let baseAmount = 0, gstAmount = 0, couponBonus = 0;

        if (token) {
            const tokenData = paymentTokens.get(token);
            if (!tokenData || (Date.now() - tokenData.createdAt > 600000) || tokenData.used) {
                return res.json({ ok: false, error: 'Invalid token' });
            }
            tokenData.used = true;
            userId = tokenData.userId;
            amount = tokenData.amount;
            baseAmount = tokenData.baseAmount || amount;
            gstAmount = tokenData.gstAmount || 0;
            couponCode = tokenData.couponCode || couponCode;
        } else {
            baseAmount = parseFloat(amount);
            gstAmount = baseAmount * 0.18;
            amount = baseAmount + gstAmount;
        }

        if (!amount || !userId) return res.json({ ok: false, error: 'Missing data' });

        if (couponCode === 'WELCOME50') couponBonus = baseAmount * 0.50;

        const user = await User.findOne({ userId });
        const userMobile = user ? (user.phone || "9999999999").replace(/[^0-9]/g, '').slice(-10) : "9999999999";
        const merchantTransactionId = "TXN" + Date.now() + Math.floor(Math.random() * 10000);
        const cleanUserId = userId.replace(/[^a-zA-Z0-9]/g, '');

        const serverUrl = process.env.SERVER_URL || 'https://astroeleven.com';

        // Save Pending Payment in DB
        await Payment.create({
            transactionId: merchantTransactionId,
            merchantTransactionId,
            userId, amount, baseAmount, gstAmount, status: 'pending',
            withGst: true, isApp: !!isApp, isSuperWallet: !!isSuperWallet || !!couponBonus,
            offerPercentage: parseFloat(offerPercentage || 0),
            couponCode: couponCode || null, couponBonus
        });

        const userMobileClean = (userMobile || "").replace(/[^0-9]/g, '').slice(-10);
        const validMobile = /^[6-9]\d{9}$/.test(userMobileClean) ? userMobileClean : undefined;
        const validUserId = (cleanUserId || "user123").substring(0, 35);

        // ===== PhonePe PG Checkout V2 Flow =====
        // Step 1: OAuth Access Token
        const tokenParams = new URLSearchParams();
        tokenParams.append('client_id', phonepeConfig.CLIENT_ID);
        tokenParams.append('client_secret', phonepeConfig.CLIENT_SECRET);
        tokenParams.append('client_version', phonepeConfig.CLIENT_VERSION);
        tokenParams.append('grant_type', 'client_credentials');

        const tokenRes = await fetch(phonepeConfig.TOKEN_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: tokenParams.toString()
        });

        const tokenData = await tokenRes.json();
        console.log('[PhonePe V2 OAuth Token]', JSON.stringify(tokenData));
        const accessToken = tokenData.access_token;

        if (!accessToken) {
            return res.json({ ok: false, error: tokenData.error_description || tokenData.message || 'OAuth authentication failed' });
        }

        // Step 2: Create V2 Checkout Order
        const payBody = {
            merchantOrderId: merchantTransactionId,
            amount: Math.round(amount * 100), // Paise
            expireAfter: 1200,
            paymentFlow: {
                type: 'PG_CHECKOUT',
                message: 'AstroEleven Wallet Recharge',
                merchantUrls: {
                    redirectUrl: `${serverUrl}/api/payment/callback?isApp=${!!isApp}&txnId=${merchantTransactionId}`
                }
            }
        };

        const payRes = await fetch(phonepeConfig.PAY_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'O-Bearer ' + accessToken
            },
            body: JSON.stringify(payBody)
        });

        const data = await payRes.json();
        console.log('[PhonePe V2 Order Create]', JSON.stringify(data));

        if (data && (data.redirectUrl || data.orderId)) {
            let redirectUrl = data.redirectUrl || '';
            res.json({
                ok: true,
                usePhonePe: true,
                paymentUrl: redirectUrl,
                redirectUrl: redirectUrl,
                orderId: data.orderId,
                merchantTransactionId: merchantTransactionId,
                transactionId: merchantTransactionId
            });
        } else {
            res.json({ ok: false, error: data?.message || data?.error || 'PhonePe V2 payment initialization failed' });
        }
    } catch (e) {
        console.error("PhonePe Payment Create Error:", e);
        res.json({ ok: false, error: 'Could not create PhonePe payment order: ' + e.message });
    }
};

// Helper to check PhonePe V2 Order Status
async function checkPhonePeOrderStatus(merchantTransactionId) {
    const tokenParams = new URLSearchParams();
    tokenParams.append('client_id', phonepeConfig.CLIENT_ID);
    tokenParams.append('client_secret', phonepeConfig.CLIENT_SECRET);
    tokenParams.append('client_version', phonepeConfig.CLIENT_VERSION);
    tokenParams.append('grant_type', 'client_credentials');

    const tokenRes = await fetch(phonepeConfig.TOKEN_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: tokenParams.toString()
    });

    const tokenData = await tokenRes.json();
    const accessToken = tokenData.access_token;
    if (!accessToken) {
        return { ok: false, error: tokenData.error_description || 'PhonePe authentication failed' };
    }

    const statusRes = await fetch(`https://api.phonepe.com/apis/pg/checkout/v2/order/${merchantTransactionId}/status`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'O-Bearer ' + accessToken
        }
    });
    const data = await statusRes.json();
    return { ok: true, data };
}

// Reusable payment processing helper
async function processPaymentSuccess(merchantTransactionId, providerRefId, io) {
    const payment = await Payment.findOne({ transactionId: merchantTransactionId });
    if (!payment) return { success: false, error: 'Payment record not found' };
    if (payment.status === 'success') {
        return { success: true, alreadyProcessed: true, payment };
    }

    // 1. Update Payment status atomically
    await Payment.updateOne(
        { transactionId: merchantTransactionId },
        { 
            $set: { 
                status: 'success', 
                providerRefId: providerRefId || payment.providerRefId || ''
            } 
        }
    );

    // 2. Atomically increment User walletBalance & superWalletBalance
    const baseCredit = Number(payment.baseAmount || payment.amount || 0);
    const superCredit = Number(payment.couponBonus || 0);

    const incUpdate = { walletBalance: baseCredit };
    if (superCredit > 0) {
        incUpdate.superWalletBalance = superCredit;
    }

    await User.updateOne({ userId: payment.userId }, { $inc: incUpdate });

    // 3. Fetch fresh User record for notifications and referral checks
    const user = await User.findOne({ userId: payment.userId });
    if (user) {
        // Referrer Reward logic
        if (user.referredBy) {
            try {
                const successCount = await Payment.countDocuments({
                    userId: user.userId,
                    status: 'success',
                    reason: 'recharge'
                });

                if (successCount === 1) {
                    const referrer = await User.findOne({ userId: user.referredBy });
                    if (referrer) {
                        await User.updateOne(
                            { userId: referrer.userId },
                            {
                                $inc: {
                                    walletBalance: 81,
                                    totalEarnings: 81,
                                    referralCount: 1
                                }
                            }
                        );

                        const refSocketId = userSockets.get(referrer.userId);
                        if (io) {
                            const refPayload = {
                                balance: (referrer.walletBalance || 0) + 81,
                                superBalance: referrer.superWalletBalance || 0
                            };
                            if (refSocketId) io.to(refSocketId).emit('wallet-update', refPayload);
                            io.to(referrer.userId).emit('wallet-update', refPayload);
                        }

                        await Payment.create({
                            transactionId: `REF_${crypto.randomBytes(8).toString('hex')}`,
                            userId: referrer.userId,
                            amount: 81,
                            baseAmount: 81,
                            gstAmount: 0,
                            status: 'success',
                            reason: 'referral'
                        });
                    }
                }
            } catch (refErr) {
                console.error('[Payment] Referral reward error:', refErr);
            }
        }

        // Emit Socket.IO balance update to user room & socketId
        if (io) {
            const socketId = userSockets.get(user.userId);
            const payload = {
                balance: user.walletBalance,
                superBalance: user.superWalletBalance
            };
            if (socketId) io.to(socketId).emit('wallet-update', payload);
            io.to(user.userId).emit('wallet-update', payload);
        }
    }

    console.log(`[Payment] Successfully credited ₹${baseCredit} to user ${payment.userId} (TXN: ${merchantTransactionId})`);
    return { success: true, payment, user };
}

exports.checkPhonePeOrderStatus = checkPhonePeOrderStatus;
exports.processPaymentSuccess = processPaymentSuccess;

exports.callback = async (req, res) => {
    try {
        const io = req.app.get('io');

        // PhonePe Webhook sends base64 payload in req.body.response
        let decodedPayload = null;
        if (req.body && req.body.response) {
            try {
                const decodedStr = Buffer.from(req.body.response, 'base64').toString('utf8');
                decodedPayload = JSON.parse(decodedStr);
                console.log('[PhonePe Webhook Decoded Payload]:', JSON.stringify(decodedPayload));
            } catch (err) {
                console.error('[PhonePe Webhook Decode Error]:', err.message);
            }
        }

        const merchantTransactionId = req.query.txnId 
            || req.body.merchantTransactionId 
            || req.body.transactionId 
            || req.query.transactionId
            || decodedPayload?.data?.merchantTransactionId
            || decodedPayload?.data?.merchantOrderId;

        const isApp = req.query.isApp === 'true' || req.body.isApp === true;

        if (!merchantTransactionId) {
            if (isApp) return res.redirect("astroeleven://payment-failed?status=failed");
            return res.json({ ok: false, error: 'Missing transactionId' });
        }

        // Verify PhonePe V2 Status
        const statusResult = await checkPhonePeOrderStatus(merchantTransactionId);
        let isSuccess = false;
        let providerRefId = '';

        if (statusResult.ok && statusResult.data) {
            const data = statusResult.data;
            console.log(`[PhonePe V2 Status Check] TXN: ${merchantTransactionId} ->`, JSON.stringify(data));
            if (data.state === 'COMPLETED' || data.state === 'SUCCESS' || data.code === 'PAYMENT_SUCCESS' || data.success === true) {
                isSuccess = true;
                providerRefId = data.orderId || data.providerReferenceId || '';
            }
        } else {
            console.warn(`[PhonePe Status Check Failed] TXN: ${merchantTransactionId} ->`, statusResult.error);
        }

        if (isSuccess) {
            await processPaymentSuccess(merchantTransactionId, providerRefId, io);

            if (isApp) {
                return res.send(`
                    <!DOCTYPE html>
                    <html>
                    <head><title>Payment Success</title><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                    <body style="background:#150E0C; color:#4ADE80; font-family:sans-serif; text-align:center; padding-top:80px;">
                        <h2 style="color:#FFD700; margin-bottom:10px;">⚡ Payment Successful!</h2>
                        <p style="color:#FFFFFF;">Updating your wallet balance...</p>
                        <script>
                            window.location.href = "astroeleven://payment-success?status=success";
                            setTimeout(function() {
                                window.location.href = "astroeleven://payment-success?status=success";
                            }, 800);
                        </script>
                    </body>
                    </html>
                `);
            }
            return res.json({ ok: true, status: 'success' });
        } else {
            // Update payment status to failed in database
            try {
                await Payment.updateOne(
                    { transactionId: merchantTransactionId },
                    { $set: { status: 'failed' } }
                );
            } catch (failDbErr) {
                console.error('[Payment] Error setting failed status:', failDbErr);
            }

            if (isApp) {
                return res.send(`
                    <!DOCTYPE html>
                    <html>
                    <head><title>Payment Failed</title><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                    <body style="background:#150E0C; color:#EF4444; font-family:sans-serif; text-align:center; padding-top:80px;">
                        <h2 style="color:#EF4444; margin-bottom:10px;">❌ Payment Failed</h2>
                        <p style="color:#9CA3AF;">Returning to app...</p>
                        <script>
                            window.location.href = "astroeleven://payment-failed?status=failed";
                            setTimeout(function() {
                                window.location.href = "astroeleven://payment-failed?status=failed";
                            }, 800);
                        </script>
                    </body>
                    </html>
                `);
            }
            return res.json({ ok: false, error: 'Payment status not success' });
        }
    } catch (e) {
        console.error("PhonePe Callback Error:", e);
        if (req.query.isApp === 'true' || req.body.isApp === true) {
            return res.redirect("astroeleven://payment-failed?status=failed");
        }
        res.json({ ok: false, error: 'Verification failed' });
    }
};

exports.getHistory = async (req, res) => {
    try {
        const { userId } = req.params;
        const payments = await Payment.find({ userId }).sort({ createdAt: -1 }).limit(50);
        res.json({ ok: true, payments });
    } catch (e) {
        res.json({ ok: false });
    }
};

// Dedicated PhonePe Server-to-Server (S2S) Webhook Endpoint
exports.webhook = async (req, res) => {
    try {
        const io = req.app.get('io');
        console.log('[PhonePe Webhook Received] Body:', JSON.stringify(req.body));

        let decodedPayload = null;
        if (req.body && req.body.response) {
            try {
                const decodedStr = Buffer.from(req.body.response, 'base64').toString('utf8');
                decodedPayload = JSON.parse(decodedStr);
                console.log('[PhonePe Webhook Decoded Payload]:', JSON.stringify(decodedPayload));
            } catch (err) {
                console.error('[PhonePe Webhook Decode Error]:', err.message);
            }
        }

        const eventPayload = req.body?.payload || req.body?.data || decodedPayload?.data || decodedPayload || {};

        const merchantTransactionId = req.body?.merchantTransactionId 
            || req.body?.transactionId 
            || req.body?.merchantOrderId
            || eventPayload?.merchantOrderId
            || eventPayload?.merchantTransactionId
            || eventPayload?.orderId
            || req.query?.txnId 
            || req.query?.transactionId;

        if (!merchantTransactionId) {
            console.warn('[PhonePe Webhook] Missing transaction ID in payload. Body:', JSON.stringify(req.body));
            return res.status(200).json({ ok: false, error: 'Missing transaction identifier' });
        }

        // Cross-verify with PhonePe Order Status API
        const statusResult = await checkPhonePeOrderStatus(merchantTransactionId);
        let isSuccess = false;
        let isFailed = false;
        let providerRefId = eventPayload?.orderId || eventPayload?.transactionId || '';

        if (statusResult.ok && statusResult.data) {
            const data = statusResult.data;
            if (data.state === 'COMPLETED' || data.state === 'SUCCESS' || data.code === 'PAYMENT_SUCCESS' || data.success === true) {
                isSuccess = true;
                providerRefId = data.orderId || data.providerReferenceId || providerRefId;
            } else if (data.state === 'FAILED') {
                isFailed = true;
            }
        } else {
            // Direct event fallback if status API is momentarily slow
            const eventName = req.body?.event || '';
            const eventState = eventPayload?.state || decodedPayload?.code || '';
            if (eventName === 'checkout.order.completed' || eventState === 'COMPLETED' || eventState === 'SUCCESS' || eventState === 'PAYMENT_SUCCESS') {
                isSuccess = true;
            } else if (eventName === 'checkout.order.failed' || eventState === 'FAILED' || eventState === 'PAYMENT_ERROR') {
                isFailed = true;
            }
        }

        if (isSuccess) {
            const result = await processPaymentSuccess(merchantTransactionId, providerRefId, io);
            console.log(`[PhonePe Webhook] Credited successfully for ${merchantTransactionId}`);
            return res.status(200).json({ ok: true, message: 'Payment credited successfully', result });
        } else if (isFailed) {
            await Payment.updateOne({ transactionId: merchantTransactionId }, { $set: { status: 'failed' } });
            console.log(`[PhonePe Webhook] Marked payment as failed for ${merchantTransactionId}`);
            return res.status(200).json({ ok: true, message: 'Payment marked as failed' });
        }

        return res.status(200).json({ ok: true, message: 'Webhook received' });
    } catch (e) {
        console.error('[PhonePe Webhook Error]:', e);
        return res.status(200).json({ ok: false, error: e.message });
    }
};

const RechargePackage = require('../models/RechargePackage');

const DEFAULT_PACKAGES = [
    { packageId: 'pack_2', amount: 2, extraPercentage: 0, badge: 'Trial Pack', description: 'Trial Recharge', isActive: true, order: 1 },
    { packageId: 'pack_20', amount: 20, extraPercentage: 50, badge: '50% Extra', description: 'Get 50% Extra', isActive: true, order: 2 },
    { packageId: 'pack_50', amount: 50, extraPercentage: 50, badge: '50% Extra', description: 'Get 50% Extra', isActive: true, order: 3 },
    { packageId: 'pack_100', amount: 100, extraPercentage: 100, badge: 'Popular', description: 'Get 100% Extra', isActive: true, order: 4 },
    { packageId: 'pack_200', amount: 200, extraPercentage: 10, badge: '10% Extra', description: 'Get 10% Extra', isActive: true, order: 5 },
    { packageId: 'pack_500', amount: 500, extraPercentage: 100, badge: 'Best Value', description: 'Get 100% Extra', isActive: true, order: 6 },
    { packageId: 'pack_1000', amount: 1000, extraPercentage: 100, badge: '100% Extra', description: 'Get 100% Extra', isActive: true, order: 7 },
    { packageId: 'pack_2000', amount: 2000, extraPercentage: 100, badge: '100% Extra', description: 'Get 100% Extra', isActive: true, order: 8 },
    { packageId: 'pack_5000', amount: 5000, extraPercentage: 20, badge: 'Mega Saver', description: 'Get 20% Extra', isActive: true, order: 9 }
];

async function ensureDefaultPackages() {
    try {
        const count = await RechargePackage.countDocuments({});
        if (count === 0) {
            for (const p of DEFAULT_PACKAGES) {
                await RechargePackage.create(p);
            }
            console.log('[RechargePackage] Seeded default recharge packages');
        }
    } catch (e) {
        console.error('[RechargePackage] Seeding error:', e.message);
    }
}

exports.getPublicRechargePackages = async (req, res) => {
    try {
        await ensureDefaultPackages();
        const packages = await RechargePackage.find({ isActive: true }).sort({ order: 1, amount: 1 }).lean();
        res.json({ ok: true, packages });
    } catch (e) {
        console.error('[PaymentController] Error in getPublicRechargePackages:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};


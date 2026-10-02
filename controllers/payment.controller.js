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

exports.callback = async (req, res) => {
    try {
        const io = req.app.get('io');
        const merchantTransactionId = req.query.txnId || req.body.merchantTransactionId || req.body.transactionId || req.query.transactionId;
        const isApp = req.query.isApp === 'true' || req.body.isApp === true;

        if (!merchantTransactionId) {
            if (isApp) return res.redirect("astroeleven://payment-failed?status=failed");
            return res.json({ ok: false, error: 'Missing transactionId' });
        }

        // Verify PhonePe V2 Status
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

        let isSuccess = false;
        let providerRefId = '';

        if (accessToken) {
            const statusRes = await fetch(`https://api.phonepe.com/apis/pg/checkout/v2/order/${merchantTransactionId}/status`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'O-Bearer ' + accessToken
                }
            });
            const data = await statusRes.json();
            console.log(`[PhonePe V2 Status Check] TXN: ${merchantTransactionId} ->`, JSON.stringify(data));
            if (data.state === 'COMPLETED' || data.state === 'SUCCESS' || data.code === 'PAYMENT_SUCCESS' || data.success === true) {
                isSuccess = true;
                providerRefId = data.orderId || data.providerReferenceId || '';
            }
        }

        if (isSuccess) {
            const payment = await Payment.findOne({ transactionId: merchantTransactionId });
            if (payment && payment.status !== 'success') {
                payment.status = 'success';
                payment.providerRefId = providerRefId;
                await payment.save();

                const user = await User.findOne({ userId: payment.userId });
                if (user) {
                    user.walletBalance = (user.walletBalance || 0) + payment.baseAmount;
                    if (payment.couponBonus > 0) {
                        user.superWalletBalance = (user.superWalletBalance || 0) + payment.couponBonus;
                    }

                    // Referrer Reward
                    if (user.referredBy) {
                        const successCount = await Payment.countDocuments({
                            userId: user.userId,
                            status: 'success',
                            reason: 'recharge'
                        });

                        if (successCount === 1) {
                            const referrer = await User.findOne({ userId: user.referredBy });
                            if (referrer) {
                                referrer.walletBalance = (referrer.walletBalance || 0) + 81;
                                referrer.totalEarnings = (referrer.totalEarnings || 0) + 81;
                                referrer.referralCount = (referrer.referralCount || 0) + 1;
                                await referrer.save();

                                const refSocketId = userSockets.get(referrer.userId);
                                if (io && refSocketId) {
                                    io.to(refSocketId).emit('wallet-update', {
                                        balance: referrer.walletBalance,
                                        superBalance: referrer.superWalletBalance
                                    });
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
                    }

                    await user.save();

                    const socketId = userSockets.get(user.userId);
                    if (io && socketId) {
                        io.to(socketId).emit('wallet-update', {
                            balance: user.walletBalance,
                            superBalance: user.superWalletBalance
                        });
                    }
                }
            }

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

const crypto = require('crypto');
const User = require('../models/User');
const { otpStore } = require('../services/socketStore');
const { sendSMS } = require('../services/otp.service');
const { generateUniqueReferralCode } = require('../utils/helpers');
const { formatImageUrl } = require('../utils/formatImage');

const normalizePhone = (p) => {
    if (!p) return p;
    const clean = String(p).replace(/\D/g, '');
    return clean.length === 10 ? '91' + clean : clean;
};

const findUserByPhoneHelper = async (phone) => {
    if (!phone) return null;
    const clean = String(phone).replace(/\D/g, '');
    const tenDigits = clean.slice(-10);
    const phoneQuery = Array.from(new Set([
        phone,
        clean,
        tenDigits,
        '91' + tenDigits,
        '+91' + tenDigits,
        '+' + clean
    ]));
    return await User.findOne({ phone: { $in: phoneQuery } });
};

exports.sendOtp = async (req, res) => {
    let { phone } = req.body;
    if (!phone) return res.json({ ok: false, error: 'Phone required' });
    phone = normalizePhone(phone);

    const cleanPhone = phone.replace(/[^0-9]/g, '').slice(-10);

    // Dummy test super admin number bypass (9876543210 uses fixed PIN 1369)
    if (cleanPhone === '9876543210') {
        const pass = '1369';
        otpStore.set(phone, { otp: pass, expires: Date.now() + 300000 });
        otpStore.set(cleanPhone, { otp: pass, expires: Date.now() + 300000 });
        otpStore.set('91' + cleanPhone, { otp: pass, expires: Date.now() + 300000 });
        console.log(`[TEST/ADMIN] Set bypass OTP 1369 for ${phone}`);
        return res.json({ ok: true });
    }

    // Test accounts bypass (8000000001, 9000000001, etc. use fixed PIN 0101)
    if (cleanPhone === '8000000001' || cleanPhone === '9000000001' || cleanPhone === '8000000000' || cleanPhone === '9000000000' || cleanPhone === '80000001' || cleanPhone === '90000001') {
        const pass = '0101';
        otpStore.set(phone, { otp: pass, expires: Date.now() + 300000 });
        otpStore.set(cleanPhone, { otp: pass, expires: Date.now() + 300000 });
        otpStore.set('91' + cleanPhone, { otp: pass, expires: Date.now() + 300000 });
        console.log(`[TEST] Set test OTP 0101 for ${phone}`);
        return res.json({ ok: true });
    }

    // Real OTP generation & SMS dispatch (INCLUDING Super Admin 7305307369 and all live users)
    const otp = Math.floor(1000 + Math.random() * 9000).toString();
    otpStore.set(phone, { otp, expires: Date.now() + 300000 });
    otpStore.set(cleanPhone, { otp, expires: Date.now() + 300000 });
    otpStore.set('91' + cleanPhone, { otp, expires: Date.now() + 300000 });

    try {
        await sendSMS(phone, otp);
        console.log(`[SMS OTP] Dispatched OTP ${otp} to phone: ${phone} (mobile: ${cleanPhone})`);
    } catch (smsErr) {
        console.error(`[SMS OTP ERROR] Failed sending SMS to ${phone}:`, smsErr);
    }

    res.json({ ok: true });
};

exports.verifyOtp = async (req, res) => {
    let { phone, otp } = req.body;
    if (!phone) return res.json({ ok: false, error: 'Phone required' });
    if (!otp) return res.json({ ok: false, error: 'OTP required' });
    otp = String(otp).trim();
    phone = normalizePhone(phone);
    const SERVER_URL = req.app.get('SERVER_URL');

    // Super Admin & Authorized Admin Detection
    const cleanPhoneVerify = phone.replace(/[^0-9]/g, '').slice(-10);
    let user = await findUserByPhoneHelper(phone);

    const isSuperAdminNumber = (cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '9876543210' || (user && (user.role === 'superadmin' || user.role === 'admin')));

    // --- Master Passkey & Direct Bypass (1369 or 0101 for superadmin) ---
    if (otp === '1369' || (isSuperAdminNumber && otp === '0101')) {
        if (isSuperAdminNumber) {
            if (!user) {
                user = await User.create({
                    userId: crypto.randomUUID(),
                    phone: cleanPhoneVerify === '7305307369' ? '917305307369' : phone,
                    name: 'Super Admin',
                    role: 'superadmin',
                    walletBalance: 100000,
                    referralCode: await generateUniqueReferralCode('Admin')
                });
            } else {
                let needsSave = false;
                if (user.role !== 'superadmin') {
                    user.role = 'superadmin';
                    needsSave = true;
                }
                if (cleanPhoneVerify === '7305307369' && user.name && user.name.startsWith('User_')) {
                    user.name = 'Super Admin';
                    needsSave = true;
                }
                if (needsSave) await user.save();
            }

            return res.json({
                ok: true,
                userId: user.userId,
                name: user.name || 'Super Admin',
                role: user.role || 'superadmin',
                phone: user.phone,
                walletBalance: user.walletBalance,
                superWalletBalance: user.superWalletBalance || 0,
                totalEarnings: user.totalEarnings || 0,
                image: formatImageUrl(user.image, user.name, SERVER_URL),
                referralCode: user.referralCode,
                isNewUser: user.isNewUser || false,
                approvalStatus: user.approvalStatus || 'approved',
                documentStatus: user.documentStatus || 'approved',
                email: user.email
            });
        }
    }

    // --- Test Astrologer Account ---
    if ((cleanPhoneVerify === '8000000001' || cleanPhoneVerify === '8000000000' || cleanPhoneVerify === '800000001') && otp === '0101') {
        if (!user) {
            user = await User.create({
                userId: crypto.randomUUID(),
                phone,
                name: 'Test Astrologer',
                role: 'astrologer',
                isOnline: true,
                isAvailable: true,
                ratePerMinute: 10,
                skills: ['Vedic', 'Prashana'],
                experience: 5,
                approvalStatus: 'approved',
                referralCode: await generateUniqueReferralCode('TestAstro')
            });
        } else if (user.role !== 'astrologer') {
            user.role = 'astrologer';
            user.isOnline = true;
            user.isAvailable = true;
            user.approvalStatus = 'approved';
            user.skills = user.skills && user.skills.length > 0 ? user.skills : ['Vedic', 'Prashana'];
            user.ratePerMinute = user.ratePerMinute || 10;
            await user.save();
        }
        return res.json({
            ok: true, userId: user.userId, name: user.name, role: user.role,
            phone: user.phone, walletBalance: user.walletBalance,
            totalEarnings: user.totalEarnings || 0,
            image: formatImageUrl(user.image, user.name, SERVER_URL),
            ratePerMinute: user.ratePerMinute,
            email: user.email
        });
    }

    // --- Test Client Account ---
    if ((cleanPhoneVerify === '9000000001' || cleanPhoneVerify === '9000000000' || cleanPhoneVerify === '900000001') && otp === '0101') {
        if (!user) {
            user = await User.create({
                userId: crypto.randomUUID(),
                phone,
                name: 'Test Client',
                role: 'client',
                walletBalance: 1000,
                referralCode: await generateUniqueReferralCode('TestClient')
            });
        }

        return res.json({
            ok: true, userId: user.userId, name: user.name, role: user.role,
            phone: user.phone, walletBalance: user.walletBalance,
            superWalletBalance: user.superWalletBalance || 0,
            totalEarnings: user.totalEarnings || 0,
            image: formatImageUrl(user.image, user.name, SERVER_URL),
            email: user.email
        });
    }

    // --- Validate Real SMS OTP from Store ---
    const entry = otpStore.get(phone) || otpStore.get(cleanPhoneVerify) || otpStore.get('91' + cleanPhoneVerify);
    if (!entry) return res.json({ ok: false, error: 'No OTP requested' });
    if (Date.now() > entry.expires) return res.json({ ok: false, error: 'Expired' });
    if (entry.otp !== otp) return res.json({ ok: false, error: 'Invalid OTP' });

    // Clean up OTP store
    otpStore.delete(phone);
    otpStore.delete(cleanPhoneVerify);
    otpStore.delete('91' + cleanPhoneVerify);

    try {
        if (user && user.isBanned) return res.json({ ok: false, error: 'Account Banned' });

        if (cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '9876543210') {
            if (!user) {
                user = await User.create({
                    userId: crypto.randomUUID(),
                    phone: cleanPhoneVerify === '7305307369' ? '917305307369' : phone,
                    name: 'Super Admin',
                    role: 'superadmin',
                    walletBalance: 100000,
                    referralCode: await generateUniqueReferralCode('Admin')
                });
            } else {
                let needsSave = false;
                if (user.role !== 'superadmin') {
                    user.role = 'superadmin';
                    needsSave = true;
                }
                if (cleanPhoneVerify === '7305307369' && user.name && user.name.startsWith('User_')) {
                    user.name = 'Super Admin';
                    needsSave = true;
                }
                if (needsSave) await user.save();
            }
        } else if (!user) {
            const { referralCode: codeApplied } = req.body;
            let referredBy = null;
            let initialBalance = 0; // Standard bonus (Disabled - set to 0)

            if (codeApplied) {
                const referrer = await User.findOne({ referralCode: codeApplied.trim().toUpperCase() });
                if (referrer) {
                    referredBy = referrer.userId;
                    initialBalance = 0; // Referral bonus (Disabled - set to 0)
                }
            }

            const userId = crypto.randomUUID();
            const name = `User_${crypto.randomBytes(2).toString('hex')}`;
            user = await User.create({
                userId, phone, name, role: 'client',
                walletBalance: initialBalance,
                referredBy: referredBy,
                referralCode: await generateUniqueReferralCode(name),
                isNewUser: true
            });
        } else if (!user.referralCode) {
            user.referralCode = await generateUniqueReferralCode(user.name);
            await user.save();
        }

        res.json({
            ok: true, userId: user.userId, name: user.name, role: user.role,
            phone: user.phone, walletBalance: user.walletBalance,
            superWalletBalance: user.superWalletBalance || 0,
            totalEarnings: user.totalEarnings || 0,
            image: formatImageUrl(user.image, user.name, SERVER_URL),
            referralCode: user.referralCode, isNewUser: user.isNewUser,
            referredBy: user.referredBy,
            approvalStatus: user.approvalStatus, documentStatus: user.documentStatus,
            email: user.email
        });
    } catch (e) {
        console.error(e);
        res.status(500).json({ ok: false, error: 'Server error' });
    }
};

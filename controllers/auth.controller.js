const crypto = require('crypto');
const User = require('../models/User');
const { otpStore } = require('../services/socketStore');
const { sendSMS } = require('../services/otp.service');
const { generateUniqueReferralCode } = require('../utils/helpers');
const { formatImageUrl } = require('../utils/formatImage');
const mongoose = require('../utils/mongoose-mysql');

// Ensure user_otps table exists in MySQL for multi-worker LiteSpeed sync
const ensureOtpTable = async () => {
    try {
        if (mongoose.pool) {
            await mongoose.pool.query(`
                CREATE TABLE IF NOT EXISTS user_otps (
                    phone VARCHAR(50) PRIMARY KEY,
                    otp VARCHAR(10) NOT NULL,
                    expires BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            `);
        }
    } catch (e) {
        // Pool initializing or table exists
    }
};
ensureOtpTable();

const saveOtpToStore = async (phone, cleanPhone, otp, expires) => {
    // 1. In-memory save
    otpStore.set(phone, { otp, expires });
    otpStore.set(cleanPhone, { otp, expires });
    otpStore.set('91' + cleanPhone, { otp, expires });

    // 2. Persistent MySQL save (cross-process sync for LiteSpeed)
    try {
        if (mongoose.pool) {
            await mongoose.pool.query(
                'REPLACE INTO user_otps (phone, otp, expires) VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)',
                [phone, otp, expires, cleanPhone, otp, expires, '91' + cleanPhone, otp, expires]
            );
        }
    } catch (dbErr) {
        console.error('[DB OTP SAVE ERROR]', dbErr.message || dbErr);
    }
};

const getOtpFromStore = async (phone, cleanPhone) => {
    // 1. Check in-memory store
    let entry = otpStore.get(phone) || otpStore.get(cleanPhone) || otpStore.get('91' + cleanPhone);
    if (entry) return entry;

    // 2. Fallback to MySQL store (if hit a different LiteSpeed worker process)
    try {
        if (mongoose.pool) {
            const [rows] = await mongoose.pool.query(
                'SELECT otp, expires FROM user_otps WHERE phone IN (?, ?, ?) ORDER BY expires DESC LIMIT 1',
                [phone, cleanPhone, '91' + cleanPhone]
            );
            if (rows && rows.length > 0) {
                return rows[0];
            }
        }
    } catch (dbErr) {
        console.error('[DB OTP FETCH ERROR]', dbErr.message || dbErr);
    }
    return null;
};

const deleteOtpFromStore = async (phone, cleanPhone) => {
    // 1. Clean in-memory
    otpStore.delete(phone);
    otpStore.delete(cleanPhone);
    otpStore.delete('91' + cleanPhone);

    // 2. Clean MySQL
    try {
        if (mongoose.pool) {
            await mongoose.pool.query(
                'DELETE FROM user_otps WHERE phone IN (?, ?, ?)',
                [phone, cleanPhone, '91' + cleanPhone]
            );
        }
    } catch (dbErr) {
        console.error('[DB OTP DELETE ERROR]', dbErr.message || dbErr);
    }
};

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
    const expires = Date.now() + 300000; // 5 minutes

    // Dummy test super admin number bypass (9876543210 uses fixed PIN 1369)
    if (cleanPhone === '9876543210') {
        const pass = '1369';
        await saveOtpToStore(phone, cleanPhone, pass, expires);
        console.log(`[TEST/ADMIN] Set bypass OTP 1369 for ${phone}`);
        return res.json({ ok: true });
    }

    // Test accounts bypass (8000000001, 9000000001, etc. use fixed PIN 0101)
    if (cleanPhone === '8000000001' || cleanPhone === '9000000001' || cleanPhone === '8000000000' || cleanPhone === '9000000000' || cleanPhone === '80000001' || cleanPhone === '90000001') {
        const pass = '0101';
        await saveOtpToStore(phone, cleanPhone, pass, expires);
        console.log(`[TEST] Set test OTP 0101 for ${phone}`);
        return res.json({ ok: true });
    }

    // Real OTP generation & SMS dispatch (INCLUDING Super Admin 7305307369 / 7305304369 and all live users)
    const otp = Math.floor(1000 + Math.random() * 9000).toString();
    await saveOtpToStore(phone, cleanPhone, otp, expires);

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

    const isSuperAdminNumber = (
        cleanPhoneVerify === '7305307369' ||
        cleanPhoneVerify === '7305304369' ||
        cleanPhoneVerify === '9876543210' ||
        (user && (user.role === 'superadmin' || user.role === 'admin'))
    );

    // --- Master Passkey & Direct Bypass (1369 or 0101 for superadmin) ---
    if (otp === '1369' || (isSuperAdminNumber && otp === '0101')) {
        if (isSuperAdminNumber) {
            if (!user) {
                user = await User.create({
                    userId: crypto.randomUUID(),
                    phone: (cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '7305304369') ? ('91' + cleanPhoneVerify) : phone,
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
                if ((cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '7305304369') && user.name && user.name.startsWith('User_')) {
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

    // --- Validate Real SMS OTP from Multi-Process Store (In-Memory + MySQL) ---
    const entry = await getOtpFromStore(phone, cleanPhoneVerify);
    if (!entry) return res.json({ ok: false, error: 'No OTP requested' });
    if (Date.now() > Number(entry.expires)) return res.json({ ok: false, error: 'Expired' });
    if (String(entry.otp).trim() !== otp) return res.json({ ok: false, error: 'Invalid OTP' });

    // Clean up OTP store across all workers
    await deleteOtpFromStore(phone, cleanPhoneVerify);

    try {
        if (user && user.isBanned) return res.json({ ok: false, error: 'Account Banned' });

        if (cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '7305304369' || cleanPhoneVerify === '9876543210') {
            if (!user) {
                user = await User.create({
                    userId: crypto.randomUUID(),
                    phone: (cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '7305304369') ? ('91' + cleanPhoneVerify) : phone,
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
                if ((cleanPhoneVerify === '7305307369' || cleanPhoneVerify === '7305304369') && user.name && user.name.startsWith('User_')) {
                    user.name = 'Super Admin';
                    needsSave = true;
                }
                if (needsSave) await user.save();
            }
        } else if (!user) {
            const { referralCode: codeApplied, name: inputName, clientName } = req.body;
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
            const chosenName = (inputName || clientName || '').trim() || `User_${crypto.randomBytes(2).toString('hex')}`;
            user = await User.create({
                userId, phone, name: chosenName, role: 'client',
                walletBalance: initialBalance,
                referredBy: referredBy,
                referralCode: await generateUniqueReferralCode(chosenName),
                isNewUser: true
            });
        } else {
            const { name: inputName, clientName } = req.body;
            const chosenName = (inputName || clientName || '').trim();
            let needsSave = false;
            if (chosenName && (user.name.startsWith('User_') || user.name === 'Client' || user.name.trim() !== chosenName)) {
                user.name = chosenName;
                needsSave = true;
            }
            if (!user.referralCode) {
                user.referralCode = await generateUniqueReferralCode(user.name);
                needsSave = true;
            }
            if (needsSave) {
                await user.save();
            }
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

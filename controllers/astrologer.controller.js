const User = require('../models/User');
const { broadcastAstroUpdate } = require('../services/astrologer.service');

exports.register = async (req, res) => {
    try {
        const {
            name, phone, // legacy/legacy-fallback
            realName, displayName,
            gender, dob, tob, pob,
            phone1, phone2, whatsapp,
            email, address,
            aadhar, pan,
            experience, profession,
            bankDetails, upiName, upiId
        } = req.body;
        
        const normalizePhone = (p) => {
            if (!p) return p;
            const clean = p.replace(/\D/g, '');
            return clean.length === 10 ? '91' + clean : clean;
        };

        const finalPhone = normalizePhone(phone1 || phone);
        const finalName = displayName || name || realName;
        const finalRealName = realName || finalName;

        if (!finalName || !finalPhone) {
            return res.status(400).json({ ok: false, error: 'Registration failed: Name and Phone are required' });
        }

        const clean = finalPhone.replace(/\D/g, '');
        const phoneQuery = [finalPhone];
        if (clean.length === 12 && clean.startsWith('91')) {
            phoneQuery.push(clean.slice(2));
        } else if (clean.length === 10) {
            phoneQuery.push('91' + clean);
        }

        const existing = await User.findOne({ phone: { $in: phoneQuery } });
        if (existing) {
            return res.json({ ok: false, error: 'This phone number is already registered' });
        }

        const userId = 'ASTRO_' + Date.now() + Math.floor(Math.random() * 1000);
        
        const newUser = new User({
            userId,
            phone: finalPhone,
            name: finalName,
            realName: finalRealName,
            email: email || '',
            gender: gender || '',
            dob: dob || '',
            tob: tob || '',
            pob: pob || '',
            cellNumber2: phone2 || '',
            whatsAppNumber: whatsapp || '',
            address: address || '',
            aadharNumber: aadhar || '',
            panNumber: pan || '',
            astrologyExperience: experience || '',
            profession: profession || '',
            bankDetails: bankDetails || '',
            upiNumber: upiName || '', // Mapping upiName to upiNumber in schema context
            upiId: upiId || '',
            role: 'astrologer',
            approvalStatus: 'pending',
            isVerified: false,
            isAvailable: false, 
            isOnline: false,
            walletBalance: 0,
            totalEarnings: 0,
            ratePerMinute: 10, // Default rate
            price: 10 // Sync with ratePerMinute
        });

        await newUser.save();
        console.log(`[Registration] New Astrologer: ${finalName} (${userId}) - Status: Pending Approval`);
        res.json({ ok: true, message: 'Registration submitted successfully. Awaiting admin approval.' });
    } catch (error) {
        console.error('Registration Error:', error);
        res.status(500).json({ ok: false, error: 'Deep server error during registration' });
    }
};

exports.toggleOnline = async (req, res) => {
    const { userId, available, fcmToken } = req.body;
    if (!userId) return res.json({ ok: false, error: 'Missing userId' });
    const io = req.app.get('io');
    const SERVER_URL = req.app.get('SERVER_URL');

    try {
        const user = await User.findOne({ userId });
        if (!user || user.role !== 'astrologer') return res.json({ ok: false, error: 'Access denied' });
        if (user.approvalStatus !== 'approved') return res.json({ ok: false, error: 'Awaiting approval' });

        const update = {
            isAvailable: available, isOnline: available,
            isChatOnline: available, isAudioOnline: available, isVideoOnline: available,
            lastSeen: new Date()
        };
        if (fcmToken) update.fcmToken = fcmToken;

        await User.updateOne({ userId }, update);
        broadcastAstroUpdate(io, SERVER_URL);
        res.json({ ok: true });
    } catch (e) {
        console.error(e);
        res.json({ ok: false });
    }
};

exports.toggleService = async (req, res) => {
    const { userId, service, enabled } = req.body;
    if (!userId || !service) return res.json({ ok: false, error: 'Missing params' });
    const io = req.app.get('io');
    const SERVER_URL = req.app.get('SERVER_URL');

    try {
        const update = { lastSeen: new Date() };
        if (service === 'chat') update.isChatOnline = enabled;
        else if (service === 'audio') update.isAudioOnline = enabled;
        else if (service === 'video') update.isVideoOnline = enabled;

        const user = await User.findOne({ userId });
        if (user) {
            const chatOn = service === 'chat' ? enabled : user.isChatOnline;
            const audioOn = service === 'audio' ? enabled : user.isAudioOnline;
            const videoOn = service === 'video' ? enabled : user.isVideoOnline;
            update.isOnline = chatOn || audioOn || videoOn;
            update.isAvailable = update.isOnline; // Sync

            await User.updateOne({ userId }, update);
            broadcastAstroUpdate(io, SERVER_URL);
            res.json({ ok: true });
        } else {
            res.json({ ok: false, error: 'User not found' });
        }
    } catch (e) {
        console.error(e);
        res.json({ ok: false });
    }
};

exports.updateRate = async (req, res) => {
    const { userId, price, chatPrice, callPrice, videoPrice, isAdminOverride } = req.body;
    if (!userId) return res.json({ ok: false, error: 'Missing userId' });
    const io = req.app.get('io');
    const SERVER_URL = req.app.get('SERVER_URL');

    try {
        const user = await User.findOne({ userId });
        if (user) {
            // Astrologers cannot set rates > 50 on their own without Super Admin permission
            if (!isAdminOverride) {
                const isOverLimit = (r) => {
                    if (r !== undefined && r !== null && r !== '') {
                        const parsed = parseFloat(r);
                        return !isNaN(parsed) && parsed > 50;
                    }
                    return false;
                };

                if (isOverLimit(price) || isOverLimit(chatPrice) || isOverLimit(callPrice) || isOverLimit(videoPrice)) {
                    return res.json({
                        ok: false,
                        error: '₹50-க்கு மேல் கட்டணம் நிர்ணயிக்க சூப்பர் அட்மினிடம் அனுமதி பெற வேண்டும் (Rates above ₹50/min require Super Admin permission).'
                    });
                }
            }

            if (price !== undefined) {
                const val = parseFloat(price);
                if (!isNaN(val) && val >= 0) {
                    user.price = val;
                    user.ratePerMinute = val;
                }
            }
            if (chatPrice !== undefined) {
                const val = parseFloat(chatPrice);
                if (!isNaN(val) && val >= 0) user.chatPrice = val;
            }
            if (callPrice !== undefined) {
                const val = parseFloat(callPrice);
                if (!isNaN(val) && val >= 0) user.callPrice = val;
            }
            if (videoPrice !== undefined) {
                const val = parseFloat(videoPrice);
                if (!isNaN(val) && val >= 0) user.videoPrice = val;
            }

            await user.save();

            broadcastAstroUpdate(io, SERVER_URL);
            res.json({
                ok: true,
                price: user.price,
                chatPrice: user.chatPrice,
                callPrice: user.callPrice,
                videoPrice: user.videoPrice
            });
        } else {
            res.json({ ok: false, error: 'User not found' });
        }
    } catch (e) {
        console.error(e);
        res.json({ ok: false, error: e.message });
    }
};

exports.requestWithdrawal = async (req, res) => {
    try {
        const { userId, amount } = req.body;
        const amt = parseFloat(amount);
        if (!userId) {
            return res.status(400).json({ ok: false, error: 'User ID is required' });
        }
        if (!amt || isNaN(amt) || amt < 100) {
            return res.status(400).json({ ok: false, error: 'Minimum withdrawal amount is ₹100' });
        }

        const user = await User.findOne({ userId });
        if (!user) {
            return res.status(404).json({ ok: false, error: 'Astrologer not found' });
        }

        const curBal = Number(user.walletBalance || 0);
        if (curBal < amt) {
            return res.status(400).json({ ok: false, error: `Insufficient balance (Available: ₹${curBal.toFixed(2)})` });
        }

        // Deduct atomically
        const u = await User.findOneAndUpdate(
            { userId, walletBalance: { $gte: amt } },
            { $inc: { walletBalance: -amt } },
            { returnDocument: 'after' }
        );

        if (!u) {
            return res.status(400).json({ ok: false, error: 'Insufficient balance' });
        }

        const Withdrawal = require('../models/Withdrawal');
        const Notification = require('../models/Notification');
        const { sendFcmV1Push } = require('../services/push.service');

        let w;
        try {
            w = await Withdrawal.create({
                astroId: userId,
                amount: amt,
                status: 'pending',
                requestedAt: new Date()
            });
        } catch (dbErr) {
            console.error('[AstroController] Error creating withdrawal, rolling back wallet:', dbErr);
            await User.updateOne({ userId }, { $inc: { walletBalance: amt } });
            return res.status(500).json({ ok: false, error: 'Database error. Amount refunded.' });
        }

        const withdrawalId = (w._id || w.id || '').toString();

        // Persistent notification for Super Admin
        try {
            await Notification.create({
                targetRole: 'superadmin',
                type: 'withdrawal_request',
                title: '💰 புதிய பணம் எடுக்கும் கோரிக்கை',
                body: `${u.name || 'Astrologer'} ₹${amt} வித்ட்ரா செய்ய கோரியுள்ளார்.`,
                data: JSON.stringify({
                    withdrawalId,
                    astroId: u.userId,
                    astroName: u.name || 'Astrologer',
                    amount: amt,
                    phone: u.phone || '',
                    upiId: u.upiId || '',
                    upiNumber: u.upiNumber || '',
                    walletBalance: Number(u.walletBalance || 0),
                    time: new Date().toISOString()
                }),
                isRead: false,
                createdAt: new Date()
            });
        } catch (notifErr) {
            console.error('[AstroController] Error creating notification:', notifErr);
        }

        const io = req.app.get('io');
        const userSockets = req.app.get('userSockets');
        const notifPayload = {
            id: withdrawalId,
            type: 'withdrawal_request',
            title: '💰 புதிய பணம் எடுக்கும் கோரிக்கை',
            text: `💰 ${u.name || 'Astrologer'} ₹${amt} வித்ட்ரா செய்ய கோரியுள்ளார்.`,
            data: { withdrawalId, astroId: u.userId, astroName: u.name, amount: amt },
            time: new Date().toLocaleTimeString(),
            createdAt: new Date()
        };

        if (io) {
            io.to('superadmin').emit('admin-notification', notifPayload);
        }

        // Send FCM to Super Admins
        try {
            const admins = await User.find({ role: { $in: ['admin', 'superadmin'] } });
            for (const admin of admins) {
                if (io && userSockets) {
                    const sId = userSockets.get(admin.userId);
                    if (sId) io.to(sId).emit('admin-notification', notifPayload);
                }
                if (admin.fcmToken) {
                    sendFcmV1Push(
                        admin.fcmToken,
                        { type: 'withdrawal_request', withdrawalId, amount: String(amt), astroName: u.name || 'Astrologer' },
                        { title: '💰 புதிய பணம் எடுக்கும் கோரிக்கை', body: `${u.name || 'Astrologer'} ₹${amt} வித்ட்ரா செய்ய கோரியுள்ளார்.` },
                        admin.userId
                    ).catch(e => console.error('[AstroController] FCM error:', e.message));
                }
            }
        } catch (err) {
            console.error('[AstroController] FCM error:', err);
        }

        if (io && userSockets) {
            const sId = userSockets.get(userId);
            if (sId) io.to(sId).emit('wallet-update', { balance: Number(u.walletBalance || 0) });
        }

        res.json({ ok: true, balance: Number(u.walletBalance || 0), amount: amt, withdrawalId });
    } catch (e) {
        console.error('[AstroController] Error in requestWithdrawal:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};


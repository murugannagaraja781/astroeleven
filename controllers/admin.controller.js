const AcademyVideo = require('../models/AcademyVideo');
const Banner = require('../models/Banner');
const AccountDeletionRequest = require('../models/AccountDeletionRequest');
const User = require('../models/User');
const Withdrawal = require('../models/Withdrawal');
const Session = require('../models/Session');
const Feedback = require('../models/Feedback');
const GlobalSettings = require('../models/GlobalSettings');
const Payment = require('../models/Payment');
const RechargePackage = require('../models/RechargePackage');
const KundaliRecord = require('../models/KundaliRecord');
const performanceService = require('../services/performance.service');
const fs = require('fs');
const path = require('path');


exports.getVideos = async (req, res) => {
    const videos = await AcademyVideo.find().sort({ createdAt: -1 });
    res.json({ ok: true, videos });
};

exports.addVideo = async (req, res) => {
    try {
        const { title, youtubeUrl, thumbnail, category } = req.body;
        const video = await AcademyVideo.create({ title, youtubeUrl, thumbnail, category });
        res.json({ ok: true, video });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.deleteVideo = async (req, res) => {
    try {
        await AcademyVideo.findByIdAndDelete(req.params.id);
        res.json({ ok: true });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.getBanners = async (req, res) => {
    try {
        const banners = await Banner.find().sort({ order: 1 });
        res.json({ ok: true, banners });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
};

exports.addBanner = async (req, res) => {
    try {
        const { id, title, subtitle, ctaText, ctaButtonSize, order, offerPercentage, expiryDate, isActive, imageUrl, type, linkUrl } = req.body;
        let finalImageUrl = imageUrl;

        if (req.file) {
            finalImageUrl = 'uploads/' + req.file.filename;
        }

        let banner = null;
        if (id && id !== 'undefined' && id !== 'null') {
            banner = await Banner.findByIdAndUpdate(id, {
                title, subtitle, ctaText, ctaButtonSize,
                order: parseInt(order || 0),
                offerPercentage: parseFloat(offerPercentage || 0),
                expiryDate: expiryDate || null,
                isActive: isActive === 'true' || isActive === true,
                imageUrl: finalImageUrl,
                type: type || 'home_slider',
                linkUrl: linkUrl || ''
            }, { returnDocument: 'after' });
        }

        if (!banner) {
            banner = await Banner.create({
                title, subtitle, ctaText, ctaButtonSize,
                order: parseInt(order || 0),
                offerPercentage: parseFloat(offerPercentage || 0),
                expiryDate: expiryDate || null,
                isActive: isActive === 'true' || isActive === true,
                imageUrl: finalImageUrl,
                type: type || 'home_slider',
                linkUrl: linkUrl || ''
            });
        }

        const io = req.app.get('io');
        if (io) io.emit('banners-updated');

        return res.json({ ok: true, banner });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
};

exports.deleteBanner = async (req, res) => {
    try {
        await Banner.findByIdAndDelete(req.params.id);
        const io = req.app.get('io');
        if (io) io.emit('banners-updated');
        res.json({ ok: true });
    } catch (err) {
        res.status(500).json({ ok: false, error: err.message });
    }
};

exports.getDeletionRequests = async (req, res) => {
    const requests = await AccountDeletionRequest.find().sort({ requestedAt: -1 });
    res.json({ ok: true, requests });
};

exports.processDeletion = async (req, res) => {
    const { requestId, action, notes, adminId } = req.body;
    const request = await AccountDeletionRequest.findOne({ requestId });
    if (!request) return res.json({ ok: false, error: 'Request not found' });

    request.status = action; // approved/rejected
    request.notes = notes;
    request.processedAt = new Date();
    request.processedBy = adminId;
    await request.save();

    if (action === 'approved') {
        // Hard delete the user to release phone number/userId for re-registration
        if (request.userId) {
            await User.deleteOne({ userId: request.userId });
            console.log(`[Admin] Hard deleted user ${request.userId} as per approved deletion request.`);
        }
    }
    res.json({ ok: true });
};
exports.updateBalance = async (req, res) => {
    try {
        const { userId, amount, action } = req.body; // action: 'add' or 'subtract'
        if (!userId || amount === undefined) return res.json({ ok: false, error: 'User ID and amount required' });

        const user = await User.findOne({ userId });
        if (!user) return res.json({ ok: false, error: 'User not found' });

        if (action === 'subtract') {
            user.walletBalance = (user.walletBalance || 0) - parseFloat(amount);
        } else {
            user.walletBalance = (user.walletBalance || 0) + parseFloat(amount);
        }

        await user.save();
        res.json({ ok: true, balance: user.walletBalance });
    } catch (error) {
        res.json({ ok: false, error: error.message });
    }
};

exports.getPendingAstrologers = async (req, res) => {
    try {
        const list = await User.find({ role: 'astrologer', approvalStatus: 'pending' }).sort({ _id: -1 });
        res.json({ ok: true, list });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.approveAstrologer = async (req, res) => {
    try {
        const { userId, status } = req.body; // status: 'approved' or 'rejected'
        const user = await User.findOneAndUpdate({ userId }, { approvalStatus: status }, { new: true });
        if (!user) return res.json({ ok: false, error: 'User not found' });
        res.json({ ok: true, status: user.approvalStatus });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.getAstrologerPerformance = async (req, res) => {
    try {
        const { astrologerId } = req.params;
        const { days } = req.query;
        const stats = await performanceService.getAstrologerPerformance(astrologerId, parseInt(days) || 30);
        res.json(stats);
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.getAllAstrologersPerformance = async (req, res) => {
    try {
        const { days } = req.query;
        const stats = await performanceService.getAllAstrologersPerformance(parseInt(days) || 30);
        res.json(stats);
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.getConfig = async (req, res) => {
    try {
        const envPath = path.join(__dirname, '../.env');
        const content = fs.readFileSync(envPath, 'utf8');
        const lines = content.split(/\r?\n/);
        const SENSITIVE_KEYS = [
            'TURN_USERNAME', 'TURN_SERVER', 'TURN_PASSWORD', 'TURN_REALM', 
            'MONGO_URI', 'JWT_SECRET', 'FCM_SERVER_KEY'
        ];
        const config = {};

        lines.forEach(line => {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#')) {
                const [key, ...valueParts] = trimmed.split('=');
                const k = key.trim();
                if (k && !SENSITIVE_KEYS.includes(k)) {
                    let val = valueParts.join('=').trim();
                    if (val.startsWith('"') && val.endsWith('"')) {
                        val = val.substring(1, val.length - 1);
                    } else if (val.startsWith("'") && val.endsWith("'")) {
                        val = val.substring(1, val.length - 1);
                    }
                    config[k] = val;
                }
            }
        });
        res.json({ ok: true, config });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.updateConfig = async (req, res) => {
    try {
        const { config } = req.body;
        if (!config) return res.json({ ok: false, error: 'Config object required' });

        const envPath = path.join(__dirname, '../.env');
        let content = fs.readFileSync(envPath, 'utf8');
        const lines = content.split(/\r?\n/);

        const updatedLines = lines.map(line => {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#')) {
                const [key] = trimmed.split('=');
                const k = key.trim();
                if (k && config[k] !== undefined) {
                    process.env[k] = config[k];
                    let val = config[k];
                    if (val.includes(' ') && !val.startsWith('"')) {
                        val = `"${val}"`;
                    }
                    return `${k}=${val}`;
                }
            }
            return line;
        });

        // Add new keys if not present
        const existingKeys = lines.map(line => {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#')) {
                return trimmed.split('=')[0].trim();
            }
            return null;
        }).filter(Boolean);

        Object.keys(config).forEach(key => {
            process.env[key] = config[key]; // Update current process env as well
            if (!existingKeys.includes(key)) {
                let val = config[key];
                if (val.includes(' ') && !val.startsWith('"')) {
                    val = `"${val}"`;
                }
                updatedLines.push(`${key}=${val}`);
            }
        });

        fs.writeFileSync(envPath, updatedLines.join('\n'), 'utf8');
        res.json({ ok: true });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.getSmtpConfig = async (req, res) => {
    try {
        const envPath = path.join(__dirname, '../.env');
        let content = '';
        if (fs.existsSync(envPath)) {
            content = fs.readFileSync(envPath, 'utf8');
        }
        const config = {
            SMTP_HOST: process.env.SMTP_HOST || 'smtp.gmail.com',
            SMTP_PORT: process.env.SMTP_PORT || '587',
            SMTP_SECURE: process.env.SMTP_SECURE || 'false',
            SMTP_USER: process.env.SMTP_USER || '',
            SMTP_PASS: process.env.SMTP_PASS || '',
            EMAIL_FROM: process.env.EMAIL_FROM || '',
            EMAIL_TO: process.env.EMAIL_TO || ''
        };
        const lines = content.split(/\r?\n/);
        lines.forEach(line => {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#')) {
                const [key, ...valueParts] = trimmed.split('=');
                if (key) {
                    const k = key.trim();
                    if (config.hasOwnProperty(k)) {
                        let val = valueParts.join('=').trim();
                        if (val.startsWith('"') && val.endsWith('"')) {
                            val = val.substring(1, val.length - 1);
                        } else if (val.startsWith("'") && val.endsWith("'")) {
                            val = val.substring(1, val.length - 1);
                        }
                        config[k] = val;
                    }
                }
            }
        });
        res.json({ ok: true, config });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.updateSmtpConfig = async (req, res) => {
    try {
        const { config } = req.body;
        if (!config) return res.json({ ok: false, error: 'Config object required' });

        const envPath = path.join(__dirname, '../.env');
        let content = '';
        if (fs.existsSync(envPath)) {
            content = fs.readFileSync(envPath, 'utf8');
        }
        const lines = content.split(/\r?\n/);

        const allowedKeys = ['SMTP_HOST', 'SMTP_PORT', 'SMTP_SECURE', 'SMTP_USER', 'SMTP_PASS', 'EMAIL_FROM', 'EMAIL_TO'];
        const updatedKeys = new Set();

        const updatedLines = lines.map(line => {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#')) {
                const [key] = trimmed.split('=');
                const k = key.trim();
                if (k && allowedKeys.includes(k) && config[k] !== undefined) {
                    updatedKeys.add(k);
                    process.env[k] = config[k];
                    let val = config[k];
                    if (val.includes(' ') && !val.startsWith('"')) {
                        val = `"${val}"`;
                    }
                    return `${k}=${val}`;
                }
            }
            return line;
        });

        allowedKeys.forEach(k => {
            if (config[k] !== undefined && !updatedKeys.has(k)) {
                process.env[k] = config[k];
                let val = config[k];
                if (val.includes(' ') && !val.startsWith('"')) {
                    val = `"${val}"`;
                }
                updatedLines.push(`${k}=${val}`);
            }
        });

        fs.writeFileSync(envPath, updatedLines.join('\n'), 'utf8');
        res.json({ ok: true });
    } catch (e) {
        res.json({ ok: false, error: e.message });
    }
};

exports.getUsers = async (req, res) => {
    try {
        const { role, page = 1, limit = 10, search = '', city = '', dob = '', joined = 'all', exportAll = false } = req.query;
        const parsedPage = Math.max(1, parseInt(page) || 1);
        const parsedLimit = Math.max(1, parseInt(limit) || 10);

        const query = {};
        if (role) {
            query.role = role;
        }

        const andConditions = [];

        // Recently Joined filter (1day, 3days, 5days, 1week, 1month)
        if (joined && joined !== 'all') {
            const now = Date.now();
            const dayMs = 24 * 60 * 60 * 1000;
            let daysAgo = 0;
            if (joined === '1day' || joined === 'today' || joined === '1') daysAgo = 1;
            else if (joined === '3days' || joined === '3') daysAgo = 3;
            else if (joined === '5days' || joined === '5') daysAgo = 5;
            else if (joined === '1week' || joined === '7days' || joined === '7') daysAgo = 7;
            else if (joined === '1month' || joined === '30days' || joined === '30') daysAgo = 30;

            if (daysAgo > 0) {
                const sinceDate = new Date(now - daysAgo * dayMs);
                andConditions.push({ created_at: { $gte: sinceDate } });
            }
        }

        if (search) {
            const lowerSearch = search.trim().toLowerCase();
            if (lowerSearch === 'online') {
                query.isOnline = true;
            } else if (lowerSearch === 'offline') {
                query.isOnline = false;
            } else if (lowerSearch === 'approved') {
                query.approvalStatus = 'approved';
            } else if (lowerSearch === 'pending') {
                query.approvalStatus = 'pending';
            } else {
                const searchRegex = new RegExp(search.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
                andConditions.push({
                    $or: [
                        { name: searchRegex },
                        { phone: searchRegex },
                        { userId: searchRegex },
                        { email: searchRegex }
                    ]
                });
            }
        }

        if (city) {
            const cityRegex = new RegExp(city.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
            andConditions.push({
                $or: [
                    { pob: cityRegex },
                    { address: cityRegex },
                    { 'birthDetails.pob': cityRegex }
                ]
            });
        }

        if (dob) {
            const dobRegex = new RegExp(dob.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
            andConditions.push({
                $or: [
                    { dob: dobRegex },
                    { 'birthDetails.dob': dobRegex }
                ]
            });
        }

        if (andConditions.length > 0) {
            query.$and = andConditions;
        }

        const isExport = exportAll === 'true' || exportAll === true;
        const usersQuery = isExport
            ? User.find(query).sort({ _id: -1 }).limit(5000)
            : User.find(query).sort({ _id: -1 }).skip((parsedPage - 1) * parsedLimit).limit(parsedLimit);

        const [total, users] = await Promise.all([
            User.countDocuments(query),
            usersQuery
        ]);

        res.json({
            ok: true,
            users,
            total,
            page: parsedPage,
            pages: Math.ceil(total / parsedLimit)
        });
    } catch (e) {
        console.error('[AdminController] Error in getUsers:', e);
        res.status(500).json({ ok: false, error: 'Failed to fetch users' });
    }
};

exports.getWithdrawals = async (req, res) => {
    try {
        const list = await Withdrawal.find().sort({ requestedAt: -1 }).limit(100);
        const enriched = [];
        for (const w of list) {
            const u = await User.findOne({ userId: w.astroId });
            enriched.push({
                ...(w.toObject ? w.toObject() : w),
                astroName: u ? u.name : 'Unknown Astro',
                bankingDetails: u ? {
                    bankName: 'Details Below',
                    accountNumber: u.bankDetails || 'N/A',
                    accountHolderName: u.realName || u.name,
                    ifscCode: '-',
                    upiId: `${u.upiId || ''} ${u.upiNumber ? '(' + u.upiNumber + ')' : ''}`
                } : null
            });
        }
        res.json({ ok: true, list: enriched });
    } catch (e) {
        console.error('[AdminController] Error in getWithdrawals:', e);
        res.status(500).json({ ok: false, error: 'Failed to fetch withdrawals' });
    }
};

exports.getSessionsHistory = async (req, res) => {
    try {
        const { page = 1, limit = 10, search = '', type = '' } = req.query;
        const parsedPage = Math.max(1, parseInt(page) || 1);
        const parsedLimit = Math.max(1, parseInt(limit) || 10);

        const query = { status: 'ended' };
        if (type) query.type = type;

        if (search) {
            const searchRegex = new RegExp(search.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
            const matchingUsers = await User.find({ name: searchRegex }).select('userId').lean();
            const matchingUserIds = matchingUsers.map(u => u.userId);
            query.$or = [
                { clientId: { $in: matchingUserIds } },
                { fromUserId: { $in: matchingUserIds } },
                { astrologerId: { $in: matchingUserIds } },
                { toUserId: { $in: matchingUserIds } }
            ];
        }

        const total = await Session.countDocuments(query);
        const sessions = await Session.find(query)
            .sort({ id: -1 })
            .skip((parsedPage - 1) * parsedLimit)
            .limit(parsedLimit)
            .lean();

        const populated = await Promise.all(sessions.map(async (s) => {
            const cId = s.clientId || s.fromUserId;
            const aId = s.astrologerId || s.toUserId;
            const [client, astro] = await Promise.all([
                User.findOne({ userId: cId }).select('name').lean(),
                User.findOne({ userId: aId }).select('name').lean()
            ]);

            let calculatedDuration = Number(s.duration) || 0;
            if (calculatedDuration >= 1000) {
                calculatedDuration = Math.round(calculatedDuration / 1000);
            } else if (!calculatedDuration && s.actualBillingStart && s.endTime && s.endTime > s.actualBillingStart) {
                calculatedDuration = Math.round((s.endTime - s.actualBillingStart) / 1000);
            } else if (!calculatedDuration && s.startTime && s.endTime && s.endTime > s.startTime) {
                calculatedDuration = Math.round((s.endTime - s.startTime) / 1000);
            }
            calculatedDuration = Math.max(0, calculatedDuration || 0);

            return {
                ...s,
                clientName: client ? client.name : 'Unknown Client',
                astrologerName: astro ? astro.name : 'Unknown Astrologer',
                duration: calculatedDuration
            };
        }));

        res.json({
            ok: true,
            sessions: populated,
            total,
            page: parsedPage,
            pages: Math.ceil(total / parsedLimit)
        });
    } catch (e) {
        console.error('[AdminController] Error in getSessionsHistory:', e);
        res.status(500).json({ ok: false, error: 'Failed to fetch sessions history' });
    }
};

exports.getFeedback = async (req, res) => {
    try {
        const feedback = await Feedback.find().sort({ _id: -1 }).limit(100);
        res.json({ ok: true, feedback });
    } catch (e) {
        console.error('[AdminController] Error in getFeedback:', e);
        res.status(500).json({ ok: false, error: 'Failed to fetch feedback' });
    }
};

exports.getSlabRates = async (req, res) => {
    try {
        const slabSetting = await GlobalSettings.findOne({ key: 'slab_rates' });
        const defaultRates = { 1: 0.50, 2: 0.60, 3: 0.70, 4: 0.80 };
        res.json({ ok: true, rates: slabSetting?.value || defaultRates });
    } catch (e) {
        console.error('[AdminController] Error in getSlabRates:', e);
        res.status(500).json({ ok: false, error: 'Failed to fetch slab rates' });
    }
};

exports.getPayments = async (req, res) => {
    try {
        const {
            page = 1,
            limit = 20,
            status = 'all',
            dateRange = 'all',
            startDate,
            endDate,
            search = ''
        } = req.query;

        const parsedPage = Math.max(1, parseInt(page) || 1);
        const parsedLimit = Math.max(1, parseInt(limit) || 20);

        const query = {};
        const andConditions = [];

        // Status Filter
        if (status && status !== 'all') {
            andConditions.push({ status: status.toLowerCase() });
        }

        // Date Filter
        const now = new Date();
        if (dateRange === 'today') {
            const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
            andConditions.push({ createdAt: { $gte: startOfDay } });
        } else if (dateRange === 'yesterday') {
            const startOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1, 0, 0, 0);
            const endOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1, 23, 59, 59, 999);
            andConditions.push({ createdAt: { $gte: startOfYesterday, $lte: endOfYesterday } });
        } else if (dateRange === '7days') {
            const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
            andConditions.push({ createdAt: { $gte: sevenDaysAgo } });
        } else if (dateRange === '30days') {
            const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
            andConditions.push({ createdAt: { $gte: thirtyDaysAgo } });
        } else if (dateRange === 'custom' && (startDate || endDate)) {
            const dateCond = {};
            if (startDate) dateCond.$gte = new Date(startDate + 'T00:00:00');
            if (endDate) dateCond.$lte = new Date(endDate + 'T23:59:59.999');
            andConditions.push({ createdAt: dateCond });
        }

        // Search Filter (TransactionId, ProviderRefId, UserId, or User Name/Phone)
        if (search && search.trim()) {
            const term = search.trim();
            const searchRegex = new RegExp(term.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
            const matchingUsers = await User.find({
                $or: [
                    { name: searchRegex },
                    { phone: searchRegex },
                    { userId: term }
                ]
            }).select('userId').limit(100).lean();

            const userIds = matchingUsers.map(u => u.userId);
            if (userIds.length > 0) {
                andConditions.push({
                    $or: [
                        { transactionId: { $regex: term } },
                        { merchantTransactionId: { $regex: term } },
                        { providerRefId: { $regex: term } },
                        { userId: { $in: userIds } }
                    ]
                });
            } else {
                andConditions.push({
                    $or: [
                        { transactionId: { $regex: term } },
                        { merchantTransactionId: { $regex: term } },
                        { providerRefId: { $regex: term } },
                        { userId: term }
                    ]
                });
            }
        }

        if (andConditions.length > 0) {
            query.$and = andConditions;
        }

        const total = await Payment.countDocuments(query);
        const payments = await Payment.find(query)
            .sort({ id: -1 })
            .skip((parsedPage - 1) * parsedLimit)
            .limit(parsedLimit)
            .lean();

        // Populate user details (name, phone, role)
        const uniqueUserIds = [...new Set(payments.map(p => p.userId).filter(Boolean))];
        const userMap = {};
        if (uniqueUserIds.length > 0) {
            const users = await User.find({ userId: { $in: uniqueUserIds } })
                .select('userId name phone role walletBalance')
                .lean();
            users.forEach(u => { userMap[u.userId] = u; });
        }

        const populatedPayments = payments.map(p => {
            const u = userMap[p.userId] || {};

            let statusReason = p.failureReason || '';
            if (!statusReason) {
                if (p.status === 'success') {
                    statusReason = 'Payment Completed & Wallet Credited';
                } else if (p.status === 'failed') {
                    statusReason = 'Order Expired / User Cancelled';
                } else if (p.status === 'pending') {
                    const createdTime = new Date(p.createdAt || p.created_at || Date.now()).getTime();
                    const ageMins = Math.round((Date.now() - createdTime) / (60 * 1000));
                    if (ageMins <= 20) {
                        statusReason = `Awaiting customer payment (${ageMins}m ago)`;
                    } else {
                        statusReason = 'Payment timed out (Customer did not complete UPI pay)';
                    }
                }
            }

            return {
                ...p,
                userName: u.name || p.userName || 'Guest User',
                userPhone: u.phone || p.userPhone || 'N/A',
                userRole: u.role || 'client',
                userBalance: u.walletBalance || 0,
                statusReason
            };
        });

        // Compute summary metrics (Total Success Collections, Today's Collections, Counts)
        const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);

        const [allSuccessTotal, todaySuccessTotal, totalSuccessCount, totalPendingCount] = await Promise.all([
            Payment.aggregate([{ $match: { status: 'success' } }]),
            Payment.aggregate([{ $match: { status: 'success', createdAt: { $gte: todayStart } } }]),
            Payment.countDocuments({ status: 'success' }),
            Payment.countDocuments({ status: 'pending' })
        ]);

        const totalVolume = allSuccessTotal[0]?.totalCollected || 0;
        const todayVolume = todaySuccessTotal[0]?.totalCollected || 0;

        res.json({
            ok: true,
            payments: populatedPayments,
            total,
            page: parsedPage,
            pages: Math.ceil(total / parsedLimit),
            summary: {
                totalVolume: Math.round(totalVolume * 100) / 100,
                todayVolume: Math.round(todayVolume * 100) / 100,
                successCount: totalSuccessCount,
                pendingCount: totalPendingCount
            }
        });
    } catch (e) {
        console.error('[AdminController] Error in getPayments:', e);
        res.status(500).json({ ok: false, error: 'Failed to fetch payments: ' + e.message });
    }
};

exports.verifyPayment = async (req, res) => {
    try {
        const { transactionId } = req.body;
        if (!transactionId) return res.status(400).json({ ok: false, error: 'transactionId is required' });

        const io = req.app.get('io');
        const { checkPhonePeOrderStatus, processPaymentSuccess } = require('./payment.controller');

        const statusRes = await checkPhonePeOrderStatus(transactionId);
        if (!statusRes.ok) {
            return res.json({ ok: false, error: statusRes.error || 'Failed to contact PhonePe' });
        }

        const phonepeData = statusRes.data;
        const isSuccess = (phonepeData.state === 'COMPLETED' || phonepeData.state === 'SUCCESS' || phonepeData.code === 'PAYMENT_SUCCESS');
        const providerRefId = phonepeData.orderId || phonepeData.providerReferenceId || '';

        if (isSuccess) {
            const processRes = await processPaymentSuccess(transactionId, providerRefId, io);
            await Payment.updateOne(
                { transactionId },
                { $set: { failureReason: 'Verified with PhonePe (Success)' } }
            );
            return res.json({
                ok: true,
                status: 'success',
                message: 'Payment verified and wallet credited successfully!',
                reason: 'Completed & Credited',
                phonepeData,
                credited: processRes.success
            });
        } else {
            let reasonText = phonepeData.detailedErrorCode || phonepeData.errorCode || phonepeData.message || 'Payment not completed';
            if (phonepeData.detailedErrorCode === 'ORDER_EXPIRED') {
                reasonText = 'Order Expired (Customer did not enter UPI PIN / cancelled)';
            } else if (phonepeData.detailedErrorCode === 'TXN_AUTO_FAILED') {
                reasonText = 'Auto-cancelled / Declined in UPI App';
            } else if (phonepeData.state === 'PENDING') {
                reasonText = 'Awaiting payment (Customer has not entered UPI PIN yet)';
            }

            if (phonepeData.state === 'FAILED') {
                await Payment.updateOne(
                    { transactionId },
                    { $set: { status: 'failed', failureReason: reasonText } }
                );
            }
            return res.json({
                ok: true,
                status: phonepeData.state === 'FAILED' ? 'failed' : (phonepeData.state || 'pending'),
                message: `PhonePe Status: ${phonepeData.state || 'PENDING'} - ${reasonText}`,
                reason: reasonText,
                phonepeData
            });
        }
    } catch (e) {
        console.error('[AdminController] Error in verifyPayment:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

// ==========================================
// RECHARGE PACKAGES MANAGEMENT (ADMIN)
// ==========================================

const DEFAULT_ADMIN_PACKAGES = [
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

async function ensureAdminPackagesSeeded() {
    try {
        const count = await RechargePackage.countDocuments({});
        if (count === 0) {
            for (const p of DEFAULT_ADMIN_PACKAGES) {
                await RechargePackage.create(p);
            }
        }
    } catch (e) {
        console.error('[RechargePackage] Seed error in admin:', e.message);
    }
}

exports.getRechargePackages = async (req, res) => {
    try {
        await ensureAdminPackagesSeeded();
        const packages = await RechargePackage.find({}).sort({ order: 1, amount: 1 }).lean();
        res.json({ ok: true, packages });
    } catch (e) {
        console.error('[AdminController] Error in getRechargePackages:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.addRechargePackage = async (req, res) => {
    try {
        const { amount, extraPercentage, badge, description, isActive, order } = req.body;
        const parsedAmount = parseFloat(amount);
        if (!parsedAmount || parsedAmount <= 0) {
            return res.status(400).json({ ok: false, error: 'Valid recharge amount is required' });
        }

        const packageId = 'pack_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
        const pkg = await RechargePackage.create({
            packageId,
            amount: parsedAmount,
            extraPercentage: parseFloat(extraPercentage) || 0,
            badge: (badge || '').trim(),
            description: (description || '').trim(),
            isActive: isActive === true || isActive === 'true' || isActive === undefined,
            order: parseInt(order) || 0,
            createdAt: new Date(),
            updatedAt: new Date()
        });

        res.json({ ok: true, package: pkg });
    } catch (e) {
        console.error('[AdminController] Error in addRechargePackage:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.updateRechargePackage = async (req, res) => {
    try {
        const { id } = req.params;
        const { amount, extraPercentage, badge, description, isActive, order } = req.body;

        const updateData = { updatedAt: new Date() };
        if (amount !== undefined) updateData.amount = parseFloat(amount);
        if (extraPercentage !== undefined) updateData.extraPercentage = parseFloat(extraPercentage);
        if (badge !== undefined) updateData.badge = String(badge).trim();
        if (description !== undefined) updateData.description = String(description).trim();
        if (isActive !== undefined) updateData.isActive = isActive === true || isActive === 'true';
        if (order !== undefined) updateData.order = parseInt(order);

        const query = (id.startsWith('pack_')) ? { packageId: id } : { _id: id };
        await RechargePackage.updateOne(query, { $set: updateData });
        const updated = await RechargePackage.findOne(query);

        res.json({ ok: true, package: updated });
    } catch (e) {
        console.error('[AdminController] Error in updateRechargePackage:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.deleteRechargePackage = async (req, res) => {
    try {
        const { id } = req.params;
        const query = (id.startsWith('pack_')) ? { packageId: id } : { _id: id };
        await RechargePackage.deleteOne(query);
        res.json({ ok: true });
    } catch (e) {
        console.error('[AdminController] Error in deleteRechargePackage:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.toggleRechargePackage = async (req, res) => {
    try {
        const { id } = req.params;
        const query = (id.startsWith('pack_')) ? { packageId: id } : { _id: id };
        const pkg = await RechargePackage.findOne(query);
        if (!pkg) return res.status(404).json({ ok: false, error: 'Package not found' });

        pkg.isActive = !pkg.isActive;
        pkg.updatedAt = new Date();
        await pkg.save();

        res.json({ ok: true, isActive: pkg.isActive });
    } catch (e) {
        console.error('[AdminController] Error in toggleRechargePackage:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

// ==========================================
// KUNDALI DETAILS & CONSULTATION HOROSCOPE RECORDS (ADMIN)
// ==========================================

let kundaliBackfillDone = false;
async function ensureKundaliBackfill() {
    if (kundaliBackfillDone) return;
    kundaliBackfillDone = true;
    try {
        const count = await KundaliRecord.countDocuments({});
        if (count === 0) {
            const users = await User.find({
                role: 'client'
            }).limit(3000).lean();

            for (const u of users) {
                const bd = u.birthDetails || {};
                const id = u.intakeDetails || {};
                const dobVal = u.dob || bd.dob;
                if (!dobVal && !u.pob && !bd.pob) continue;

                let bYear = null, bMonth = null, bDay = null;
                if (typeof dobVal === 'string') {
                    if (dobVal.includes('-')) {
                        const parts = dobVal.split('-').map(Number);
                        if (parts.length >= 3) {
                            bYear = parts[0];
                            bMonth = parts[1];
                            bDay = parts[2];
                        }
                    } else if (dobVal.includes('/')) {
                        const parts = dobVal.split('/').map(Number);
                        if (parts.length >= 3) {
                            bDay = parts[0];
                            bMonth = parts[1];
                            bYear = parts[2];
                        }
                    }
                }

                const tobStr = u.tob || bd.tob || '12:00';
                let tobFormatted = tobStr;
                if (tobStr && tobStr.includes(':')) {
                    const [h, m] = tobStr.split(':').map(Number);
                    if (!isNaN(h)) {
                        const ampm = h >= 12 ? 'PM' : 'AM';
                        const h12 = h % 12 || 12;
                        tobFormatted = `${String(h12).padStart(2, '0')}:${String(m || 0).padStart(2, '0')} ${ampm}`;
                    }
                }

                await KundaliRecord.create({
                    recordId: 'kr_' + (u.userId || Date.now() + Math.random().toString(36).substring(2, 6)),
                    clientId: u.userId || '',
                    clientName: u.name || 'Client',
                    clientPhone: u.phone || '',
                    dob: dobVal || '',
                    birthDay: bDay,
                    birthMonth: bMonth,
                    birthYear: bYear,
                    tob: tobStr,
                    tobFormatted,
                    pob: u.pob || bd.pob || '',
                    latitude: bd.lat || null,
                    longitude: bd.lon || null,
                    gender: id.gender || u.gender || '',
                    marital: id.marital || '',
                    occupation: id.occupation || u.profession || '',
                    topic: id.topic || 'General Consultation',
                    astrologerId: '',
                    astrologerName: 'Platform Intake',
                    consultationType: 'profile',
                    sessionId: '',
                    consultationDate: u.createdAt || new Date(),
                    createdAt: u.createdAt || new Date()
                });
            }
            console.log('[KundaliRecord] Initial client backfill complete');
        }
    } catch (e) {
        console.error('[KundaliRecord] Backfill error:', e.message);
    }
}

function buildKundaliFilters(queryObj) {
    const {
        search = '',
        dateRange = 'all',
        startDate,
        endDate,
        birthDay,
        birthMonth,
        birthYear,
        pob = ''
    } = queryObj;

    const query = {};
    const andConditions = [];

    // 1. Search (Client Name, Phone, District/City)
    if (search && search.trim()) {
        const term = search.trim();
        const searchRegex = new RegExp(term.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
        andConditions.push({
            $or: [
                { clientName: searchRegex },
                { clientPhone: searchRegex },
                { pob: searchRegex },
                { topic: searchRegex }
            ]
        });
    }

    // 2. Specific POB / District Filter
    if (pob && pob.trim()) {
        const pobRegex = new RegExp(pob.trim().replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'), 'i');
        andConditions.push({ pob: pobRegex });
    }

    // 3. Consultation / Recorded Date Range (Today, Yesterday, Last Week, Last Month, Custom)
    const now = new Date();
    if (dateRange === 'today') {
        const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        andConditions.push({ consultationDate: { $gte: startOfDay } });
    } else if (dateRange === 'yesterday') {
        const startOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1, 0, 0, 0);
        const endOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1, 23, 59, 59, 999);
        andConditions.push({ consultationDate: { $gte: startOfYesterday, $lte: endOfYesterday } });
    } else if (dateRange === '7days') {
        const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
        andConditions.push({ consultationDate: { $gte: sevenDaysAgo } });
    } else if (dateRange === '30days') {
        const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
        andConditions.push({ consultationDate: { $gte: thirtyDaysAgo } });
    } else if (dateRange === 'custom' && (startDate || endDate)) {
        const dateCond = {};
        if (startDate) dateCond.$gte = new Date(startDate + 'T00:00:00');
        if (endDate) dateCond.$lte = new Date(endDate + 'T23:59:59.999');
        andConditions.push({ consultationDate: dateCond });
    }

    // 4. Birth Date Filters (Specific Day, Month, Year e.g. 13th, 7th month, 1998)
    if (birthDay && parseInt(birthDay) > 0) {
        andConditions.push({ birthDay: parseInt(birthDay) });
    }
    if (birthMonth && parseInt(birthMonth) > 0) {
        andConditions.push({ birthMonth: parseInt(birthMonth) });
    }
    if (birthYear && parseInt(birthYear) > 1900) {
        andConditions.push({ birthYear: parseInt(birthYear) });
    }

    if (andConditions.length > 0) {
        query.$and = andConditions;
    }

    return query;
}

exports.getKundaliRecords = async (req, res) => {
    try {
        await ensureKundaliBackfill();

        const { page = 1, limit = 20 } = req.query;
        const parsedPage = Math.max(1, parseInt(page) || 1);
        const parsedLimit = Math.max(1, parseInt(limit) || 20);

        const query = buildKundaliFilters(req.query);

        const total = await KundaliRecord.countDocuments(query);
        const records = await KundaliRecord.find(query)
            .sort({ id: -1 })
            .skip((parsedPage - 1) * parsedLimit)
            .limit(parsedLimit)
            .lean();

        // Compute summary counts
        const now = new Date();
        const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        const [todayCount, allTotalCount] = await Promise.all([
            KundaliRecord.countDocuments({ consultationDate: { $gte: startOfToday } }),
            KundaliRecord.countDocuments({})
        ]);

        res.json({
            ok: true,
            records,
            total,
            page: parsedPage,
            pages: Math.ceil(total / parsedLimit),
            summary: {
                totalCount: allTotalCount,
                filteredCount: total,
                todayCount
            }
        });
    } catch (e) {
        console.error('[AdminController] Error in getKundaliRecords:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.exportKundaliRecords = async (req, res) => {
    try {
        await ensureKundaliBackfill();

        const query = buildKundaliFilters(req.query);
        const records = await KundaliRecord.find(query)
            .sort({ id: -1 })
            .limit(100000)
            .lean();

        // Build CSV with UTF-8 BOM so Excel opens Tamil characters cleanly
        const csvRows = [];
        csvRows.push([
            'S.No',
            'Client Name',
            'Mobile Number',
            'Date of Birth',
            'Birth Day',
            'Birth Month',
            'Birth Year',
            'Time of Birth',
            'Birth District / Place',
            'Gender',
            'Marital Status',
            'Topic',
            'Astrologer',
            'Consultation Type',
            'Consultation Date'
        ].map(col => `"${col.replace(/"/g, '""')}"`).join(','));

        records.forEach((r, idx) => {
            const cDate = r.consultationDate ? new Date(r.consultationDate).toISOString().slice(0, 10) : '';
            csvRows.push([
                idx + 1,
                r.clientName || 'Client',
                r.clientPhone || 'N/A',
                r.dob || '',
                r.birthDay || '',
                r.birthMonth || '',
                r.birthYear || '',
                r.tobFormatted || r.tob || '',
                r.pob || '',
                r.gender || '',
                r.marital || '',
                r.topic || '',
                r.astrologerName || '',
                r.consultationType || '',
                cDate
            ].map(val => `"${String(val ?? '').replace(/"/g, '""')}"`).join(','));
        });

        const csvContent = '\uFEFF' + csvRows.join('\r\n');
        const filename = `kundali_records_${new Date().toISOString().slice(0, 10)}.csv`;

        res.setHeader('Content-Type', 'text/csv; charset=utf-8');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
        res.status(200).send(csvContent);
    } catch (e) {
        console.error('[AdminController] Error in exportKundaliRecords:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.deleteKundaliRecord = async (req, res) => {
    try {
        const { id } = req.params;
        const query = (id.startsWith('kr_')) ? { recordId: id } : { _id: id };
        await KundaliRecord.deleteOne(query);
        res.json({ ok: true });
    } catch (e) {
        console.error('[AdminController] Error in deleteKundaliRecord:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};



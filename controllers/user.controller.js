const User = require('../models/User');
const ChatMessage = require('../models/ChatMessage');
const Session = require('../models/Session');
const { formatImageUrl } = require('../utils/formatImage');

exports.getProfile = async (req, res) => {
    try {
        const user = await User.findOne({ userId: req.params.userId });
        if (!user) return res.status(404).json({ ok: false, error: 'User not found' });
        
        const SERVER_URL = req.app.get('SERVER_URL');
        const userObj = user.toObject ? user.toObject() : { ...user };
        delete userObj.model;
        delete userObj._doc;

        res.json({ 
            ok: true, 
            ...userObj, 
            image: formatImageUrl(user.image, user.name, SERVER_URL)
        });
    } catch (e) {
        res.status(500).json({ ok: false });
    }
};

exports.getChatHistory = async (req, res) => {
    try {
        const { sessionId } = req.params;
        const limit = parseInt(req.query.limit) || 200;
        let messages = await ChatMessage.find({ sessionId }).sort({ timestamp: 1 }).limit(limit).lean();

        // Fallback: If no messages found by exact sessionId, try matching session participants
        if ((!messages || messages.length === 0) && sessionId) {
            const sess = await Session.findOne({ $or: [{ sessionId }, { _id: sessionId }] }).lean();
            if (sess) {
                const u1 = sess.clientId || sess.fromUserId;
                const u2 = sess.astrologerId || sess.toUserId;
                if (u1 && u2) {
                    const sTime = sess.startTime || sess.actualBillingStart || 0;
                    const eTime = sess.endTime || sess.sessionEndAt || (Date.now() + 86400000);
                    messages = await ChatMessage.find({
                        $or: [
                            { fromUserId: u1, toUserId: u2 },
                            { fromUserId: u2, toUserId: u1 }
                        ],
                        ...(sTime > 0 ? { timestamp: { $gte: sTime - 300000, $lte: eTime + 300000 } } : {})
                    }).sort({ timestamp: 1 }).limit(limit).lean();
                }
            }
        }

        const formatted = messages.map(m => {
            const textVal = m.text || (m.content && typeof m.content === 'object' ? m.content.text : '') || '';
            const typeVal = m.type || (m.content && typeof m.content === 'object' ? m.content.type : '') || 'text';
            const fileUrlVal = m.fileUrl || (m.content && typeof m.content === 'object' ? m.content.fileUrl : '') || '';
            return {
                messageId: m.messageId,
                fromUserId: m.fromUserId,
                toUserId: m.toUserId,
                sessionId: m.sessionId || sessionId,
                timestamp: m.timestamp || (m.createdAt ? new Date(m.createdAt).getTime() : Date.now()),
                text: textVal,
                type: typeVal,
                fileUrl: fileUrlVal,
                fileName: m.fileName || '',
                status: m.status || 'delivered',
                content: {
                    text: textVal,
                    type: typeVal,
                    fileUrl: fileUrlVal
                }
            };
        });
        res.json({ ok: true, messages: formatted });
    } catch (e) {
        console.error('getChatHistory error:', e);
        res.status(500).json({ ok: false, error: e.message });
    }
};

exports.applyReferral = async (req, res) => {
    try {
        const { userId, referralCode } = req.body;
        if (!userId || !referralCode) return res.json({ ok: false, error: 'Missing required data' });

        const user = await User.findOne({ userId });
        if (!user) return res.json({ ok: false, error: 'User not found' });
        if (user.referredBy) return res.json({ ok: false, error: 'Referral already applied' });

        const referrer = await User.findOne({ referralCode: referralCode.trim().toUpperCase() });
        if (!referrer) return res.json({ ok: false, error: 'Invalid referral code' });
        if (referrer.userId === userId) return res.json({ ok: false, error: 'Cannot refer yourself' });

        // Rule 3: Add ₹80 difference payout (188 - 108)
        user.walletBalance = (user.walletBalance || 0) + 80;
        user.referredBy = referrer.userId;
        await user.save();

        res.json({ ok: true, message: 'Referral code applied! ₹80 bonus added.', walletBalance: user.walletBalance });
    } catch (e) {
        console.error(e);
        res.status(500).json({ ok: false, error: 'Server error' });
    }
};

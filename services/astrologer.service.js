const User = require('../models/User');
const { formatImageUrl } = require('../utils/formatImage');

function extractSpecialties(skills, profession) {
    let list = [];
    if (Array.isArray(skills)) {
        list = [...skills];
    } else if (typeof skills === 'string' && skills.trim()) {
        try {
            const parsed = JSON.parse(skills);
            if (Array.isArray(parsed)) list = parsed;
            else list = skills.split(',');
        } catch {
            list = skills.split(',');
        }
    }

    // Also extract tags mentioned in profession/bio
    if (profession && typeof profession === 'string') {
        const parts = profession.split(/[,/•|;]+|\band\b/i).map(s => s.trim()).filter(Boolean);
        for (const p of parts) {
            if (!list.some(existing => existing.toLowerCase() === p.toLowerCase())) {
                list.push(p);
            }
        }
    }

    const cleanList = list.map(s => s.trim()).filter(Boolean);

    // If astrologer has Vedic or is empty, include primary consultation areas
    const isVedicOrGeneral = cleanList.length === 0 || cleanList.some(s => /vedic|வேதிக்|jathagam|ஜாதகம்/i.test(s));
    if (isVedicOrGeneral) {
        const standardTags = ['Vedic', 'Love', 'Career', 'Marriage', 'Business', 'Finance'];
        for (const tag of standardTags) {
            if (!cleanList.some(existing => existing.toLowerCase() === tag.toLowerCase())) {
                cleanList.push(tag);
            }
        }
    }

    return cleanList;
}

async function getFormattedAstrologers(SERVER_URL) {
    try {
        const astros = await User.find({ role: 'astrologer', approvalStatus: 'approved' })
            .select('userId name phone skills profession price chatPrice callPrice videoPrice isOnline isChatOnline isAudioOnline isVideoOnline experience isVerified image walletBalance totalEarnings isBusy languages orderCount isDocumentVerified')
            .lean();

        return astros.map(a => ({
            userId: a.userId,
            name: a.name,
            skills: extractSpecialties(a.skills, a.profession),
            profession: a.profession || '',
            price: a.price || 15,
            chatPrice: a.chatPrice || a.price || 15,
            callPrice: a.callPrice || a.price || 15,
            videoPrice: a.videoPrice || a.price || 20,
            isOnline: a.isOnline || false,
            isChatOnline: a.isChatOnline || false,
            isAudioOnline: a.isAudioOnline || false,
            isVideoOnline: a.isVideoOnline || false,
            experience: a.experience || 0,
            isVerified: a.isVerified || false,
            isBusy: a.isBusy || false,
            image: formatImageUrl(a.image, a.name, SERVER_URL),
            languages: a.languages || ['Tamil', 'English'],
            orderCount: a.orderCount || 0,
            isDocumentVerified: a.isDocumentVerified || false
        }));
    } catch (e) {
        console.error('Error fetching formatted astros:', e);
        return [];
    }
}

async function broadcastAstroUpdate(io, SERVER_URL) {
    if (!io) {
        console.error('[Broadcast] Failed: io instance is undefined. Make sure app.set("io", io) is called in server.js');
        return;
    }
    try {
        const formattedAstros = await getFormattedAstrologers(SERVER_URL);
        io.emit('astrologer-update', formattedAstros);
        console.log(`Broadcasting update for ${formattedAstros.length} astrologers.`);
    } catch (e) {
        console.error('Broadcast Error:', e);
    }
}

async function broadcastSingleAstroUpdate(io, userId, SERVER_URL) {
    if (!io) return;
    try {
        const a = await User.findOne({ userId })
            .select('userId name phone skills profession price chatPrice callPrice videoPrice isOnline isChatOnline isAudioOnline isVideoOnline experience isVerified image walletBalance totalEarnings isBusy languages orderCount isDocumentVerified')
            .lean();
        if (!a) return;

        const formatted = {
            userId: a.userId,
            name: a.name,
            skills: extractSpecialties(a.skills, a.profession),
            profession: a.profession || '',
            price: a.price || 15,
            chatPrice: a.chatPrice || a.price || 15,
            callPrice: a.callPrice || a.price || 15,
            videoPrice: a.videoPrice || a.price || 20,
            isOnline: a.isOnline || false,
            isChatOnline: a.isChatOnline || false,
            isAudioOnline: a.isAudioOnline || false,
            isVideoOnline: a.isVideoOnline || false,
            experience: a.experience || 0,
            isVerified: a.isVerified || false,
            isBusy: a.isBusy || false,
            image: formatImageUrl(a.image, a.name, SERVER_URL),
            languages: a.languages || ['Tamil', 'English'],
            orderCount: a.orderCount || 0,
            isDocumentVerified: a.isDocumentVerified || false
        };

        io.emit('astrologer-single-update', formatted);
        console.log(`Broadcasting single update for ${a.name} (${userId}).`);
    } catch (e) {
        console.error('Single Broadcast Error:', e);
    }
}

module.exports = { getFormattedAstrologers, broadcastAstroUpdate, broadcastSingleAstroUpdate };

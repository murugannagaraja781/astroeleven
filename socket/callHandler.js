const crypto = require('crypto');
const User = require('../models/User');
const Session = require('../models/Session');
const ChatMessage = require('../models/ChatMessage');
const PairMonth = require('../models/PairMonth'); // Added for billing logic
const { sendFcmV1Push } = require('../services/push.service');
const { userSockets, socketToUser, activeSessions, userActiveSession, sessionDisconnectTimeouts, SESSION_GRACE_PERIOD } = require('../services/socketStore');
const billingService = require('../services/billing.service');
const presenceService = require('../services/presence.service');
const { broadcastAstroUpdate } = require('../services/astrologer.service');
const { getPanchanga } = require('../utils/rasiEng/panchangaCalc');
const { swissEph } = require('../utils/rasiEng/swisseph');
const { DateTime } = require('luxon');
const KundaliRecord = require('../models/KundaliRecord');

async function saveKundaliRecord({ clientId, clientName, clientPhone, birthData, astrologerId, astrologerName, consultationType, sessionId }) {
    try {
        if (!clientId && !clientPhone) return;
        const bYear = parseInt(birthData?.year) || (birthData?.dob ? parseInt(birthData.dob.split('-')[0]) : null);
        const bMonth = parseInt(birthData?.month) || (birthData?.dob ? parseInt(birthData.dob.split('-')[1]) : null);
        const bDay = parseInt(birthData?.day) || (birthData?.dob ? parseInt(birthData.dob.split('-')[2]) : null);

        let dobStr = '';
        if (bYear && bMonth && bDay) {
            dobStr = `${bYear}-${String(bMonth).padStart(2, '0')}-${String(bDay).padStart(2, '0')}`;
        } else if (birthData?.dob) {
            dobStr = birthData.dob;
        }

        const h = birthData?.hour !== undefined ? parseInt(birthData.hour) : null;
        const m = birthData?.minute !== undefined ? parseInt(birthData.minute) : 0;
        let tobStr = birthData?.tob || (h !== null ? `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}` : '');
        
        let tobFormatted = tobStr;
        if (h !== null && !isNaN(h)) {
            const ampm = h >= 12 ? 'PM' : 'AM';
            const h12 = h % 12 || 12;
            tobFormatted = `${String(h12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${ampm}`;
        }

        const pobStr = birthData?.city || birthData?.pob || '';

        await KundaliRecord.create({
            recordId: 'kr_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
            clientId: clientId || '',
            clientName: clientName || birthData?.name || 'Client',
            clientPhone: clientPhone || '',
            dob: dobStr,
            birthDay: bDay,
            birthMonth: bMonth,
            birthYear: bYear,
            tob: tobStr,
            tobFormatted,
            pob: pobStr,
            latitude: birthData?.latitude || null,
            longitude: birthData?.longitude || null,
            gender: birthData?.gender || '',
            marital: birthData?.marital || '',
            occupation: birthData?.occupation || '',
            topic: birthData?.topic || '',
            astrologerId: astrologerId || '',
            astrologerName: astrologerName || 'Astrologer',
            consultationType: consultationType || 'call',
            sessionId: sessionId || '',
            consultationDate: new Date()
        });
        console.log(`[KundaliRecord] Recorded consultation horoscope for ${clientName || clientPhone}`);
    } catch (e) {
        console.error('[KundaliRecord] Error saving kundali record:', e.message);
    }
}

function enrichBirthData(birthData) {
    if (!birthData) return null;
    try {
        const { year, month, day, hour, minute, latitude, longitude, timezone } = birthData;
        if (!year || !month || !day) return birthData;

        const h = hour !== undefined ? hour : 12;
        const m = minute !== undefined ? minute : 0;
        const lat = latitude !== undefined ? parseFloat(latitude) : 13.0827;
        const lon = longitude !== undefined ? parseFloat(longitude) : 80.2707;
        const tz = timezone || 'Asia/Kolkata';

        const dt = DateTime.fromObject(
            { year, month, day, hour: h, minute: m },
            { zone: tz }
        );
        const dtUtc = dt.toUTC();
        
        const jd = swissEph.julday(
            dtUtc.year,
            dtUtc.month,
            dtUtc.day,
            dtUtc.hour + dtUtc.minute / 60 + dtUtc.second / 3600
        );

        const panchanga = getPanchanga(jd, lat, lon);
        
        birthData.moonSign = panchanga.moonSign;
        birthData.nakshatra = panchanga.nakshatra?.name || '';
        birthData.pada = panchanga.nakshatra?.pada || '';
        birthData.sunSign = panchanga.sunSign;
        birthData.tithi = panchanga.tithi?.name || '';
    } catch (e) {
        console.error('[CallHandler] Failed to enrichBirthData:', e);
    }
    return birthData;
}

// Helper to get partner ID
function getOtherUserIdFromSession(sessionId, userId) {
    const s = activeSessions.get(sessionId);
    if (!s) return null;
    return s.users?.find(u => u !== userId);
}

module.exports = (io, socket, SERVER_URL, broadcastAstroUpdate) => {
    const logError = (event, err) => {
        console.error(`[CallHandler][${event}] error:`, err);
    };

    // --- Request Session (Call Initiation) ---
    socket.on('request-session', async (data, cb) => {
        try {
            const { toUserId, type, birthData } = data || {};
            let fromUserId = socketToUser.get(socket.id) || (data && data.fromUserId);

            // Self-registration for signaling robustness
            if (!socketToUser.has(socket.id) && fromUserId) {
                console.log(`[CallHandler][request-session] Auto-registering socket ${socket.id} for user ${fromUserId}`);
                socketToUser.set(socket.id, fromUserId);
                userSockets.set(fromUserId, socket.id);
                socket.join(fromUserId);
            }

            if (!fromUserId) {
                console.warn(`[CallHandler][request-session] unauthorized from socket ${socket.id}. Data:`, JSON.stringify(data));
                return typeof cb === 'function' && cb({ ok: false, error: 'Not registered' });
            }

            if (!toUserId || !type) {
                console.warn(`[CallHandler][request-session] missing fields from ${fromUserId}. Data:`, JSON.stringify(data));
                return typeof cb === 'function' && cb({ ok: false, error: 'Missing fields' });
            }

            console.log(`[CallHandler][request-session] from=${fromUserId}, to=${toUserId}, type=${type}`);

            const toUser = await User.findOne({ userId: toUserId });
            const fromUser = await User.findOne({ userId: fromUserId });

            if (!toUser) {
                console.warn(`[CallHandler][request-session] target user ${toUserId} not found`);
                return typeof cb === 'function' && cb({ ok: false, error: 'User not found' });
            }

            // Sync true wallet balance before starting the call
            if (fromUser && fromUser.role === 'client') {
                const syncedBal = await billingService.syncWalletBalance(fromUserId);
                if (syncedBal !== null && syncedBal !== undefined) {
                    fromUser.walletBalance = syncedBal;
                }
            }

            const requiredFirstMin = billingService.getRateForSession(toUser, type);

            if (fromUser && fromUser.role === 'client') {
                if ((fromUser.walletBalance || 0) < 1) {
                    console.warn(`[CallHandler][request-session] Wallet balance below 1 for ${fromUserId}. Balance: ${fromUser.walletBalance}`);
                    return typeof cb === 'function' && cb({
                        ok: false,
                        error: 'குறைந்த இருப்பு! உங்கள் வாலட் இருப்பு ₹1-க்கும் குறைவாக உள்ளது. அழைப்பைத் தொடங்க ரீசார்ஜ் செய்யவும்.'
                    });
                }
                if ((fromUser.walletBalance || 0) < requiredFirstMin) {
                    console.warn(`[CallHandler][request-session] Insufficient balance for ${fromUserId}. Balance: ${fromUser.walletBalance}, Required: ${requiredFirstMin}`);
                    return typeof cb === 'function' && cb({
                        ok: false,
                        error: `Insufficient balance. Minimum ₹${requiredFirstMin} required for 1 minute consultation.`
                    });
                }
            }

            // Enrich and save birthData/intakeDetails
            let enrichedBirthData = null;
            if (birthData) {
                enrichedBirthData = enrichBirthData(birthData);
                if (fromUser && fromUser.role === 'client') {
                    fromUser.name = birthData.name || fromUser.name;
                    fromUser.birthDetails = {
                        dob: `${birthData.year}-${String(birthData.month).padStart(2, '0')}-${String(birthData.day).padStart(2, '0')}`,
                        tob: `${String(birthData.hour).padStart(2, '0')}:${String(birthData.minute).padStart(2, '0')}`,
                        pob: birthData.city,
                        lat: birthData.latitude,
                        lon: birthData.longitude
                    };
                    fromUser.intakeDetails = {
                        gender: birthData.gender,
                        marital: birthData.marital,
                        occupation: birthData.occupation,
                        topic: birthData.topic,
                        partner: birthData.partnerData ? {
                            name: birthData.partnerData.name,
                            dob: birthData.partnerData.year ? `${birthData.partnerData.year}-${String(birthData.partnerData.month).padStart(2, '0')}-${String(birthData.partnerData.day).padStart(2, '0')}` : undefined,
                            tob: birthData.partnerData.hour !== undefined ? `${String(birthData.partnerData.hour).padStart(2, '0')}:${String(birthData.partnerData.minute).padStart(2, '0')}` : undefined,
                            pob: birthData.partnerData.city
                        } : undefined
                    };
                    await fromUser.save();
                }
            } else if (fromUser && fromUser.role === 'client' && fromUser.birthDetails && fromUser.birthDetails.dob) {
                // Backend fallback: Reconstruct birthData from saved profile details if not passed via socket payload
                try {
                    const bd = fromUser.birthDetails;
                    const id = fromUser.intakeDetails || {};
                    const [y, m, d] = bd.dob.split('-').map(Number);
                    const [h, min] = (bd.tob || '12:00').split(':').map(Number);
                    
                    const reconstructedBirthData = {
                        name: fromUser.name,
                        gender: id.gender || 'Male',
                        marital: id.marital || 'Single',
                        occupation: id.occupation || '',
                        topic: id.topic || 'General',
                        partner: id.partner ? {
                            name: id.partner.name,
                            dob: id.partner.dob,
                            tob: id.partner.tob,
                            pob: id.partner.pob
                        } : undefined,
                        year: y,
                        month: m,
                        day: d,
                        hour: h,
                        minute: min,
                        latitude: parseFloat(bd.lat || 0),
                        longitude: parseFloat(bd.lon || 0),
                        city: bd.pob,
                        timezone: parseFloat(bd.tz || 5.5)
                    };
                    enrichedBirthData = enrichBirthData(reconstructedBirthData);
                } catch (e) {
                    console.error('[CallHandler] Failed to reconstruct birthData from sender profile:', e);
                }
            }

            if (userActiveSession.has(toUserId)) {
                const existingId = userActiveSession.get(toUserId);
                const existingS = activeSessions.get(existingId);
                if (!existingS) {
                    userActiveSession.delete(toUserId);
                } else if (existingS.users.includes(fromUserId)) {
                    // DEDUPLICATION: If a session between these same two users already exists and is recent (within 15s), reuse it.
                    // This prevents "End Session" loops when users click the call button multiple times or during network retries.
                    const age = Date.now() - (existingS.startedAt || 0);
                    if (existingS.status === 'ringing' || age < 15000) {
                        console.log(`[CallHandler][request-session] Deduplicating request. Reusing active session ${existingId}`);
                        return typeof cb === 'function' && cb({ ok: true, sessionId: existingId });
                    }
                    console.log(`[CallHandler][request-session] Ending stale session ${existingId} to start new one.`);
                    await billingService.endSessionRecord(existingId, broadcastAstroUpdate);
                } else {
                    return typeof cb === 'function' && cb({ ok: false, error: 'User busy' });
                }
            }

            const sessionId = crypto.randomUUID();
            let clientId = null;
            let astrologerId = null;

            if (fromUser?.role === 'client') clientId = fromUserId;
            if (fromUser?.role === 'astrologer') astrologerId = fromUserId;
            if (toUser?.role === 'client') clientId = toUserId;
            if (toUser?.role === 'astrologer') astrologerId = toUserId;

            // Fallback: ensure neither is null
            if (!astrologerId) {
                if (toUser?.role === 'astrologer') astrologerId = toUserId;
                else if (fromUser?.role === 'astrologer') astrologerId = fromUserId;
                else astrologerId = toUserId;
            }
            if (!clientId) {
                if (fromUserId !== astrologerId) clientId = fromUserId;
                else clientId = toUserId;
            }

            await Session.create({
                sessionId, fromUserId, toUserId, type, startTime: Date.now(),
                clientId, astrologerId, status: 'ringing'
            });

            if (enrichedBirthData || birthData) {
                const bData = enrichedBirthData || birthData;
                saveKundaliRecord({
                    clientId: clientId,
                    clientName: bData.name || (fromUser && fromUser.role === 'client' ? fromUser.name : (toUser ? toUser.name : 'Client')),
                    clientPhone: bData.phone || (fromUser && fromUser.role === 'client' ? fromUser.phone : (toUser ? toUser.phone : '')),
                    birthData: bData,
                    astrologerId: astrologerId,
                    astrologerName: (toUser && toUser.role === 'astrologer' ? toUser.name : (fromUser ? fromUser.name : 'Astrologer')),
                    consultationType: type || 'call',
                    sessionId: sessionId
                });
            }

            activeSessions.set(sessionId, {
                type,
                users: [fromUserId, toUserId],
                startedAt: Date.now(),
                clientId,
                astrologerId,
                status: 'ringing',
                elapsedBillableSeconds: 0,
                lastBilledMinute: 0,
                actualBillingStart: null,
                totalDeducted: 0,
                totalEarned: 0
            });
            userActiveSession.set(fromUserId, sessionId);
            userActiveSession.set(toUserId, sessionId);

            console.log(`[CallHandler][request-session] Emitting incoming-session to room ${toUserId} | callerName: ${fromUser?.name}`);
            io.to(toUserId).emit('incoming-session', {
                sessionId, 
                fromUserId, // Legacy
                callerId: fromUserId, // Map to FCM style
                callerName: fromUser?.name || 'User',
                type, 
                birthData: enrichedBirthData || null
            });

            if (toUser.fcmToken) {
                const isVideo = type === 'video';
                const fcmType = type === 'chat' ? 'INCOMING_CHAT' : 'INCOMING_CALL';
                const fcmData = {
                    type: fcmType, sessionId, callType: type,
                    callerName: fromUser?.name || 'User',
                    callerId: fromUserId, timestamp: Date.now().toString(),
                    birthData: JSON.stringify(enrichedBirthData || {})
                };
                const fcmNotif = type === 'chat' ? {
                    title: `💬 Incoming Chat Request`,
                    body: `${fromUser?.name || 'Someone'} wants to chat with you`
                } : {
                    title: `📞 Incoming ${isVideo ? 'Video' : 'Audio'} Call`,
                    body: `${fromUser?.name || 'Someone'} is calling you for ${isVideo ? 'video' : 'audio'} consultation`
                };
                sendFcmV1Push(toUser.fcmToken, fcmData, fcmNotif, toUserId).catch(e => logError('FCM', e));
            }

            if (typeof cb === 'function') cb({ ok: true, sessionId });

            // Timeout for no answer (30 seconds)
            setTimeout(async () => {
                const s = activeSessions.get(sessionId);
                if (s && s.status === 'ringing') {
                    console.log(`[CallHandler][Timeout] Missed call from ${fromUserId} to ${toUserId}`);
                    
                    io.to(fromUserId).emit('session-ended', { sessionId, reason: 'no_answer' });
                    io.to(toUserId).emit('session-ended', { sessionId, reason: 'missed' });
                    
                    userActiveSession.delete(fromUserId);
                    userActiveSession.delete(toUserId);
                    activeSessions.delete(sessionId);

                    // GHOST CALL FIX 1: Mark session as ended in DB immediately so late answers are rejected
                    await Session.updateOne({ sessionId }, { 
                        status: 'ended', 
                        endReason: 'missed', 
                        endTime: Date.now() 
                    }).catch(() => {});

                    // GHOST CALL FIX 2: Send FCM CANCEL_CALL to recipient to kill notification & ringing
                    try {
                        const recipient = await User.findOne({ userId: toUserId }).select('fcmToken');
                        if (recipient && recipient.fcmToken) {
                            sendFcmV1Push(recipient.fcmToken, {
                                type: 'CANCEL_CALL',
                                sessionId,
                                callId: sessionId,
                                callerId: fromUserId,
                                reason: 'missed'
                            }, null, toUserId).catch(e => logError('missed-call-FCM', e));
                        }
                    } catch (e) {
                        logError('missed-call-FCM-lookup', e);
                    }

                    // AUTO-OFFLINE LOGIC: Modified as per USER REQUEST (Astrologer stays online)
                    const astro = await User.findOne({ userId: toUserId });
                    if (astro && astro.role === 'astrologer') {
                        // Log to file
                        const fs = require('fs');
                        const path = require('path');
                        const logPath = path.join(__dirname, '../missed_calls_log.txt');
                        const logEntry = `${new Date().toLocaleString()} | Astro: ${astro.name} (${astro.userId}) | Client: ${fromUserId} | Session: ${sessionId}\n`;
                        fs.appendFile(logPath, logEntry, (err) => {
                            if (err) console.error('[CallHandler] Failed to log missed call:', err);
                        });
                    }
                }
            }, 30000);

        } catch (err) {
            logError('request-session', err);
            if (typeof cb === 'function') cb({ ok: false, error: 'Internal error' });
        }
    });

    // --- Cancel Call (Caller cancels before astrologer answers) ---
    // GHOST CALL FIX 3: Fully handle caller cancellation and immediately clear astrologer UI & notification
    socket.on('cancel-call', async (data, cb) => {
        try {
            const { sessionId } = data || {};
            let toUserId = data?.toUserId;
            let fromUserId = socketToUser.get(socket.id) || (data && data.fromUserId);

            console.log(`[CallHandler][cancel-call] Caller ${fromUserId} cancelled session ${sessionId} for ${toUserId}`);

            let session = null;
            if (sessionId) {
                session = activeSessions.get(sessionId);
            }

            // GUARD: If session was already answered or active, do NOT treat as caller cancellation!
            // Route through normal billing completion so duration & earnings are accurately saved.
            if (session && (session.status === 'active' || session.status === 'answered' || session.actualBillingStart)) {
                console.log(`[CallHandler][cancel-call] Session ${sessionId} was already active/connected. Routing to endSessionRecord.`);
                billingService.endSessionRecord(sessionId, broadcastAstroUpdate, 'ended');
                if (typeof cb === 'function') cb({ ok: true });
                return;
            }

            const dbSess = sessionId ? await Session.findOne({ sessionId }) : null;
            if (dbSess && (dbSess.actualBillingStart || dbSess.status === 'active') && dbSess.status !== 'ended') {
                console.log(`[CallHandler][cancel-call] DB Session ${sessionId} was already active/connected. Routing to endSessionRecord.`);
                billingService.endSessionRecord(sessionId, broadcastAstroUpdate, 'ended');
                if (typeof cb === 'function') cb({ ok: true });
                return;
            }

            if (!toUserId && session && session.users) {
                toUserId = session.users.find(u => u !== fromUserId);
            }
            if (!toUserId && dbSess) {
                toUserId = dbSess.toUserId === fromUserId ? dbSess.fromUserId : dbSess.toUserId;
            }

            if (sessionId) {
                await Session.updateOne({ sessionId }, { 
                    status: 'ended', 
                    endReason: 'caller_cancelled',
                    endTime: Date.now() 
                }).catch(() => {});
                activeSessions.delete(sessionId);
            }

            if (fromUserId && userActiveSession.get(fromUserId) === sessionId) {
                userActiveSession.delete(fromUserId);
            }
            if (toUserId && userActiveSession.get(toUserId) === sessionId) {
                userActiveSession.delete(toUserId);
            }

            if (toUserId) {
                io.to(toUserId).emit('session-ended', { sessionId, reason: 'caller_cancelled' });
                io.to(toUserId).emit('call-cancelled', { sessionId, fromUserId });

                // Dismiss notification & ringing on recipient device immediately via FCM
                try {
                    const recipientUser = await User.findOne({ userId: toUserId }).select('fcmToken');
                    if (recipientUser && recipientUser.fcmToken) {
                        sendFcmV1Push(recipientUser.fcmToken, {
                            type: 'CANCEL_CALL',
                            sessionId,
                            callId: sessionId,
                            callerId: fromUserId,
                            reason: 'caller_cancelled'
                        }, null, toUserId).catch(e => logError('cancel-call-FCM', e));
                    }
                } catch (e) {
                    logError('cancel-call-FCM-lookup', e);
                }
            }

            if (fromUserId) {
                io.to(fromUserId).emit('session-ended', { sessionId, reason: 'caller_cancelled' });
            }

            if (typeof cb === 'function') cb({ ok: true });
        } catch (err) {
            logError('cancel-call', err);
            if (typeof cb === 'function') cb({ ok: false, error: 'Internal error' });
        }
    });

    // --- Answer Session ---
    socket.on('answer-session', async (data, cb) => {
        try {
            const { sessionId, type, accept } = data || {};
            let toUserId = data?.toUserId;
            let fromUserId = socketToUser.get(socket.id) || (data && data.fromUserId);

            // Fallback for fromUserId if missing
            if (!fromUserId && sessionId) {
                const existingS = activeSessions.get(sessionId);
                if (existingS && existingS.users && toUserId) {
                    fromUserId = existingS.users.find(u => u !== toUserId);
                }
                if (!fromUserId) {
                    const dbS = await Session.findOne({ sessionId });
                    if (dbS) fromUserId = (toUserId && dbS.toUserId === toUserId) ? dbS.fromUserId : dbS.toUserId;
                }
            }
            
            // Self-registration for signaling robustness
            if (!socketToUser.has(socket.id) && fromUserId) {
                console.log(`[CallHandler][answer-session] Auto-registering socket ${socket.id} for user ${fromUserId}`);
                socketToUser.set(socket.id, fromUserId);
                userSockets.set(fromUserId, socket.id);
                socket.join(fromUserId);
            }

            // Fallback for toUserId if missing from packet
            if (!toUserId && sessionId) {
                const existingS = activeSessions.get(sessionId);
                if (existingS && existingS.users) {
                    toUserId = existingS.users.find(u => u !== fromUserId);
                }
                if (!toUserId) {
                    const dbS = await Session.findOne({ sessionId });
                    if (dbS) toUserId = dbS.fromUserId === fromUserId ? dbS.toUserId : dbS.fromUserId;
                }
            }

            // Fallback for type if missing from packet
            let sessionType = type;
            if (!sessionType && sessionId) {
                const existingS = activeSessions.get(sessionId);
                if (existingS) sessionType = existingS.type;
                if (!sessionType) {
                    const dbS = await Session.findOne({ sessionId });
                    if (dbS) sessionType = dbS.type;
                }
            }

            if (!fromUserId || !sessionId || !toUserId) {
                console.warn(`[CallHandler][answer-session] missing data for session ${sessionId}. fromUserId=${fromUserId}, toUserId=${toUserId}`);
                return typeof cb === 'function' && cb({ ok: false, error: 'Missing data' });
            }

            // Guard: Check if DB session is already ended or expired (prevents late answer packet from resurrecting ended sessions)
            const existingDbSession = await Session.findOne({ sessionId });
            if (existingDbSession && (existingDbSession.status === 'ended' || existingDbSession.status === 'cancelled')) {
                console.log(`[CallHandler][answer-session] Session ${sessionId} already ended in DB (${existingDbSession.endReason || 'ended'}). Rejecting stale answer.`);
                socket.emit('session-ended', { sessionId, reason: existingDbSession.endReason || 'already_ended' });
                return typeof cb === 'function' && cb({ ok: false, error: 'Session already ended' });
            }
            if (existingDbSession && existingDbSession.startTime && (Date.now() - existingDbSession.startTime > 45000)) {
                console.log(`[CallHandler][answer-session] Session ${sessionId} expired (${Math.round((Date.now() - existingDbSession.startTime)/1000)}s old). Rejecting.`);
                await Session.updateOne({ sessionId }, { status: 'ended', endReason: 'expired', endTime: Date.now() }).catch(() => {});
                socket.emit('session-ended', { sessionId, reason: 'expired' });
                return typeof cb === 'function' && cb({ ok: false, error: 'Session expired' });
            }

            // Update status to prevent timeout cleanup
            const s = activeSessions.get(sessionId);
            if (s) {
                s.status = accept ? 'answered' : 'rejected';
                if (!accept) {
                    billingService.endSessionRecord(sessionId, broadcastAstroUpdate);
                }
            }
            
            // Mark in DB as well
            const updatePayload = { status: accept ? 'answered' : 'rejected' };
            if (accept) {
                updatePayload.astrologerConnectedAt = Date.now();
            }
            Session.updateOne({ sessionId }, updatePayload).catch(() => {});

            io.to(toUserId).emit('session-answered', {
                sessionId, fromUserId, type: sessionType || 'chat', accept: !!accept
            });

            console.log(`[CallHandler][answer-session] ${accept ? '✅ ACCEPTED' : '❌ REJECTED'} | session=${sessionId}, type=${sessionType || 'chat'} | by=${fromUserId} for=${toUserId}`);
            if (typeof cb === 'function') cb({ ok: true });
        } catch (err) {
            logError('answer-session', err);
            if (typeof cb === 'function') cb({ ok: false, error: 'Internal error' });
        }
    });

    // --- Answer session from Android Native (doesn't have toUserId) ---
    socket.on('answer-session-native', async (data, cb) => {
        try {
            const { sessionId, accept, callType } = data || {};
            let astrologerId = socketToUser.get(socket.id) || (data && (data.astrologerId || data.fromUserId));

            if (!astrologerId && sessionId) {
                const existingS = activeSessions.get(sessionId);
                if (existingS) astrologerId = existingS.toUserId || existingS.users?.[1] || existingS.users?.[0];
                if (!astrologerId) {
                    const dbS = await Session.findOne({ sessionId });
                    if (dbS) astrologerId = dbS.toUserId;
                }
            }

            if (!astrologerId || !sessionId) {
                if (typeof cb === 'function') cb({ ok: false, error: 'Invalid data' });
                return;
            }

            if (!socketToUser.has(socket.id) && astrologerId) {
                socketToUser.set(socket.id, astrologerId);
                userSockets.set(astrologerId, socket.id);
                socket.join(astrologerId);
            }

            const dbSession = await Session.findOne({ sessionId });
            if (!dbSession || dbSession.status === 'ended' || dbSession.status === 'cancelled') {
                console.log(`[CallHandler][answer-session-native] Session ${sessionId} not found or already ended in DB. Rejecting.`);
                socket.emit('session-ended', { sessionId, reason: dbSession?.endReason || 'already_ended' });
                if (typeof cb === 'function') cb({ ok: false, error: 'Session already ended' });
                return;
            }

            if (dbSession.startTime && (Date.now() - dbSession.startTime > 45000)) {
                console.log(`[CallHandler][answer-session-native] Session ${sessionId} expired (>45s). Rejecting.`);
                await Session.updateOne({ sessionId }, { status: 'ended', endReason: 'expired', endTime: Date.now() }).catch(() => {});
                socket.emit('session-ended', { sessionId, reason: 'expired' });
                if (typeof cb === 'function') cb({ ok: false, error: 'Session expired' });
                return;
            }

            const session = activeSessions.get(sessionId);
            if (session) {
                session.status = accept ? 'answered' : 'rejected';
            }
            Session.updateOne({ sessionId }, { status: accept ? 'answered' : 'rejected' }).catch(() => {});

            if (!session) {
                const fromUserId = dbSession.fromUserId;
                if (accept) {
                    io.to(fromUserId).emit('session-answered', {
                        sessionId, 
                        fromUserId: astrologerId, // Legacy
                        astrologerId: astrologerId, // Specific
                        type: callType || dbSession.type, 
                        accept: true
                    });
                    if (typeof cb === 'function') cb({ ok: true, fromUserId });
                } else {
                    io.to(fromUserId).emit('session-answered', {
                        sessionId, fromUserId: astrologerId, type: callType || dbSession.type, accept: false
                    });
                    billingService.endSessionRecord(sessionId, broadcastAstroUpdate);
                    if (typeof cb === 'function') cb({ ok: true });
                }
                return;
            }

            const fromUserId = session.users.find(u => u !== astrologerId);
            if (accept) {
                io.to(fromUserId).emit('session-answered', {
                    sessionId, fromUserId: astrologerId, type: callType || session.type, accept: true
                });
                console.log(`[CallHandler][answer-session-native] ✅ ACCEPTED | session=${sessionId} | by=${astrologerId} for=${fromUserId}`);
                if (typeof cb === 'function') cb({ ok: true, fromUserId });
            } else {
                io.to(fromUserId).emit('session-answered', {
                    sessionId, fromUserId: astrologerId, type: callType || session.type, accept: false
                });
                console.log(`[CallHandler][answer-session-native] ❌ REJECTED | session=${sessionId} | by=${astrologerId} for=${fromUserId}`);
                billingService.endSessionRecord(sessionId, broadcastAstroUpdate);
                if (typeof cb === 'function') cb({ ok: true });
            }
        } catch (err) {
            logError('answer-session-native', err);
            if (typeof cb === 'function') cb({ ok: false, error: 'Internal error' });
        }
    });

    // --- Signaling (WebRTC) ---
    socket.on('signal', (data) => {
        try {
            const { sessionId, toUserId, signal } = data || {};
            let fromUserId = socketToUser.get(socket.id) || data.fromUserId;

            const signalType = signal?.type || (signal?.candidate ? 'candidate' : 'unknown');
            
            // Self-registration for signaling robustness
            if (!fromUserId && data.fromUserId) {
                fromUserId = data.fromUserId;
                socketToUser.set(socket.id, fromUserId);
                userSockets.set(fromUserId, socket.id);
                socket.join(fromUserId);
            }

            let targetUserId = toUserId;
            if ((!targetUserId || targetUserId === 'unknown') && sessionId) {
                const s = activeSessions.get(sessionId);
                if (s && s.users) {
                    targetUserId = s.users.find(u => u !== fromUserId);
                }
                if (!targetUserId) {
                    for (const [uid, sessId] of userActiveSession.entries()) {
                        if (sessId === sessionId && uid !== fromUserId) {
                            targetUserId = uid;
                            break;
                        }
                    }
                }
            }

            console.log(`[CallHandler][signal] type=${signalType} | from=${fromUserId} to=${targetUserId} (orig: ${toUserId}) | session=${sessionId}`);

            if (targetUserId) {
                console.log(`[CallHandler][signal] Forwarding ${signalType} to room ${targetUserId}`);
                io.to(targetUserId).emit('signal', { sessionId, fromUserId, signal });
            }
        } catch (err) {
            logError('signal', err);
        }
    });

    // --- Session Connect (Billing initialization) ---
    socket.on('session-connect', async (data) => {
        try {
            const { sessionId } = data || {};
            let userId = socketToUser.get(socket.id);
            if (!userId && data && data.userId) {
                userId = data.userId;
                socketToUser.set(socket.id, userId);
                userSockets.set(userId, socket.id);
                socket.join(userId);
            }
            if (!userId || !sessionId) return;

            console.log(`[CallHandler][session-connect] User ${userId} joined session ${sessionId}`);

            const session = await Session.findOne({ sessionId });
            if (!session || session.status === 'ended' || session.status === 'cancelled') {
                console.warn(`[CallHandler][session-connect] Session ${sessionId} already ended or cancelled in DB. Rejecting user ${userId}`);
                socket.emit('session-ended', { sessionId, reason: session?.endReason || 'session_expired' });
                return;
            }

            // GHOST CALL GUARD: Validate session age
            const sessionAge = Date.now() - (session.startTime || 0);
            if (session.status === 'ringing' && sessionAge > 45000) {
                console.warn(`[CallHandler][session-connect] Session ${sessionId} is expired (${Math.round(sessionAge / 1000)}s old). Terminating.`);
                await Session.updateOne({ sessionId }, { status: 'ended', endReason: 'expired', endTime: Date.now() }).catch(() => {});
                socket.emit('session-ended', { sessionId, reason: 'expired' });
                return;
            }

            // Ensure clientId and astrologerId are set
            let updated = false;
            if (!session.clientId && session.fromUserId) {
                session.clientId = session.fromUserId;
                updated = true;
            }
            if (!session.astrologerId && session.toUserId) {
                session.astrologerId = session.toUserId;
                updated = true;
            }

            const now = Date.now();

            if (userId === session.clientId && !session.clientConnectedAt) {
                session.clientConnectedAt = now;
                updated = true;
            } else if (userId === session.astrologerId && !session.astrologerConnectedAt) {
                session.astrologerConnectedAt = now;
                updated = true;
            }

            if (updated) await session.save();

            // GHOST CALL GUARD: Check if both parties actually have alive socket connections!
            const clientSocketId = userSockets.get(session.clientId);
            const isClientSocketAlive = !!(clientSocketId && io.sockets.sockets.has(clientSocketId));

            const astroSocketId = userSockets.get(session.astrologerId);
            const isAstroSocketAlive = !!(astroSocketId && io.sockets.sockets.has(astroSocketId));

            // Check if both are connected and ready to start billing
            const isClientReady = !!(session.clientConnectedAt || userId === session.clientId) && isClientSocketAlive;
            const isAstroReady = !!(session.astrologerConnectedAt || userId === session.astrologerId || session.status === 'answered' || session.status === 'active') && isAstroSocketAlive;

            if (isClientReady && isAstroReady && !session.actualBillingStart) {
                const billingStart = Date.now();
                session.actualBillingStart = billingStart;
                session.status = 'active';
                if (!session.clientConnectedAt) session.clientConnectedAt = billingStart;
                if (!session.astrologerConnectedAt) session.astrologerConnectedAt = billingStart;
                await session.save();

                let activeSession = activeSessions.get(sessionId);
                if (!activeSession) {
                    activeSession = {
                        type: session.type || 'call',
                        users: [session.fromUserId || session.clientId, session.toUserId || session.astrologerId].filter(Boolean),
                        startedAt: session.startTime || Date.now(),
                        clientId: session.clientId,
                        astrologerId: session.astrologerId,
                        status: 'active',
                        elapsedBillableSeconds: 0,
                        lastBilledMinute: 0,
                        actualBillingStart: billingStart,
                        totalDeducted: 0,
                        totalEarned: 0
                    };
                    activeSessions.set(sessionId, activeSession);
                    if (session.clientId) userActiveSession.set(session.clientId, sessionId);
                    if (session.astrologerId) userActiveSession.set(session.astrologerId, sessionId);
                } else {
                    activeSession.status = 'active';
                    activeSession.actualBillingStart = billingStart;
                    activeSession.elapsedBillableSeconds = 0;
                    activeSession.clientId = session.clientId;
                    activeSession.astrologerId = session.astrologerId;
                }
                    
                    // Init Slab for Pair
                    const currentMonth = new Date().toISOString().slice(0, 7);
                    const pairId = `${session.clientId}_${session.astrologerId}`;
                    let pairRec = await PairMonth.findOne({ pairId, yearMonth: currentMonth });
                    if (!pairRec) {
                        pairRec = await PairMonth.create({
                            pairId, clientId: session.clientId, astrologerId: session.astrologerId,
                            yearMonth: currentMonth, currentSlab: 1
                        });
                    }
                    activeSession.pairMonthId = pairRec._id;
                    activeSession.currentSlab = pairRec.currentSlab || 1;
                    activeSession.initialPairSeconds = pairRec.slabLockedAt || 0;

                    // DEDUCT MINUTE 1 UPFRONT IMMEDIATELY ON CONNECT!
                    const billRes = await billingService.processBillingCharge(sessionId, 60, 1, 'first_60_full');
                    activeSession.lastBilledMinute = 1;
                    activeSession.totalDeducted = billRes?.charged || 0;
                    activeSession.totalEarned = billRes?.astroShare || 0;

                    if (!billRes || !billRes.success) {
                        console.warn(`[CallHandler][session-connect] Insufficient balance for session ${sessionId} at start. Ending.`);
                        billingService.endSessionRecord(sessionId, broadcastAstroUpdate, 'insufficient_balance');
                        return;
                    }

                    if (billRes.remainingBalance !== undefined && billRes.remainingBalance < 1) {
                        console.warn(`[CallHandler][session-connect] Remaining balance after minute 1 is < 1 (₹${billRes.remainingBalance}). Ending session ${sessionId}.`);
                        billingService.endSessionRecord(sessionId, broadcastAstroUpdate, 'insufficient_balance');
                        return;
                    }

                    const freshClient = await User.findOne({ userId: session.clientId });
                    const freshAstro = await User.findOne({ userId: session.astrologerId });
                    const clientBalance = freshClient ? Number(freshClient.walletBalance || 0) : 0;
                    if (clientBalance < 1) {
                        console.warn(`[CallHandler][session-connect] Client balance is < 1 (₹${clientBalance}). Ending session ${sessionId}.`);
                        billingService.endSessionRecord(sessionId, broadcastAstroUpdate, 'insufficient_balance');
                        return;
                    }
                    const ratePerMinute = billingService.getRateForSession(freshAstro, session.type);
                    const availableMinutes = ratePerMinute > 0 ? (Math.floor(clientBalance / ratePerMinute) + 1) : 1; // 1 min already paid + remaining

                    io.to(session.clientId).emit('billing-started', { startTime: billingStart, clientBalance, availableMinutes });
                    io.to(session.astrologerId).emit('billing-started', { startTime: billingStart, clientBalance, ratePerMinute, availableMinutes });
                    
                    console.log(`[CallHandler][session-connect] 🚀 BOTH CONNECTED. Minute 1 billed upfront (₹${activeSession.totalDeducted}). Client balance: ₹${clientBalance}`);
                    presenceService.setBusy(session.astrologerId, true, io);
            }
        } catch (err) {
            logError('session-connect', err);
        }
    });

    // --- End Session (Call Termination) ---
    socket.on('end-session', async (data) => {
        try {
            const { sessionId } = data || {};
            if (sessionId) {
                // Guard: Only terminate if session still exists (prevents duplicate termination)
                const s = activeSessions.get(sessionId);
                if (!s) {
                    console.log(`[CallHandler][end-session] Session ${sessionId} already terminated, acknowledging.`);
                    socket.emit('session-ended', { sessionId, reason: 'ended', summary: { deducted: 0, earned: 0, duration: 0 } });
                    return;
                }
                console.log(`[CallHandler][end-session] terminating session ${sessionId}`);
                billingService.endSessionRecord(sessionId, () => {
                    if (broadcastAstroUpdate) broadcastAstroUpdate();
                });
            }
        } catch (err) {
            logError('end-session', err);
        }
    });

    // --- Save Intake Details ---
    socket.on('save-intake-details', async (data, cb) => {
        try {
            const userId = socketToUser.get(socket.id);
            if (!userId) return;

            const u = await User.findOne({ userId });
            if (u) {
                u.birthDetails = {
                    dob: `${data.year}-${String(data.month).padStart(2, '0')}-${String(data.day).padStart(2, '0')}`,
                    tob: `${String(data.hour).padStart(2, '0')}:${String(data.minute).padStart(2, '0')}`,
                    pob: data.city, lat: data.latitude, lon: data.longitude
                };
                u.name = data.name;
                u.intakeDetails = {
                    gender: data.gender, marital: data.marital,
                    occupation: data.occupation, topic: data.topic, partner: data.partner
                };
                await u.save();
                saveKundaliRecord({
                    clientId: u.userId,
                    clientName: u.name,
                    clientPhone: u.phone,
                    birthData: data,
                    astrologerId: '',
                    astrologerName: 'Intake Submission',
                    consultationType: 'intake',
                    sessionId: userActiveSession.get(userId) || ''
                });
                if (typeof cb === 'function') cb({ ok: true });

                const sessionId = userActiveSession.get(userId);
                if (sessionId) {
                    const partnerId = getOtherUserIdFromSession(sessionId, userId);
                    if (partnerId) {
                        io.to(partnerId).emit('client-birth-chart', { sessionId, fromUserId: userId, birthData: data });
                    }
                }
            }
        } catch (err) {
            logError('save-intake-details', err);
        }
    });

    // --- Monitor Disconnects during calls ---
    socket.on('disconnect', () => {
        const userId = socketToUser.get(socket.id);
        if (userId && userActiveSession.has(userId)) {
            const sessionId = userActiveSession.get(userId);
            console.log(`[CallHandler][disconnect] User ${userId} disconnected during active session ${sessionId}`);
        }
    });
};

const mongoose = require('../utils/mongoose-mysql');

const KundaliRecordSchema = new mongoose.Schema({
    recordId: { type: String, unique: true },
    clientId: String,
    clientName: String,
    clientPhone: String,
    dob: String,          // e.g. "1998-07-13" or "13/07/1998"
    birthDay: Number,     // 1 to 31 (e.g. 13)
    birthMonth: Number,   // 1 to 12 (e.g. 7)
    birthYear: Number,    // e.g. 1998
    tob: String,          // "14:30"
    tobFormatted: String, // "02:30 PM"
    pob: String,          // Birth District / City
    latitude: Number,
    longitude: Number,
    gender: String,
    marital: String,
    occupation: String,
    topic: String,
    astrologerId: String,
    astrologerName: String,
    consultationType: { type: String, default: 'call' }, // 'call', 'chat', 'profile'
    sessionId: String,
    consultationDate: { type: Date, default: Date.now },
    createdAt: { type: Date, default: Date.now },
    updatedAt: { type: Date, default: Date.now }
});

KundaliRecordSchema.index({ clientPhone: 1 });
KundaliRecordSchema.index({ clientName: 1 });
KundaliRecordSchema.index({ birthDay: 1 });
KundaliRecordSchema.index({ birthMonth: 1 });
KundaliRecordSchema.index({ birthYear: 1 });
KundaliRecordSchema.index({ pob: 1 });
KundaliRecordSchema.index({ createdAt: 1 });

module.exports = mongoose.model('KundaliRecord', KundaliRecordSchema);

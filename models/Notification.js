const mongoose = require('../utils/mongoose-mysql');

const NotificationSchema = new mongoose.Schema({
    targetRole: { type: String, default: 'superadmin' },
    targetUserId: String,
    type: { type: String, default: 'withdrawal_request' },
    title: String,
    body: String,
    data: String,
    isRead: { type: Boolean, default: false },
    createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Notification', NotificationSchema);

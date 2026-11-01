const mongoose = require('../utils/mongoose-mysql');

const AcademyVideoSchema = new mongoose.Schema({
    title: String,
    youtubeUrl: String,
    thumbnail: String,
    category: String,
    audience: { type: String, default: 'user' }, // 'user' or 'astrologer'
    createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('AcademyVideo', AcademyVideoSchema);

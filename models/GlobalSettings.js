const mongoose = require('../utils/mongoose-mysql');

const GlobalSettingsSchema = new mongoose.Schema({
    key: { type: String, unique: true },
    value: mongoose.Schema.Types.Mixed
});

module.exports = mongoose.model('GlobalSettings', GlobalSettingsSchema);

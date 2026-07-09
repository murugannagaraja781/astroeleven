const mongoose = require('../utils/mongoose-mysql');

const ShopOrderSchema = new mongoose.Schema({
    orderId: { type: String, unique: true },
    userId: { type: String, required: true },
    itemType: { type: String, enum: ['puja', 'product'], required: true },
    itemId: { type: String, required: true },
    itemName: { type: String, required: true },
    price: { type: Number, required: true },
    astrologerReferralCode: { type: String, default: null },
    status: { type: String, enum: ['pending', 'completed', 'cancelled'], default: 'completed' },
    bookingDate: { type: Date, default: Date.now }
}, { timestamps: true });

module.exports = mongoose.model('ShopOrder', ShopOrderSchema);

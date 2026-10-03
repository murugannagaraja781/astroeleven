const mongoose = require('../utils/mongoose-mysql');

const PujaBookingSchema = new mongoose.Schema({
    bookingId: { type: String, unique: true },
    customerId: { type: String, default: '' },
    customerName: { type: String, required: true },
    mobile: { type: String, required: true },
    email: { type: String, default: '' },
    address: { type: String, default: '' },
    pujaId: { type: String, default: '' },
    pujaName: { type: String, required: true },
    pujaCategory: { type: String, default: 'Vedic Puja' },
    bookingDate: { type: String, default: '' },
    preferredTime: { type: String, default: '' },
    location: { type: String, default: '' },
    priestName: { type: String, default: '' },
    participants: { type: Number, default: 1 },
    specialInstructions: { type: String, default: '' },
    rasi: { type: String, default: '' },
    nakshatra: { type: String, default: '' },
    amount: { type: Number, default: 0 },
    discount: { type: Number, default: 0 },
    tax: { type: Number, default: 0 },
    finalAmount: { type: Number, default: 0 },
    paymentMethod: { type: String, default: 'ONLINE' },
    paymentTransactionId: { type: String, default: '' },
    paymentStatus: { type: String, enum: ['PENDING', 'PAID', 'FAILED', 'REFUNDED'], default: 'PENDING' },
    bookingStatus: { type: String, enum: ['PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED'], default: 'PENDING' },
    referralCode: { type: String, default: '' },
    notes: { type: String, default: '' }
}, { timestamps: true });

module.exports = mongoose.model('PujaBooking', PujaBookingSchema);

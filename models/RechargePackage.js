const mongoose = require('../utils/mongoose-mysql');

const RechargePackageSchema = new mongoose.Schema({
    packageId: { type: String, unique: true },
    amount: { type: Number, required: true },
    extraPercentage: { type: Number, default: 0 },
    badge: { type: String, default: '' },
    description: { type: String, default: '' },
    isActive: { type: Boolean, default: true },
    order: { type: Number, default: 0 },
    createdAt: { type: Date, default: Date.now },
    updatedAt: { type: Date, default: Date.now }
});

RechargePackageSchema.index({ amount: 1 });
RechargePackageSchema.index({ isActive: 1 });
RechargePackageSchema.index({ order: 1 });

module.exports = mongoose.model('RechargePackage', RechargePackageSchema);

const PujaBooking = require('../models/PujaBooking');
const User = require('../models/User');

// Helper to generate unique Booking ID: PB + 5 digits
function generateBookingId() {
    const randomDigits = Math.floor(10000 + Math.random() * 90000);
    return `PB${randomDigits}`;
}

// 1. Create a new Puja Booking (Proper API, no WhatsApp dependency)
exports.createBooking = async (req, res) => {
    try {
        const {
            customerId,
            customerName,
            mobile,
            email,
            address,
            pujaId,
            pujaName,
            pujaCategory,
            bookingDate,
            preferredTime,
            location,
            priestName,
            participants,
            specialInstructions,
            amount = 0,
            discount = 0,
            tax = 0,
            finalAmount,
            paymentMethod = 'ONLINE',
            paymentTransactionId,
            paymentStatus = 'PENDING',
            referralCode,
            rasi,
            nakshatra,
            notes
        } = req.body;

        // Validation
        if (!customerName || !customerName.trim()) {
            return res.status(400).json({ success: false, message: 'Customer Name is required.' });
        }
        if (!mobile || !mobile.toString().trim()) {
            return res.status(400).json({ success: false, message: 'Mobile number is required.' });
        }
        const cleanMobile = mobile.toString().replace(/[^0-9]/g, '');
        if (cleanMobile.length < 10) {
            return res.status(400).json({ success: false, message: 'Please enter a valid 10-digit mobile number.' });
        }
        if (!pujaName || !pujaName.trim()) {
            return res.status(400).json({ success: false, message: 'Puja Name is required.' });
        }
        const numAmount = amount ? (parseFloat(amount) || 0) : 0;
        const calculatedFinal = finalAmount !== undefined ? parseFloat(finalAmount) : Math.max(0, numAmount - parseFloat(discount || 0) + parseFloat(tax || 0));

        // Generate unique booking ID (check collision)
        let bookingId = generateBookingId();
        let exists = await PujaBooking.findOne({ bookingId });
        while (exists) {
            bookingId = generateBookingId();
            exists = await PujaBooking.findOne({ bookingId });
        }

        // Astrologer Referral Logic
        let refCodeApplied = null;
        if (referralCode && referralCode.trim()) {
            try {
                const astrologer = await User.findOne({ referralCode: referralCode.trim(), role: 'astrologer' });
                if (astrologer) {
                    refCodeApplied = astrologer.referralCode;
                    const commission = Math.round(calculatedFinal * 0.10); // 10% referral commission
                    astrologer.referredPujaCount = (astrologer.referredPujaCount || 0) + 1;
                    astrologer.referredCommission = (astrologer.referredCommission || 0) + commission;
                    astrologer.totalEarnings = (astrologer.totalEarnings || 0) + commission;
                    await astrologer.save();
                }
            } catch (err) {
                console.error('Referral commission error:', err);
            }
        }

        const booking = new PujaBooking({
            bookingId,
            customerId: customerId || cleanMobile,
            customerName: customerName.trim(),
            mobile: cleanMobile,
            email: email ? email.trim() : '',
            address: address ? address.trim() : '',
            pujaId: pujaId || '',
            pujaName: pujaName.trim(),
            pujaCategory: pujaCategory || 'Vedic Puja',
            bookingDate: bookingDate || new Date().toISOString().split('T')[0],
            preferredTime: preferredTime || 'Morning (09:00 AM - 12:00 PM)',
            location: location ? location.trim() : 'Home / Online Sankalpam',
            priestName: priestName ? priestName.trim() : '',
            participants: parseInt(participants) || 1,
            specialInstructions: specialInstructions ? specialInstructions.trim() : '',
            rasi: rasi ? rasi.trim() : '',
            nakshatra: nakshatra ? nakshatra.trim() : '',
            notes: notes ? notes.trim() : (specialInstructions ? specialInstructions.trim() : ''),
            amount: numAmount,
            discount: parseFloat(discount) || 0,
            tax: parseFloat(tax) || 0,
            finalAmount: calculatedFinal,
            paymentMethod,
            paymentTransactionId: paymentTransactionId || '',
            paymentStatus: paymentStatus.toUpperCase(),
            bookingStatus: 'PENDING',
            referralCode: refCodeApplied || ''
        });

        await booking.save();

        // Realtime Socket Notification if available
        const io = req.app.get('io');
        if (io) {
            io.emit('new-puja-booking', {
                bookingId: booking.bookingId,
                customerName: booking.customerName,
                pujaName: booking.pujaName,
                amount: booking.finalAmount,
                createdAt: booking.createdAt
            });
        }

        return res.status(201).json({
            success: true,
            message: 'Puja booking created successfully',
            bookingId: booking.bookingId,
            booking
        });
    } catch (error) {
        console.error('Create Puja Booking error:', error);
        return res.status(500).json({ success: false, message: error.message || 'Server error creating booking' });
    }
};

// 2. Get All Bookings (Search, Filter, Sort, Pagination)
exports.getAllBookings = async (req, res) => {
    try {
        const {
            search = '',
            bookingStatus = '',
            status = '',
            paymentStatus = '',
            startDate = '',
            endDate = '',
            sortBy = 'createdAt',
            sortOrder = 'desc',
            page = 1,
            limit = 20
        } = req.query;

        const effectiveStatus = bookingStatus || status || '';
        const query = {};

        if (effectiveStatus && effectiveStatus.toUpperCase() !== 'ALL') {
            query.bookingStatus = effectiveStatus.toUpperCase();
        }

        if (paymentStatus && paymentStatus.toUpperCase() !== 'ALL') {
            query.paymentStatus = paymentStatus.toUpperCase();
        }

        // Fetch records from PujaBooking
        let allRecords = await PujaBooking.find(query);

        // Also merge any Puja orders placed via ShopOrder table
        try {
            const ShopOrder = require('../models/ShopOrder');
            const shopOrders = await ShopOrder.find({ itemType: 'puja' });
            if (shopOrders && shopOrders.length > 0) {
                const existingIds = new Set(allRecords.map(r => String(r.bookingId || r.id)));
                shopOrders.forEach(so => {
                    const bId = so.orderId || `ORD${so.id}`;
                    if (!existingIds.has(bId)) {
                        const bStatus = (so.status || 'PENDING').toUpperCase() === 'COMPLETED' ? 'CONFIRMED' : (so.status || 'PENDING').toUpperCase();
                        const pStatus = 'PAID';

                        // Check status filter matching
                        if (effectiveStatus && effectiveStatus.toUpperCase() !== 'ALL' && bStatus !== effectiveStatus.toUpperCase()) {
                            return;
                        }
                        if (paymentStatus && paymentStatus.toUpperCase() !== 'ALL' && pStatus !== paymentStatus.toUpperCase()) {
                            return;
                        }

                        allRecords.push({
                            id: so.id,
                            bookingId: bId,
                            customerId: so.userId || '',
                            customerName: so.userId || 'Shop Customer',
                            mobile: so.userId || '',
                            email: '',
                            address: '',
                            pujaId: so.itemId || '',
                            pujaName: so.itemName || 'Puja Service',
                            pujaCategory: 'Vedic Puja',
                            bookingDate: so.bookingDate ? new Date(so.bookingDate).toISOString().split('T')[0] : '',
                            preferredTime: 'Morning (09:00 AM - 12:00 PM)',
                            location: 'Online Ritual',
                            priestName: '',
                            participants: 1,
                            specialInstructions: '',
                            rasi: '',
                            nakshatra: '',
                            amount: so.price || 0,
                            discount: 0,
                            tax: 0,
                            finalAmount: so.price || 0,
                            paymentMethod: 'ONLINE',
                            paymentTransactionId: '',
                            paymentStatus: pStatus,
                            bookingStatus: bStatus,
                            referralCode: so.astrologerReferralCode || '',
                            notes: 'Ordered via Astro Shop'
                        });
                    }
                });
            }
        } catch (e) {
            // Ignore if ShopOrder model or table unavailable
        }

        // In-memory search filtering for multi-field search (supported across both MySQL & Mongo)
        if (search && search.trim()) {
            const q = search.trim().toLowerCase();
            allRecords = allRecords.filter(b => {
                const bId = (b.bookingId || '').toLowerCase();
                const cName = (b.customerName || '').toLowerCase();
                const mob = (b.mobile || '').toLowerCase();
                const pName = (b.pujaName || '').toLowerCase();
                return bId.includes(q) || cName.includes(q) || mob.includes(q) || pName.includes(q);
            });
        }

        // Date range filtering
        if (startDate || endDate) {
            allRecords = allRecords.filter(b => {
                const bDate = b.bookingDate || (b.createdAt ? new Date(b.createdAt).toISOString().split('T')[0] : '');
                if (startDate && bDate < startDate) return false;
                if (endDate && bDate > endDate) return false;
                return true;
            });
        }

        // Sorting
        allRecords.sort((a, b) => {
            let valA = a[sortBy];
            let valB = b[sortBy];
            if (sortBy === 'amount' || sortBy === 'finalAmount') {
                valA = parseFloat(valA || 0);
                valB = parseFloat(valB || 0);
            } else if (sortBy === 'createdAt') {
                valA = new Date(valA || a.bookingDate || 0).getTime();
                valB = new Date(valB || b.bookingDate || 0).getTime();
            }
            if (sortOrder === 'asc') {
                return valA > valB ? 1 : -1;
            } else {
                return valA < valB ? 1 : -1;
            }
        });

        const total = allRecords.length;
        const pageNum = parseInt(page) || 1;
        const limitNum = parseInt(limit) || 20;
        const startIndex = (pageNum - 1) * limitNum;
        const paginatedRecords = allRecords.slice(startIndex, startIndex + limitNum);

        return res.status(200).json({
            success: true,
            data: paginatedRecords,
            total,
            page: pageNum,
            totalPages: Math.ceil(total / limitNum) || 1
        });
    } catch (error) {
        console.error('Get Puja Bookings error:', error);
        return res.status(500).json({ success: false, message: error.message || 'Server error' });
    }
};

// 3. Get Booking Summary Statistics
exports.getBookingSummary = async (req, res) => {
    try {
        const bookings = await PujaBooking.find({});
        
        // Also include ShopOrder puja items in summary
        try {
            const ShopOrder = require('../models/ShopOrder');
            const shopOrders = await ShopOrder.find({ itemType: 'puja' });
            if (shopOrders && shopOrders.length > 0) {
                const existingIds = new Set(bookings.map(r => String(r.bookingId || r.id)));
                shopOrders.forEach(so => {
                    const bId = so.orderId || `ORD${so.id}`;
                    if (!existingIds.has(bId)) {
                        bookings.push({
                            bookingId: bId,
                            bookingStatus: (so.status || 'PENDING').toUpperCase() === 'COMPLETED' ? 'CONFIRMED' : (so.status || 'PENDING').toUpperCase(),
                            paymentStatus: 'PAID',
                            finalAmount: so.price || 0,
                            amount: so.price || 0,
                            createdAt: so.createdAt || so.bookingDate,
                            bookingDate: so.bookingDate
                        });
                    }
                });
            }
        } catch (e) {}

        const todayStr = new Date().toISOString().split('T')[0];

        let total = bookings.length;
        let newToday = 0;
        let pending = 0;
        let confirmed = 0;
        let inProgress = 0;
        let completed = 0;
        let cancelled = 0;
        let failed = 0;
        let totalRevenue = 0;

        bookings.forEach(b => {
            const bDate = b.createdAt ? new Date(b.createdAt).toISOString().split('T')[0] : (b.bookingDate || '');
            if (bDate === todayStr) newToday++;

            const status = (b.bookingStatus || '').toUpperCase();
            if (status === 'PENDING') pending++;
            else if (status === 'CONFIRMED') confirmed++;
            else if (status === 'IN_PROGRESS') inProgress++;
            else if (status === 'COMPLETED') completed++;
            else if (status === 'CANCELLED') cancelled++;
            else if (status === 'REJECTED') failed++;

            const payStatus = (b.paymentStatus || '').toUpperCase();
            if (payStatus === 'PAID') {
                totalRevenue += (parseFloat(b.finalAmount || b.amount) || 0);
            }
        });

        return res.status(200).json({
            success: true,
            stats: {
                totalBookings: total,
                newBookings: newToday,
                pending,
                confirmed,
                inProgress,
                completed,
                cancelled,
                failed,
                totalRevenue: Math.round(totalRevenue)
            }
        });
    } catch (error) {
        console.error('Get Booking Summary error:', error);
        return res.status(500).json({ success: false, message: error.message || 'Server error' });
    }
};

// 4. Get Single Booking by ID
exports.getBookingById = async (req, res) => {
    try {
        const { bookingId } = req.params;
        let booking = await PujaBooking.findOne({ bookingId });
        if (!booking) {
            booking = await PujaBooking.findOne({ id: bookingId });
        }
        if (!booking) {
            try {
                const ShopOrder = require('../models/ShopOrder');
                const so = await ShopOrder.findOne({ orderId: bookingId }) || await ShopOrder.findOne({ id: bookingId });
                if (so && so.itemType === 'puja') {
                    booking = {
                        id: so.id,
                        bookingId: so.orderId,
                        customerId: so.userId,
                        customerName: so.userId,
                        mobile: so.userId,
                        pujaName: so.itemName,
                        pujaCategory: 'Vedic Puja',
                        amount: so.price,
                        finalAmount: so.price,
                        paymentMethod: 'ONLINE',
                        paymentStatus: 'PAID',
                        bookingStatus: (so.status || 'PENDING').toUpperCase() === 'COMPLETED' ? 'CONFIRMED' : (so.status || 'PENDING').toUpperCase(),
                        bookingDate: so.bookingDate ? new Date(so.bookingDate).toISOString().split('T')[0] : '',
                        createdAt: so.createdAt || so.bookingDate,
                        referralCode: so.astrologerReferralCode,
                        notes: 'Ordered via Shop Checkout'
                    };
                }
            } catch(e) {}
        }
        if (!booking) {
            return res.status(404).json({ success: false, message: 'Booking not found' });
        }
        return res.status(200).json({ success: true, data: booking });
    } catch (error) {
        return res.status(500).json({ success: false, message: error.message });
    }
};

// 5. Update Booking Status
exports.updateBookingStatus = async (req, res) => {
    try {
        const { bookingId } = req.params;
        const { bookingStatus, status, notes } = req.body;
        const statusVal = (bookingStatus || status || '').toUpperCase();

        const validStatuses = ['PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED'];
        if (!statusVal || !validStatuses.includes(statusVal)) {
            return res.status(400).json({ success: false, message: 'Invalid booking status' });
        }

        const booking = await PujaBooking.findOne({ bookingId });
        if (!booking) {
            return res.status(404).json({ success: false, message: 'Booking not found' });
        }

        booking.bookingStatus = statusVal;
        if (notes !== undefined) {
            booking.notes = notes;
        }
        await booking.save();

        return res.status(200).json({
            success: true,
            message: `Booking status updated to ${booking.bookingStatus}`,
            booking
        });
    } catch (error) {
        return res.status(500).json({ success: false, message: error.message });
    }
};

// 6. Update Payment Status
exports.updatePaymentStatus = async (req, res) => {
    try {
        const { bookingId } = req.params;
        const { paymentStatus, status, paymentTransactionId } = req.body;
        const statusVal = (paymentStatus || status || '').toUpperCase();

        const validStatuses = ['PENDING', 'PAID', 'FAILED', 'REFUNDED'];
        if (!statusVal || !validStatuses.includes(statusVal)) {
            return res.status(400).json({ success: false, message: 'Invalid payment status' });
        }

        const booking = await PujaBooking.findOne({ bookingId });
        if (!booking) {
            return res.status(404).json({ success: false, message: 'Booking not found' });
        }

        booking.paymentStatus = statusVal;
        if (paymentTransactionId) {
            booking.paymentTransactionId = paymentTransactionId;
        }
        await booking.save();

        return res.status(200).json({
            success: true,
            message: `Payment status updated to ${booking.paymentStatus}`,
            booking
        });
    } catch (error) {
        return res.status(500).json({ success: false, message: error.message });
    }
};

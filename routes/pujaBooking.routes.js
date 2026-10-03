const express = require('express');
const router = express.Router();
const pujaBookingController = require('../controllers/pujaBooking.controller');

// 1. Create new Puja Booking
router.post('/', pujaBookingController.createBooking);

// 2. Summary stats for Dashboard
router.get('/summary', pujaBookingController.getBookingSummary);

// 3. Get all bookings with search, filter, sort, pagination
router.get('/', pujaBookingController.getAllBookings);

// 4. Get single booking by bookingId
router.get('/:bookingId', pujaBookingController.getBookingById);

// 5. Update booking status (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED)
router.put('/:bookingId/status', pujaBookingController.updateBookingStatus);

// 6. Update payment status (PENDING, PAID, FAILED, REFUNDED)
router.put('/:bookingId/payment', pujaBookingController.updatePaymentStatus);

module.exports = router;

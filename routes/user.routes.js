const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');

router.get('/chat/history/:sessionId', userController.getChatHistory);
router.get('/history/:sessionId', userController.getChatHistory);
router.get('/:userId', userController.getProfile);
router.post('/apply-referral', userController.applyReferral);

module.exports = router;

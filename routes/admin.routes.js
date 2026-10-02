const express = require('express');
const router = express.Router();
const adminController = require('../controllers/admin.controller');
const upload = require('../middleware/upload');

router.get('/academy/videos', adminController.getVideos);
router.post('/academy/videos', adminController.addVideo);
router.delete('/academy/videos/:id', adminController.deleteVideo);
router.get('/banners', adminController.getBanners);
router.post('/banners', upload.single('bannerImage'), adminController.addBanner);
router.put('/banners/:id', upload.single('bannerImage'), adminController.addBanner);
router.delete('/banners/:id', adminController.deleteBanner);
router.get('/deletion-requests', adminController.getDeletionRequests);
router.post('/process-deletion', adminController.processDeletion);
router.post('/update-balance', adminController.updateBalance);
router.get('/astrologers/pending', adminController.getPendingAstrologers);
router.post('/astrologers/approve', adminController.approveAstrologer);
router.get('/astrologer-performance/:astrologerId', adminController.getAstrologerPerformance);
router.get('/astrologers-performance', adminController.getAllAstrologersPerformance);
router.get('/config', adminController.getConfig);
router.post('/config', adminController.updateConfig);
router.get('/smtp-config', adminController.getSmtpConfig);
router.post('/smtp-config', adminController.updateSmtpConfig);

module.exports = router;

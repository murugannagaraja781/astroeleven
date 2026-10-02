module.exports = {
    CLIENT_ID: process.env.PHONEPE_CLIENT_ID || process.env.PHONEPE_MERCHANT_ID || 'SU2607301135464361584371',
    CLIENT_SECRET: process.env.PHONEPE_CLIENT_SECRET || process.env.PHONEPE_SALT_KEY || '0c6d1657-d27b-4ba4-a23d-ec48054dd3dd',
    CLIENT_VERSION: process.env.PHONEPE_CLIENT_VERSION || '1',
    TOKEN_URL: 'https://api.phonepe.com/apis/identity-manager/v1/oauth/token',
    PAY_URL: 'https://api.phonepe.com/apis/pg/checkout/v2/pay'
};
